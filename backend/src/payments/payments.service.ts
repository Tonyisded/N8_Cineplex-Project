import {
  Injectable,
  BadRequestException,
  ConflictException,
  NotFoundException,
  ServiceUnavailableException,
} from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { Interval } from "@nestjs/schedule";
import { randomUUID } from "node:crypto";
import type { Payment, Ticket } from "../generated/prisma/client.js";
import { Prisma } from "../generated/prisma/client.js";
import { PrismaService } from "../prisma.service.js";
import { BookingGateway } from "../booking/booking.gateway.js";
import { PayosAdapter } from "./payos.adapter.js";
import {
  canRefund,
  collectionAccountMatches,
  collectionProblem,
  maskAccount,
  openRecipient,
  payer,
  sealRecipient,
  ticketQrPayload,
  tokenHash,
} from "./payment-domain.js";
import {
  initialRefundRetry,
  readRefundRetry,
  refundRetryDecision,
  refundRetryAt,
  REFUND_RETRY_LIMIT,
  safeRefundError,
} from "./refund-retry.js";
import type { Transaction, Payout } from "@payos/node";
const include = {
  seats: true,
  user: true,
  combo: true,
  ticket: true,
  showtime: { include: { movie: true, auditorium: true } },
} satisfies Prisma.OrderInclude;
type FullOrder = Prisma.OrderGetPayload<{ include: typeof include }>;
type Tx = Prisma.TransactionClient;
const fail = (code: string, message: string) =>
  new ConflictException({ code, message });
const missing = () =>
  new NotFoundException({
    code: "NOT_FOUND",
    message: "Không tìm thấy dữ liệu của bạn.",
  });
const obj = (
  v: Prisma.JsonValue,
): Record<string, Prisma.JsonValue | undefined> =>
  v && typeof v === "object" && !Array.isArray(v) ? v : {};
const json = (value: unknown) =>
  JSON.parse(JSON.stringify(value)) as Prisma.InputJsonValue;
