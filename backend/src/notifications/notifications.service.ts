import { Injectable, Logger } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { Interval } from "@nestjs/schedule";
import QRCode from "qrcode";
import { PrismaService } from "../prisma.service.js";
import { MailService } from "../auth/mail.service.js";
import { FcmService } from "./fcm.service.js";
import {
  purchaseEmail,
  refundEmail,
  type TicketSnapshot,
} from "./transaction-email.js";
import { ticketQrPayload } from "../payments/payment-domain.js";
import { Prisma } from "../generated/prisma/client.js";

type Notice = {
  sentAt?: string;
  pushSentAt?: string;
  attempts?: number;
  nextAttemptAt?: string;
};
type RefundNotices = {
  leaseUntil?: string;
  recipientLabel?: string;
  requested?: Notice;
  completed?: Notice;
  review?: Notice;
};
export const notificationRetryAt = (attempt: number, now: Date) =>
  new Date(
    now.getTime() + Math.min(3600000, 30000 * 2 ** Math.min(attempt, 7)),
  );
export function refundNoticeEvents(
  state: string,
): ("requested" | "completed" | "review")[] {
  return [
    "requested",
    ...(state === "SUCCEEDED" ? ["completed" as const] : []),
    ...(["NEEDS_REVIEW", "FAILED"].includes(state) ? ["review" as const] : []),
  ];
}
@Injectable()
export class NotificationsService {
  private running = false;
  private readonly logger = new Logger(NotificationsService.name);
  constructor(
    private readonly prisma: PrismaService,
    private readonly config: ConfigService,
    private readonly mail: MailService,
    private readonly fcm: FcmService,
  ) {}
  @Interval(15000)
  async tick() {
    if (this.running || (!this.emailEnabled && !this.pushEnabled)) return;
    this.running = true;
    try {
      const now = new Date();
      const tickets = await this.prisma.ticket.findMany({
        where: {
          emailAttempts: { lt: 10 },
          AND: [
            {
              OR: [
                { emailLeaseUntil: null },
                { emailLeaseUntil: { lte: now } },
              ],
            },
            {
              OR: [
                { emailNextAttemptAt: null },
                { emailNextAttemptAt: { lte: now } },
              ],
            },
          ],
          OR: [
            ...(this.emailEnabled ? [{ emailSentAt: null }] : []),
            ...(this.pushEnabled
              ? [
                  {
                    fcmSentAt: null,
                    order: { user: { fcmToken: { not: null } } },
                  },
                ]
              : []),
          ],
        },
        select: { id: true },
        take: 10,
        orderBy: { issuedAt: "asc" },
      });
      for (const ticket of tickets) await this.ticket(ticket.id);
      const dueNotice = (event: string) => Prisma.sql`(
        COALESCE((p.notification_state->${event}->>'attempts')::int,0) < 10
        AND COALESCE((p.notification_state->${event}->>'nextAttemptAt')::timestamptz, '-infinity'::timestamptz) <= clock_timestamp()
        AND ((${this.emailEnabled} AND p.notification_state->${event}->>'sentAt' IS NULL)
          OR (${this.pushEnabled} AND p.notification_state->${event}->>'pushSentAt' IS NULL AND u.fcm_token IS NOT NULL)))`;
      const refunds = await this.prisma.$queryRaw<{ id: string }[]>(Prisma.sql`
        SELECT p.id FROM payments p JOIN orders o ON o.id=p.order_id JOIN users u ON u.id=o.user_id
        WHERE p.kind='REFUND'
          AND COALESCE((p.notification_state->>'leaseUntil')::timestamptz, '-infinity'::timestamptz) <= clock_timestamp()
          AND (${dueNotice("requested")}
            OR (p.operation_state='SUCCEEDED' AND ${dueNotice("completed")})
            OR (p.operation_state IN ('NEEDS_REVIEW','FAILED') AND ${dueNotice("review")}))
        ORDER BY p.created_at ASC LIMIT 100`);
      for (const refund of refunds) await this.refund(refund.id);
    } catch {
      this.logger.warn(
        "Transaction notification retry failed; financial state is unchanged.",
      );
    } finally {
      this.running = false;
    }
  }
  private get emailEnabled() {
    return this.config.get<boolean>("TRANSACTION_EMAIL_ENABLED") === true;
  }
  private get pushEnabled() {
    return this.config.get<boolean>("FCM_ENABLED") === true;
  }
  private async ticket(id: string) {
    const now = new Date();
    const lease = await this.prisma.ticket.updateMany({
      where: {
        id,
        OR: [{ emailLeaseUntil: null }, { emailLeaseUntil: { lte: now } }],
      },
      data: { emailLeaseUntil: new Date(now.getTime() + 120000) },
    });
    if (!lease.count) return;
    try {
      const ticket = await this.prisma.ticket.findUniqueOrThrow({
        where: { id },
        include: { order: { include: { user: true } } },
      });
      const snapshot = ticket.snapshot as unknown as TicketSnapshot;
      let failed = false;
      if (this.emailEnabled && !ticket.emailSentAt) {
        const qr = await QRCode.toBuffer(
          ticketQrPayload(
            id,
            this.config.getOrThrow<string>("TICKET_QR_SECRET"),
          ),
          { type: "png", width: 480, margin: 4, errorCorrectionLevel: "M" },
        );
        const sent = await this.mail.sendTransaction(
          snapshot.email,
          purchaseEmail(
            snapshot,
            qr,
            ticket.status === "VALID" && ticket.expiresAt > new Date(),
          ),
        );
        if (sent)
          await this.prisma.ticket.update({
            where: { id },
            data: { emailSentAt: new Date() },
          });
        else failed = true;
      }
      const user = ticket.order.user;
      if (this.pushEnabled && !ticket.fcmSentAt && user.fcmToken) {
        const sent = await this.fcm.send(
          user.id,
          user.fcmToken,
          "Đặt vé thành công",
          `${snapshot.movieTitle} · ${snapshot.bookingCode}`,
          id,
        );
        if (sent)
          await this.prisma.ticket.update({
            where: { id },
            data: { fcmSentAt: new Date() },
          });
        else failed = true;
      }
      await this.prisma.ticket.update({
        where: { id },
        data: {
          emailLeaseUntil: null,
          emailAttempts: failed ? { increment: 1 } : undefined,
          emailNextAttemptAt: failed
            ? notificationRetryAt(ticket.emailAttempts, new Date())
            : null,
        },
      });
    } catch {
      await this.prisma.ticket.update({
        where: { id },
        data: {
          emailLeaseUntil: null,
          emailAttempts: { increment: 1 },
          emailNextAttemptAt: notificationRetryAt(2, new Date()),
        },
      });
    }
  }
  private async refund(id: string) {
    const payment = await this.prisma.$transaction(async (tx) => {
      await tx.$queryRaw`SELECT id FROM payments WHERE id=${id}::uuid FOR UPDATE`;
      const p = await tx.payment.findUniqueOrThrow({
        where: { id },
        include: { order: { include: { ticket: true, user: true } } },
      });
      const state = (p.notificationState ?? {}) as unknown as RefundNotices;
      if (state.leaseUntil && new Date(state.leaseUntil) > new Date())
        return null;
      const events = refundNoticeEvents(p.operationState);
      if (
        !events.some((event) => {
          const n = state[event];
          return (
            (n?.attempts ?? 0) < 10 &&
            (!n?.nextAttemptAt || new Date(n.nextAttemptAt) <= new Date()) &&
            ((this.emailEnabled && !n?.sentAt) ||
              (this.pushEnabled && p.order?.user.fcmToken && !n?.pushSentAt))
          );
        })
      )
        return null;
      state.leaseUntil = new Date(Date.now() + 120000).toISOString();
      await tx.payment.update({
        where: { id },
        data: { notificationState: state as unknown as Prisma.InputJsonValue },
      });
      return { payment: p, state };
    });
    if (!payment?.payment.order?.ticket) return;
    const { payment: p, state } = payment;
    const order = p.order!;
    const ticket = order.ticket!;
    const snapshot = ticket.snapshot as unknown as TicketSnapshot;
    try {
      for (const event of refundNoticeEvents(p.operationState)) {
        const n = state[event] ?? {};
        if (
          (n.attempts ?? 0) >= 10 ||
          (n.nextAttemptAt && new Date(n.nextAttemptAt) > new Date())
        )
          continue;
        let failed = false;
        if (this.emailEnabled && !n.sentAt) {
          const message = refundEmail(snapshot, event, {
            id,
            amount: p.expectedAmount.toString(),
            recipientLabel: state.recipientLabel,
            reference: p.providerTransactionId ?? undefined,
            completedAt: p.paidAt?.toISOString(),
          });
          if (await this.mail.sendTransaction(snapshot.email, message))
            n.sentAt = new Date().toISOString();
          else failed = true;
        }
        if (this.pushEnabled && order.user.fcmToken && !n.pushSentAt) {
          const title =
            event === "completed"
              ? "Hoàn tiền thành công"
              : event === "review"
                ? "Yêu cầu hoàn cần kiểm tra"
                : "Đã tiếp nhận yêu cầu hoàn vé";
          if (
            await this.fcm.send(
              order.user.id,
              order.user.fcmToken,
              title,
              `${snapshot.movieTitle} · ${snapshot.bookingCode}`,
              ticket.id,
            )
          )
            n.pushSentAt = new Date().toISOString();
          else failed = true;
        }
        if (failed) {
          n.attempts = (n.attempts ?? 0) + 1;
          n.nextAttemptAt = notificationRetryAt(
            n.attempts,
            new Date(),
          ).toISOString();
        }
        state[event] = n;
        // Persist each event before another send; retries never re-enter payment/refund logic.
        await this.prisma.payment.update({
          where: { id },
          data: {
            notificationState: state as unknown as Prisma.InputJsonValue,
          },
        });
      }
    } finally {
      delete state.leaseUntil;
      await this.prisma.payment.update({
        where: { id },
        data: { notificationState: state as unknown as Prisma.InputJsonValue },
      });
    }
  }
}