@Injectable()
export class PaymentsService {
  private busy = false;
  constructor(
    private readonly db: PrismaService,
    private readonly config: ConfigService,
    private readonly provider: PayosAdapter,
    private readonly gateway: BookingGateway,
  ) {}
  private enabled(key: string) {
    const value = this.config.get(key);
    return value === true || value === "true";
  }
  private wrap<T>(item: T) {
    return { serverTime: new Date().toISOString(), item };
  }
  private async now(tx: Tx) {
    const [r] = await tx.$queryRaw<
      { now: Date }[]
    >`SELECT clock_timestamp() AS now`;
    return r.now;
  }
  private async lockOrder(
    tx: Tx,
    id: string,
    userId?: string,
  ): Promise<FullOrder> {
    const found = await tx.order.findUnique({
      where: { id },
      select: { userId: true, showtimeId: true },
    });
    if (!found || (userId && found.userId !== userId)) throw missing();
    await tx.$queryRaw`SELECT id FROM showtimes WHERE id=${found.showtimeId}::uuid FOR UPDATE`;
    await tx.$queryRaw`SELECT id FROM users WHERE id=${found.userId}::uuid FOR UPDATE`;
    await tx.$queryRaw`SELECT id FROM orders WHERE id=${id}::uuid FOR UPDATE`;
    const order = await tx.order.findUniqueOrThrow({ where: { id }, include });
    if (order.seats.length)
      await tx.$queryRaw(
        Prisma.sql`SELECT id FROM showtime_seats WHERE id IN (${Prisma.join(order.seats.map((s) => Prisma.sql`${s.showtimeSeatId}::uuid`))}) ORDER BY id FOR UPDATE`,
      );
    if (order.ticket)
      await tx.$queryRaw`SELECT id FROM tickets WHERE id=${order.ticket.id}::uuid FOR UPDATE`;
    return order;
  }
  private snapshot(order: FullOrder, ticketId: string, bookingCode: string) {
    const comboTotal =
      BigInt(order.subtotal.toString()) -
      order.seats.reduce((sum, s) => sum + BigInt(s.price.toString()), 0n);
    const comboUnitPrice = order.comboQuantity
      ? String(comboTotal / BigInt(order.comboQuantity))
      : "0";
    return json({
      ticketId,
      bookingCode,
      email: order.user.email,
      movieTitle: order.showtime.movie.title,
      posterUrl: order.showtime.movie.posterUrl,
      startAt: order.showtime.startAt.toISOString(),
      format: order.showtime.format,
      auditorium: order.showtime.auditorium.name,
      seatCodes: order.seats.map((s) => s.seatCode),
      seats: order.seats.map((s) => ({
        seatCode: s.seatCode,
        price: s.price.toString(),
      })),
      comboName: order.combo?.name ?? null,
      comboQuantity: order.comboQuantity,
      comboUnitPrice,
      subtotal: order.subtotal.toString(),
      discount: order.discount.toString(),
      totalAmount: order.totalAmount.toString(),
      voucherCode: order.voucherCode,
    });
  }
  private async issue(tx: Tx, order: FullOrder, now: Date) {
    if (order.ticket) return order.ticket;
    if (
      order.status !== "PENDING_PAYMENT" ||
      order.expiresAt <= now ||
      order.showtime.status !== "ACTIVE"
    )
      throw fail("ORDER_CLOSED", "Đơn đã hết hiệu lực.");
    const held = await tx.showtimeSeat.count({
      where: {
        id: { in: order.seats.map((s) => s.showtimeSeatId) },
        status: "HELD",
        heldByUserId: order.userId,
        holdExpiresAt: order.expiresAt,
      },
    });
    if (!order.seats.length || held !== order.seats.length)
      throw fail("SEAT_UNAVAILABLE", "Ghế không còn thuộc đơn.");
    await tx.showtimeSeat.updateMany({
      where: {
        id: { in: order.seats.map((s) => s.showtimeSeatId) },
        status: "HELD",
        heldByUserId: order.userId,
        holdExpiresAt: order.expiresAt,
      },
      data: { status: "SOLD", heldByUserId: null, holdExpiresAt: null },
    });
    await tx.orderSeat.updateMany({
      where: { orderId: order.id, status: "HELD" },
      data: { status: "ACTIVE" },
    });
    await tx.order.update({
      where: { id: order.id },
      data: { status: "PAID" },
    });
    const id = randomUUID(),
      bookingCode = `N8${id.replaceAll("-", "").slice(0, 16).toUpperCase()}`;
    return tx.ticket.create({
      data: {
        id,
        orderId: order.id,
        bookingCode,
        qrTokenHash: tokenHash(
          ticketQrPayload(
            id,
            this.config.getOrThrow<string>("TICKET_QR_SECRET"),
          ),
        ),
        expiresAt: new Date(order.showtime.startAt.getTime() + 30 * 60_000),
        snapshot: this.snapshot(order, id, bookingCode),
      },
    });
  }
  private publish(
    order: Pick<FullOrder, "id" | "userId" | "showtimeId">,
    name: "PAYMENT_PAID" | "TICKET_REFUNDED",
    seats: string,
  ) {
    const data = {
      orderId: order.id,
      showtimeId: order.showtimeId,
      serverTime: new Date().toISOString(),
    };
    this.gateway.publish([
      { room: `user:${order.userId}`, name, data },
      { room: `showtime:${order.showtimeId}`, name: seats, data },
    ]);
  }
  private publicPayment(p: Payment, ticketId?: string | null) {
    const s = obj(p.snapshot);
    return {
      id: p.id,
      orderId: p.orderId,
      status: p.status,
      operationState: p.operationState,
      amount: p.expectedAmount.toString(),
      expiresAt: p.expiresAt.toISOString(),
      paymentLinkId: p.providerObjectId,
      checkoutUrl: s.checkoutUrl ?? null,
      qrCode: s.qrCode ?? null,
      description: s.description ?? null,
      accountName: s.accountName ?? null,
      accountNumber: s.accountNumber ?? null,
      bin: s.bin ?? null,
      ticketId: ticketId ?? null,
    };
  }
  private publicRefund(p: Payment) {
    return {
      id: p.id,
      status: p.status,
      operationState: p.operationState,
      amount: p.expectedAmount.toString(),
      accountMasked: obj(p.notificationState).recipientLabel ?? null,
      reason:
        p.issueReason ??
        (p.operationState === "NEEDS_REVIEW"
          ? "Cần xác minh hoặc hỗ trợ"
          : null),
      paidAt: p.paidAt?.toISOString() ?? null,
    };
  }
  async start(id: string, userId: string) {
    let paidOrder: FullOrder | null = null;
    const result = await this.db.$transaction(
      async (tx) => {
        const order = await this.lockOrder(tx, id, userId),
          now = await this.now(tx);
        if (order.ticket && order.status === "PAID")
          return { ticket: order.ticket, amount: order.totalAmount.toString() };
        if (order.status !== "PENDING_PAYMENT" || order.expiresAt <= now)
          throw fail("ORDER_CLOSED", "Đơn đã hết hiệu lực.");
        if (!order.checkoutConfirmedAt)
          throw fail("CHECKOUT_REQUIRED", "Hãy xác nhận báo giá trước.");
        const old = await tx.payment.findUnique({
          where: { intentOrderId: id },
        });
        if (old) return { payment: old };
        if (order.totalAmount.isZero()) {
          const ticket = await this.issue(tx, order, now);
          paidOrder = order;
          return { ticket, amount: "0" };
        }
        if (!this.enabled("PAYMENT_LIVE_ENABLED"))
          throw new ServiceUnavailableException({
            code: "PAYMENT_DISABLED",
            message: "Thanh toán thật chưa được mở.",
          });
        const [sequence] = await tx.$queryRaw<
          { code: bigint }[]
        >`SELECT nextval('payos_order_code_seq') AS code`;
        const p = await tx.payment.create({
          data: {
            orderId: id,
            intentOrderId: id,
            kind: "INTENT",
            paymentCode: `N8${sequence.code}`,
            providerOrderCode: sequence.code,
            expectedAmount: order.totalAmount,
            expiresAt: order.expiresAt,
          },
        });
        return { payment: p };
      },
      { timeout: 15000, maxWait: 10000 },
    );
    if (paidOrder) this.publish(paidOrder, "PAYMENT_PAID", "SEAT_SOLD");
    if ("ticket" in result && result.ticket)
      return this.wrap({
        id: result.ticket.id,
        orderId: id,
        status: "PAID",
        operationState: "SUCCEEDED",
        amount: result.amount,
        expiresAt: result.ticket.expiresAt.toISOString(),
        ticketId: result.ticket.id,
      });
    await this.processIntent(result.payment!.id);
    const p = await this.db.payment.findUniqueOrThrow({
      where: { id: result.payment!.id },
      include: { order: { select: { ticket: { select: { id: true } } } } },
    });
    return this.wrap(this.publicPayment(p, p.order?.ticket?.id));
  }
  private async lease(id: string) {
    const now = new Date();
    const r = await this.db.payment.updateMany({
      where: { id, OR: [{ leaseUntil: null }, { leaseUntil: { lte: now } }] },
      data: {
        leaseUntil: new Date(now.getTime() + 60_000),
        attempts: { increment: 1 },
        nextAttemptAt: new Date(now.getTime() + 15000),
      },
    });
    return r.count === 1;
  }
  private async processIntent(id: string) {
    if (!(await this.lease(id))) return;
    try {
      const p = await this.db.payment.findUniqueOrThrow({ where: { id } });
      if (p.kind !== "INTENT" || !p.orderId || p.providerOrderCode === null)
        return;
      if (p.operationState === "SUCCEEDED") return;
      if (p.providerObjectId || p.attempts > 1) {
        const link = await this.provider.getLink(Number(p.providerOrderCode));
        if (
          link.orderCode !== Number(p.providerOrderCode) ||
          String(link.amount) !== p.expectedAmount.toString()
        )
          throw new Error("Provider snapshot mismatch");
        if (!p.providerObjectId)
          await this.db.payment.update({
            where: { id },
            data: {
              providerObjectId: link.id,
              snapshot: json({
                checkoutUrl: `https://pay.payos.vn/web/${encodeURIComponent(link.id)}`,
              }),
            },
          });
        for (const receipt of link.transactions)
          await this.receive(p, receipt, link.id);
        const current = await this.db.payment.findUniqueOrThrow({
          where: { id },
        });
        if (["PENDING", "EXPIRED"].includes(current.status)) {
          if (
            p.expiresAt <= new Date() ||
            ["EXPIRED", "CANCELLED"].includes(link.status)
          ) {
            if (link.status === "PENDING")
              await this.provider.cancelLink(Number(p.providerOrderCode));
            await this.db.payment.update({
              where: { id },
              data: { operationState: "EXPIRED", status: "EXPIRED" },
            });
          } else if (link.status === "PENDING") {
            await this.db.payment.update({
              where: { id },
              data: { operationState: "PROCESSING" },
            });
          }
        }
        return;
      }
      if (!this.enabled("PAYMENT_LIVE_ENABLED") || p.expiresAt <= new Date())
        return;
      // Persisted lease/attempt precedes network; an ambiguous create is queried, never recreated.
      await this.db.payment.update({
        where: { id },
        data: { operationState: "PROCESSING" },
      });
      const base = this.config.getOrThrow<string>("PUBLIC_BASE_URL");
      const link = await this.provider.createLink({
        orderCode: Number(p.providerOrderCode),
        amount: Number(p.expectedAmount.toString()),
        description: p.paymentCode,
        expiredAt: Math.floor(p.expiresAt.getTime() / 1000),
        returnUrl: `${base}/payments/return`,
        cancelUrl: `${base}/payments/cancel`,
      });
      if (
        String(link.amount) !== p.expectedAmount.toString() ||
        !/^[a-zA-Z0-9]{4,34}$/.test(link.accountNumber) ||
        link.orderCode !== Number(p.providerOrderCode) ||
        link.currency !== "VND"
      )
        throw new Error("Provider snapshot mismatch");
      await this.db.payment.update({
        where: { id },
        data: {
          providerObjectId: link.paymentLinkId,
          operationState: "PROCESSING",
          snapshot: json({
            checkoutUrl: link.checkoutUrl,
            qrCode: link.qrCode,
            description: link.description,
            bin: link.bin,
            accountName: link.accountName,
            accountNumber: link.accountNumber,
          }),
        },
      });
    } catch {
      await this.db.payment.updateMany({
        where: { id, status: { not: "PAID" } },
        data: { operationState: "UNKNOWN" },
      });
    } finally {
      await this.db.payment.update({
        where: { id },
        data: { leaseUntil: null },
      });
    }
  }
  async paymentStatus(id: string, userId: string) {
    const order = await this.db.order.findFirst({
      where: { id, userId },
      include: {
        ticket: true,
        payments: { where: { kind: "INTENT" }, take: 1 },
      },
    });
    if (!order) throw missing();
    const p = order.payments[0];
    if (p) return this.wrap(this.publicPayment(p, order.ticket?.id));
    return this.wrap({
      id: order.id,
      orderId: order.id,
      status: order.status === "PENDING_PAYMENT" ? "PENDING" : order.status,
      operationState: order.ticket ? "SUCCEEDED" : "REQUESTED",
      amount: order.totalAmount.toString(),
      expiresAt: order.expiresAt.toISOString(),
      ticketId: order.ticket?.id ?? null,
    });
  }
  async check(id: string, userId: string) {
    const order = await this.db.order.findFirst({
      where: { id, userId },
      include: {
        ticket: true,
        payments: { where: { kind: "INTENT" }, take: 1 },
      },
    });
    if (!order) throw missing();
    if (!order.payments[0]) {
      if (order.ticket)
        return this.wrap({
          orderId: id,
          status: order.status,
          operationState: "SUCCEEDED",
          amount: order.totalAmount.toString(),
          expiresAt: order.expiresAt.toISOString(),
          ticketId: order.ticket.id,
        });
      return this.start(id, userId);
    }
    await this.processIntent(order.payments[0].id);
    const p = await this.db.payment.findUniqueOrThrow({
      where: { id: order.payments[0].id },
      include: { order: { include: { ticket: true } } },
    });
    return this.wrap(this.publicPayment(p, p.order?.ticket?.id));
  }
  async webhook(body: unknown) {
    let data;
    try {
      data = await this.provider.verify(body);
    } catch {
      throw new BadRequestException({
        code: "INVALID_SIGNATURE",
        message: "Webhook không hợp lệ.",
      });
    }
    if (data.code !== "00") return { received: true };
    // Official signed connection probes use a reserved code outside the N8 sequence.
    if (Number.isSafeInteger(data.orderCode) && data.orderCode < 1000000)
      return { received: true };
    if (
      data.currency !== "VND" ||
      !Number.isSafeInteger(data.amount) ||
      data.amount < 0 ||
      typeof data.reference !== "string" ||
      !data.reference ||
      data.reference.length > 128 ||
      typeof data.accountNumber !== "string" ||
      !/^[a-zA-Z0-9]{4,34}$/.test(data.accountNumber)
    )
      throw new BadRequestException({
        code: "INVALID_PAYMENT_DATA",
        message: "Dữ liệu giao dịch không hợp lệ.",
      });
    if (!Number.isSafeInteger(data.orderCode) || data.orderCode < 1000000)
      return { received: true };
    const link = await this.provider.getLink(data.orderCode);
    if (link.orderCode !== data.orderCode || link.id !== data.paymentLinkId)
      throw new BadRequestException("Provider reference mismatch");
    const transaction = link.transactions.find(
      (t) => t.reference === data.reference,
    );
    if (!transaction) return { received: true };
    const p = await this.db.payment.findUnique({
      where: { providerOrderCode: BigInt(data.orderCode) },
    });
    if (!p) {
      await this.db.payment.upsert({
        where: { providerTransactionId: `payos:${data.reference}` },
        create: {
          kind: "RECEIPT",
          paymentCode: `ISSUE-${randomUUID()}`,
          providerTransactionId: `payos:${data.reference}`,
          expectedAmount: 0,
          receivedAmount:
            Number.isSafeInteger(data.amount) && data.amount >= 0
              ? data.amount
              : 0,
          status: "NEEDS_ADMIN",
          operationState: "NEEDS_REVIEW",
          issueReason: "WRONG_REFERENCE",
          expiresAt: new Date(),
        },
        update: {},
      });
      return { received: true };
    }
    // Query authoritative transaction set; browser-return data and test pings cannot issue tickets.
    if (transaction) await this.receive(p, transaction, link.id);
    return { received: true };
  }
  private async receive(intent: Payment, t: Transaction, linkId: string) {
    if (!intent.orderId) return;
    let changed = false;
    const order = await this.db.$transaction(
      async (tx) => {
        const o = await this.lockOrder(tx, intent.orderId!),
          now = await this.now(tx);
        const transactionId = `payos:${t.reference}`;
        if (
          await tx.payment.findUnique({
            where: { providerTransactionId: transactionId },
          })
        )
          return o;
        let problem = collectionProblem(
          intent.expectedAmount.toString(),
          t.amount,
          o.status === "PENDING_PAYMENT",
          o.expiresAt,
          now,
        );
        if (
          (intent.providerObjectId && intent.providerObjectId !== linkId) ||
          !collectionAccountMatches(
            t.accountNumber,
            this.config.getOrThrow<string>("PAYOS_COLLECT_ACCOUNT_NUMBER"),
            obj(intent.snapshot).accountNumber,
          )
        )
          problem = "WRONG_AMOUNT";
        const currentIntent = await tx.payment.findUniqueOrThrow({
          where: { id: intent.id },
        });
        if (currentIntent.status === "NEEDS_ADMIN") problem = "WRONG_AMOUNT";
        if (o.showtime.status !== "ACTIVE" || o.ticket)
          problem = "LATE_PAYMENT";
        const held = await tx.showtimeSeat.count({
          where: {
            id: { in: o.seats.map((s) => s.showtimeSeatId) },
            status: "HELD",
            heldByUserId: o.userId,
            holdExpiresAt: o.expiresAt,
          },
        });
        if (held !== o.seats.length || !held) problem = "LATE_PAYMENT";
        const recipient = payer(t);
        const receipt = await tx.payment.create({
          data: {
            kind: "RECEIPT",
            parentId: intent.id,
            orderId: o.id,
            paymentCode: `RCV-${randomUUID()}`,
            expectedAmount: intent.expectedAmount,
            receivedAmount: t.amount,
            providerTransactionId: transactionId,
            transferContent: t.description,
            status: problem ? "NEEDS_ADMIN" : "PAID",
            operationState: problem ? "NEEDS_REVIEW" : "SUCCEEDED",
            issueReason: problem,
            expiresAt: o.expiresAt,
            paidAt: now,
            recipientCipher: recipient
              ? sealRecipient(
                  recipient,
                  this.config.getOrThrow<string>("PAYMENT_DATA_ENCRYPTION_KEY"),
                )
              : null,
          },
        });
        if (problem) {
          if (currentIntent.status !== "PAID")
            await tx.payment.update({
              where: { id: intent.id },
              data: {
                status: "NEEDS_ADMIN",
                operationState: "NEEDS_REVIEW",
                issueReason: problem,
              },
            });
          return o;
        }
        await this.issue(tx, o, now);
        await tx.payment.update({
          where: { id: intent.id },
          data: {
            status: "PAID",
            operationState: "SUCCEEDED",
            receivedAmount: receipt.receivedAmount,
            paidAt: now,
          },
        });
        changed = true;
        return o;
      },
      { timeout: 15000, maxWait: 10000 },
    );
    if (changed) this.publish(order, "PAYMENT_PAID", "SEAT_SOLD");
  }
  async orders(userId: string) {
    const now = await this.now(this.db);
    const orders = await this.db.order.findMany({
      where: {
        userId,
        status: { not: "EXPIRED" },
        OR: [
          { status: { not: "PENDING_PAYMENT" } },
          { expiresAt: { gt: now } },
        ],
      },
      include: {
        seats: true,
        ticket: true,
        payments: { where: { kind: "INTENT" }, take: 1 },
      },
      orderBy: [{ createdAt: "desc" }, { id: "desc" }],
      take: 100,
    });
    return {
      serverTime: new Date().toISOString(),
      items: orders.map((o) => ({
        id: o.id,
        showtimeId: o.showtimeId,
        status: o.status,
        totalAmount: o.totalAmount.toString(),
        bankAmount: o.totalAmount.toString(),
        expiresAt: o.expiresAt.toISOString(),
        seats: o.seats.map((s) => ({
          id: s.showtimeSeatId,
          seatCode: s.seatCode,
          price: s.price.toString(),
        })),
        payment: o.payments[0]
          ? this.publicPayment(o.payments[0], o.ticket?.id)
          : null,
        ticket: o.ticket ? { id: o.ticket.id, status: o.ticket.status } : null,
      })),
    };
  }
  private async ticketItem(
    t: Ticket & {
      order: {
        status: string;
        showtime: { startAt: Date };
        payments: Payment[];
      };
    },
    now: Date,
  ) {
    const refund = t.order.payments.find((p) => p.kind === "REFUND");
    return {
      id: t.id,
      orderId: t.orderId,
      status: t.status,
      bookingCode: t.bookingCode,
      issuedAt: t.issuedAt.toISOString(),
      expiresAt: t.expiresAt.toISOString(),
      snapshot: t.snapshot,
      refundEligible: canRefund(
        t.order.status,
        t,
        t.order.showtime.startAt,
        now,
      ),
      refundReason: canRefund(t.order.status, t, t.order.showtime.startAt, now)
        ? null
        : "Vé đã dùng/đang hoàn hoặc còn dưới 45 phút trước suất chiếu.",
      accountMasked: refund
        ? (obj(refund.notificationState).recipientLabel ?? null)
        : null,
      refund: refund ? this.publicRefund(refund) : null,
    };
  }
  async tickets(userId: string, cursor?: string) {
    if (
      cursor &&
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
        cursor,
      )
    )
      throw new BadRequestException("Invalid cursor");
    const items = await this.db.ticket.findMany({
      where: { order: { userId } },
      include: {
        order: {
          include: { showtime: true, payments: { where: { kind: "REFUND" } } },
        },
      },
      orderBy: [{ issuedAt: "desc" }, { id: "desc" }],
      take: 20,
      ...(cursor ? { cursor: { id: cursor }, skip: 1 } : {}),
    });
    return {
      serverTime: new Date().toISOString(),
      items: await Promise.all(
        items.map((t) => this.ticketItem(t, new Date())),
      ),
      nextCursor: items.length === 20 ? items.at(-1)!.id : null,
    };
  }
  async ticket(id: string, userId: string) {
    const t = await this.db.ticket.findFirst({
      where: { id, order: { userId } },
      include: {
        order: {
          include: { showtime: true, payments: { where: { kind: "REFUND" } } },
        },
      },
    });
    if (!t) throw missing();
    return this.wrap(await this.ticketItem(t, new Date()));
  }
  async qr(id: string, userId: string) {
    const t = await this.db.ticket.findFirst({
      where: { id, order: { userId, status: "PAID" } },
    });
    if (!t) throw missing();
    if (t.status !== "VALID" || t.expiresAt <= new Date())
      throw fail("TICKET_NOT_VALID", "Vé không còn giá trị sử dụng.");
    return this.wrap({
      payload: ticketQrPayload(
        t.id,
        this.config.getOrThrow<string>("TICKET_QR_SECRET"),
      ),
    });
  }
  async refund(id: string, userId: string) {
    const found = await this.db.ticket.findFirst({
      where: { id, order: { userId } },
      select: { orderId: true },
    });
    if (!found) throw missing();
    let changed = false;
    const result = await this.db.$transaction(
      async (tx) => {
        const order = await this.lockOrder(tx, found.orderId, userId),
          now = await this.now(tx);
        const old = await tx.payment.findUnique({
          where: { refundOrderId: order.id },
        });
        if (old) return { refund: old, order };
        if (
          !order.ticket ||
          !canRefund(order.status, order.ticket, order.showtime.startAt, now)
        )
          throw fail("REFUND_NOT_ELIGIBLE", "Vé không đủ điều kiện hoàn.");
        const receipt = await tx.payment.findFirst({
          where: {
            orderId: order.id,
            kind: "RECEIPT",
            status: "PAID",
            operationState: "SUCCEEDED",
          },
        });
        const zero = order.totalAmount.isZero();
        const receiptMatches =
          receipt?.receivedAmount?.equals(order.totalAmount) &&
          receipt.expectedAmount.equals(order.totalAmount);
        const recipient =
          receiptMatches && receipt?.recipientCipher
            ? openRecipient(
                receipt.recipientCipher,
                this.config.getOrThrow<string>("PAYMENT_DATA_ENCRYPTION_KEY"),
              )
            : null;
        const retryId =
          !zero && recipient && this.enabled("REFUND_RETRY_ENABLED")
            ? randomUUID()
            : undefined;
        const p = await tx.payment.create({
          data: {
            id: retryId,
            snapshot: retryId
              ? json({ refundRetry: initialRefundRetry(retryId) })
              : undefined,
            kind: "REFUND",
            orderId: order.id,
            refundOrderId: order.id,
            parentId: receipt?.id,
            paymentCode: `REF-${randomUUID()}`,
            expectedAmount: order.totalAmount,
            recipientCipher: receipt?.recipientCipher,
            expiresAt: order.showtime.startAt,
            operationState: zero
              ? "SUCCEEDED"
              : recipient
                ? "REQUESTED"
                : "NEEDS_REVIEW",
            status: zero ? "RESOLVED" : recipient ? "PENDING" : "NEEDS_ADMIN",
            paidAt: zero ? now : null,
            notificationState: json({ recipientLabel: maskAccount(recipient) }),
          },
        });
        const released = await tx.showtimeSeat.updateMany({
          where: {
            id: { in: order.seats.map((s) => s.showtimeSeatId) },
            status: "SOLD",
            orderSeats: { some: { orderId: order.id, status: "ACTIVE" } },
          },
          data: { status: "AVAILABLE" },
        });
        if (released.count !== order.seats.length)
          throw fail("SEAT_STATE_CHANGED", "Trạng thái ghế cần kiểm tra.");
        await tx.orderSeat.updateMany({
          where: { orderId: order.id, status: "ACTIVE" },
          data: { status: "REFUNDED" },
        });
        await tx.ticket.update({
          where: { id },
          data: {
            status: zero ? "REFUNDED" : "REFUND_PENDING",
            refundedAt: zero ? now : null,
          },
        });
        await tx.order.update({
          where: { id: order.id },
          data: { status: zero ? "REFUNDED" : "REFUND_PENDING" },
        });
        changed = true;
        return { refund: p, order };
      },
      { timeout: 15000, maxWait: 10000 },
    );
    if (changed) this.publish(result.order, "TICKET_REFUNDED", "SEAT_RELEASED");
    return this.wrap(this.publicRefund(result.refund));
  }
  async refundState(id: string, userId: string) {
    const p = await this.db.payment.findFirst({
      where: { kind: "REFUND", order: { userId, ticket: { id } } },
    });
    if (!p) throw missing();
    return this.wrap(this.publicRefund(p));
  }
  private async processRefund(id: string) {
    const candidate = await this.db.payment.findUnique({ where: { id } });
    if (candidate && obj(candidate.snapshot).refundRetry !== undefined)
      return this.processRetryRefund(id);
    if (!(await this.lease(id))) return;
    try {
      const p = await this.db.payment.findUniqueOrThrow({ where: { id } });
      if (
        p.kind !== "REFUND" ||
        !p.orderId ||
        !p.recipientCipher ||
        !["REQUESTED", "PROCESSING", "UNKNOWN"].includes(p.operationState)
      )
        return;
      let result: Payout | undefined;
      if (p.providerObjectId)
        result = await this.provider.getPayout(p.providerObjectId);
      else if (p.attempts > 1) {
        const matches = await this.provider.findPayout(p.paymentCode);
        if (matches.length === 1) result = matches[0];
      } else if (this.enabled("REFUND_PAYOUT_ENABLED")) {
        const r = openRecipient(
          p.recipientCipher,
          this.config.getOrThrow<string>("PAYMENT_DATA_ENCRYPTION_KEY"),
        );
        await this.db.payment.update({
          where: { id },
          data: { operationState: "PROCESSING" },
        });
        result = await this.provider.createPayout(
          {
            referenceId: p.paymentCode,
            amount: Number(p.expectedAmount.toString()),
            description: `N8 hoan ${p.id.slice(0, 8)}`,
            toBin: r.bin,
            toAccountNumber: r.account,
          },
          p.id,
        );
      }
      if (!result) {
        if (p.attempts > 1)
          await this.db.payment.update({
            where: { id },
            data: { operationState: "UNKNOWN" },
          });
        return;
      }
      const r = openRecipient(
        p.recipientCipher,
        this.config.getOrThrow<string>("PAYMENT_DATA_ENCRYPTION_KEY"),
      );
      const ts = result.transactions;
      const valid =
        result.referenceId === p.paymentCode &&
        ts.length === 1 &&
        ts[0].referenceId === p.paymentCode &&
        String(ts[0].amount) === p.expectedAmount.toString() &&
        ts[0].toBin === r.bin &&
        ts[0].toAccountNumber === r.account;
      const success = valid && ts[0].state === "SUCCEEDED";
      const review =
        !valid ||
        ["FAILED", "CANCELLED", "REVERSED"].includes(ts[0]?.state ?? "");
      await this.db.$transaction(
        async (tx) => {
          const order = await this.lockOrder(tx, p.orderId!);
          const now = await this.now(tx);
          const current = await tx.payment.findUniqueOrThrow({ where: { id } });
          if (current.operationState === "SUCCEEDED") return;
          await tx.payment.update({
            where: { id },
            data: {
              providerObjectId: result!.id,
              status: success ? "RESOLVED" : review ? "NEEDS_ADMIN" : "PENDING",
              operationState: success
                ? "SUCCEEDED"
                : review
                  ? "NEEDS_REVIEW"
                  : "PROCESSING",
              paidAt: success ? now : null,
            },
          });
          if (success) {
            await tx.order.update({
              where: { id: order.id },
              data: { status: "REFUNDED" },
            });
            await tx.ticket.update({
              where: { orderId: order.id },
              data: { status: "REFUNDED", refundedAt: now },
            });
          }
        },
        { timeout: 15000, maxWait: 10000 },
      );
    } catch {
      await this.db.payment.updateMany({
        where: { id, operationState: { not: "SUCCEEDED" } },
        data: { operationState: "UNKNOWN" },
      });
    } finally {
      await this.db.payment.update({
        where: { id },
        data: { leaseUntil: null },
      });
    }
  }
  private async processRetryRefund(id: string) {
    const now = await this.now(this.db);
    const claimed = await this.db.payment.updateMany({
      where: {
        id,
        kind: "REFUND",
        OR: [
          { operationState: { in: ["REQUESTED", "PROCESSING", "UNKNOWN"] } },
          {
            operationState: "NEEDS_REVIEW",
            snapshot: { path: ["refundRetry", "reconcileOnly"], equals: true },
          },
        ],
        AND: [
          { OR: [{ leaseUntil: null }, { leaseUntil: { lte: now } }] },
          { OR: [{ nextAttemptAt: null }, { nextAttemptAt: { lte: now } }] },
        ],
      },
      data: {
        leaseUntil: new Date(now.getTime() + 120_000),
        nextAttemptAt: refundRetryAt(now),
        attempts: { increment: 1 },
      },
    });
    if (claimed.count !== 1) return;
    let p: Payment | undefined;
    let state: ReturnType<typeof readRefundRetry> | undefined;
    let result: Payout | undefined;
    let review = false;
    let retryAfter: string | null | undefined;
    let phase = "query";
    let invalid = false;
    const recordError = async (error: unknown, stage = phase) => {
      const event = safeRefundError(
        error,
        stage,
        new Date(),
        Date.now() - now.getTime(),
        state?.cycles ?? 0,
        state?.sendAttempts ?? 0,
      );
      console.warn(
        "refund_failure",
        JSON.stringify({ refundId: id, ...event }),
      );
      if (state && p) {
        state.firstError ??= event;
        state.lastError = event;
        state.errors = [...(state.errors ?? []).slice(-7), event];
        await this.db.payment.update({
          where: { id },
          data: { snapshot: json({ ...obj(p.snapshot), refundRetry: state }) },
        });
      }
    };
    try {
      p = await this.db.payment.findUniqueOrThrow({ where: { id } });
      if (!p.orderId || !p.recipientCipher) {
        review = true;
        return;
      }
      state = readRefundRetry(obj(p.snapshot).refundRetry);
      state.cycles++;
      await this.db.payment.update({
        where: { id },
        data: {
          snapshot: json({ ...obj(p.snapshot), refundRetry: state }),
          operationState: "UNKNOWN",
        },
      });
      const recipient = openRecipient(
        p.recipientCipher,
        this.config.getOrThrow<string>("PAYMENT_DATA_ENCRYPTION_KEY"),
      );
      const results = await this.provider.findPayout(p.paymentCode);
      const choice = refundRetryDecision(
        results,
        state,
        p.paymentCode,
        p.expectedAmount.toString(),
        recipient,
      );
      if (choice.action === "REVIEW") {
        review = true;
        invalid = true;
        state.reconcileOnly = false;
        await recordError({ code: "PAYOUT_VALIDATION" }, "validation");
        return;
      }
      if (choice.action === "SUCCESS") {
        result = choice.result;
        return;
      }
      if (
        choice.action === "SEND" &&
        !state.reconcileOnly &&
        (state.sendAttempts ?? 0) < REFUND_RETRY_LIMIT &&
        this.enabled("REFUND_PAYOUT_ENABLED") &&
        this.enabled("REFUND_RETRY_ENABLED")
      ) {
        if (choice.newAttempt) state.key = `${id}:retry:${state.cycles}`;
        if (!state.dispatched || choice.newAttempt) {
          state.baseIds = results.map((p) => p.id);
          state.dispatches++;
        }
        state.dispatched = true;
        state.sendAttempts = (state.sendAttempts ?? 0) + 1;
        phase = "send";
        await this.db.payment.update({
          where: { id },
          data: { snapshot: json({ ...obj(p.snapshot), refundRetry: state }) },
        });
        const sent = await this.provider.createPayout(
          {
            referenceId: p.paymentCode,
            amount: Number(p.expectedAmount.toString()),
            description: `N8 hoan ${id.slice(0, 8)}`,
            toBin: recipient.bin,
            toAccountNumber: recipient.account,
          },
          state.key,
        );
        const verified = refundRetryDecision(
          [sent],
          { ...state, baseIds: [] },
          p.paymentCode,
          p.expectedAmount.toString(),
          recipient,
        );
        if (verified.action === "REVIEW") {
          review = true;
          invalid = true;
          state.reconcileOnly = false;
          await recordError({ code: "PAYOUT_VALIDATION" }, "validation");
          return;
        }
        if (sent.transactions[0].state === "FAILED")
          await recordError(
            { code: sent.transactions[0].errorCode },
            "provider",
          );
        await this.db.payment.update({
          where: { id },
          data: { providerObjectId: sent.id },
        });
        if (verified.action === "SUCCESS") result = sent;
      }
    } catch (error) {
      // Never log SDK errors/headers: they can contain credentials or beneficiary details.
      const headers = (error as { headers?: Headers })?.headers;
      retryAfter = headers?.get?.("retry-after");
      if (!state) review = true;
      await recordError(error);
    } finally {
      const completedAt = await this.now(this.db);
      const next = refundRetryAt(completedAt, retryAfter);
      const success = !!result;
      if (
        !success &&
        state &&
        !invalid &&
        ((state.sendAttempts ?? 0) >= REFUND_RETRY_LIMIT || state.reconcileOnly)
      ) {
        review = true;
        state.reconcileOnly = true;
      }
      if (state && p)
        await this.db.payment.update({
          where: { id },
          data: { snapshot: json({ ...obj(p.snapshot), refundRetry: state }) },
        });
      await this.db.$transaction(async (tx) => {
        const current = await tx.payment.findUniqueOrThrow({ where: { id } });
        if (current.operationState === "SUCCEEDED") return;
        if (success && p?.orderId) {
          const order = await this.lockOrder(tx, p.orderId);
          if (order.status !== "REFUND_PENDING")
            throw fail("REFUND_STATE_CHANGED", "Trạng thái hoàn cần kiểm tra.");
          await tx.order.update({
            where: { id: order.id },
            data: { status: "REFUNDED" },
          });
          await tx.ticket.update({
            where: { orderId: order.id },
            data: { status: "REFUNDED", refundedAt: completedAt },
          });
        }
        await tx.payment.update({
          where: { id },
          data: {
            status: success ? "RESOLVED" : review ? "NEEDS_ADMIN" : "PENDING",
            operationState: success
              ? "SUCCEEDED"
              : review
                ? "NEEDS_REVIEW"
                : "PROCESSING",
            providerObjectId: result?.id ?? current.providerObjectId,
            paidAt: success ? completedAt : null,
            nextAttemptAt:
              success || (review && !state?.reconcileOnly) ? null : next,
            leaseUntil: null,
          },
        });
      });
    }
  }
  async issues() {
    const items = await this.db.payment.findMany({
      where: { status: "NEEDS_ADMIN", kind: { in: ["RECEIPT", "REFUND"] } },
      orderBy: { createdAt: "asc" },
      take: 100,
    });
    return {
      items: items.map((p) => ({
        ...this.publicRefund(p),
        orderId: p.orderId,
        kind: p.kind,
        providerTransactionId: p.providerTransactionId,
        providerObjectId: p.providerObjectId,
      })),
    };
  }
  async resolve(id: string, actor: string, action: string, note: string) {
    const p = await this.db.payment.findUnique({ where: { id } });
    if (!p || p.status !== "NEEDS_ADMIN") throw missing();
    if (p.kind === "INTENT")
      throw fail(
        "RECEIPT_REQUIRED",
        "Chỉ xử lý khoản thực nhận hoặc nghĩa vụ hoàn, không hoàn lần nữa từ intent.",
      );
    if (
      !["MARK_BANK_REFUNDED", "MARK_RESOLVED"].includes(action) ||
      note.trim().length < 8
    )
      throw new BadRequestException("Reference/note required");
    if (
      p.operationState === "UNKNOWN" ||
      (p.leaseUntil && p.leaseUntil > new Date())
    )
      throw fail(
        "PAYOUT_UNCERTAIN",
        "Phải xác minh không còn lệnh chi đang chờ.",
      );
    if (p.providerObjectId && p.kind === "REFUND") {
      const payout = await this.provider.getPayout(p.providerObjectId);
      const recipient = p.recipientCipher
        ? openRecipient(
            p.recipientCipher,
            this.config.getOrThrow<string>("PAYMENT_DATA_ENCRYPTION_KEY"),
          )
        : null;
      const transaction = payout.transactions[0];
      if (
        !recipient ||
        payout.id !== p.providerObjectId ||
        payout.referenceId !== p.paymentCode ||
        payout.transactions.length !== 1 ||
        transaction.referenceId !== p.paymentCode ||
        String(transaction.amount) !== p.expectedAmount.toString() ||
        transaction.toBin !== recipient.bin ||
        transaction.toAccountNumber !== recipient.account ||
        !["FAILED", "CANCELLED"].includes(transaction.state)
      )
        throw fail(
          "PAYOUT_UNCERTAIN",
          "Lệnh chi chưa được xác nhận thất bại dứt điểm.",
        );
    }
    if (p.kind === "REFUND" && action !== "MARK_BANK_REFUNDED")
      throw fail(
        "REFUND_DEBT_OPEN",
        "Không đóng nghĩa vụ hoàn khi chưa trả tiền.",
      );
    return this.wrap(
      await this.db.$transaction(async (tx) => {
        if (p.orderId) await this.lockOrder(tx, p.orderId);
        const current = await tx.payment.findUniqueOrThrow({ where: { id } });
        if (current.status !== "NEEDS_ADMIN")
          throw fail("ISSUE_ALREADY_RESOLVED", "Vấn đề đã được xử lý.");
        if (
          current.operationState !== p.operationState ||
          current.providerObjectId !== p.providerObjectId ||
          (current.leaseUntil && current.leaseUntil > new Date())
        )
          throw fail(
            "PAYOUT_UNCERTAIN",
            "Lệnh chi đã thay đổi; hãy kiểm tra lại.",
          );
        const inFlight = await tx.payment.count({
          where: {
            parentId: p.id,
            kind: "REFUND",
            operationState: {
              in: ["REQUESTED", "PROCESSING", "UNKNOWN", "SUCCEEDED"],
            },
          },
        });
        if (inFlight)
          throw fail(
            "PAYOUT_UNCERTAIN",
            "Khoản thu đã có nghĩa vụ hoàn đang xử lý hoặc đã hoàn.",
          );
        const updated = await tx.payment.update({
          where: { id },
          data: {
            status: "RESOLVED",
            operationState:
              action === "MARK_BANK_REFUNDED" ? "SUCCEEDED" : "NEEDS_REVIEW",
            resolutionNote: note,
            resolvedBy: actor,
            paidAt: action === "MARK_BANK_REFUNDED" ? await this.now(tx) : null,
          },
        });
        if (p.kind === "REFUND" && p.orderId) {
          await tx.order.update({
            where: { id: p.orderId },
            data: { status: "REFUNDED" },
          });
          await tx.ticket.update({
            where: { orderId: p.orderId },
            data: { status: "REFUNDED", refundedAt: await this.now(tx) },
          });
        }
        return this.publicRefund(updated);
      }),
    );
  }
  async pruneExpiredOrders() {
    return this.db.$transaction(async (tx) => {
      // Keep provider references for late receipts/refund reconciliation; prune only empty holds.
      const rows = await tx.$queryRaw<{ id: string }[]>`
        SELECT o.id FROM orders o WHERE o.status='EXPIRED' AND o.expires_at<=clock_timestamp()
        AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.order_id=o.id OR p.intent_order_id=o.id OR p.refund_order_id=o.id)
        AND NOT EXISTS (SELECT 1 FROM tickets t WHERE t.order_id=o.id)
        AND NOT EXISTS (SELECT 1 FROM order_seats s WHERE s.order_id=o.id AND s.status<>'CANCELED')
        ORDER BY o.expires_at LIMIT 100 FOR UPDATE OF o SKIP LOCKED`;
      const ids = rows.map((o) => o.id);
      if (!ids.length) return 0;
      await tx.orderSeat.deleteMany({ where: { orderId: { in: ids } } });
      const deleted = await tx.order.deleteMany({
        where: {
          id: { in: ids },
          status: "EXPIRED",
          payments: { none: {} },
          ticket: null,
        },
      });
      return deleted.count;
    });
  }
  @Interval(15000)
  async tick() {
    if (this.busy) return;
    this.busy = true;
    try {
      const now = new Date();
      const due = await this.db.payment.findMany({
        where: {
          kind: { in: ["INTENT", "REFUND"] },
          AND: [
            {
              OR: [
                {
                  operationState: {
                    in: ["REQUESTED", "PROCESSING", "UNKNOWN"],
                  },
                },
                {
                  kind: "REFUND",
                  operationState: "NEEDS_REVIEW",
                  snapshot: {
                    path: ["refundRetry", "reconcileOnly"],
                    equals: true,
                  },
                },
              ],
            },
            { OR: [{ nextAttemptAt: null }, { nextAttemptAt: { lte: now } }] },
          ],
        },
        take: 20,
        orderBy: { createdAt: "asc" },
      });
      for (const p of due) {
        if (p.kind === "INTENT") await this.processIntent(p.id);
        else if (
          this.enabled("REFUND_PAYOUT_ENABLED") ||
          p.operationState !== "REQUESTED"
        )
          await this.processRefund(p.id);
      }
      await this.pruneExpiredOrders();
      await this.db.ticket.updateMany({
        where: { status: "VALID", expiresAt: { lte: now } },
        data: { status: "EXPIRED" },
      });
    } finally {
      this.busy = false;
    }
  }
}
