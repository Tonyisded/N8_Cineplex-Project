import {
  ConflictException,
  ForbiddenException,
  Injectable,
  Logger,
  NotFoundException,
  type OnApplicationBootstrap,
} from "@nestjs/common";
import { Interval } from "@nestjs/schedule";
import { Prisma } from "../generated/prisma/client.js";
import { PrismaService } from "../prisma.service.js";
import { BookingGateway, type BookingEvent } from "./booking.gateway.js";
import type { CreateOrderDto } from "./dto.js";

type Tx = Prisma.TransactionClient;
type Order = Prisma.OrderGetPayload<{ include: { seats: true } }>;
const conflict = (code: string, message: string) =>
  new ConflictException({ code, message });
const missing = () =>
  new NotFoundException({
    code: "ORDER_NOT_FOUND",
    message: "Không tìm thấy đơn giữ chỗ.",
  });

export function completeCouples(
  seats: {
    seatCode: string;
    rowName: string;
    seatNumber: number;
    seatType: string;
  }[],
) {
  const codes = new Map(seats.map((s) => [s.seatCode, s]));
  return seats.every(
    (s) =>
      s.seatType !== "COUPLE" ||
      codes.get(
        `${s.rowName}${s.seatNumber % 2 ? s.seatNumber + 1 : s.seatNumber - 1}`,
      )?.seatType === "COUPLE",
  );
}
export function publicOrder(order: Order) {
  return {
    id: order.id,
    showtimeId: order.showtimeId,
    status: order.status,
    subtotal: order.subtotal.toString(),
    discount: order.discount.toString(),
    creditUsed: order.creditUsed.toString(),
    totalAmount: order.totalAmount.toString(),
    bankAmount: order.totalAmount.minus(order.creditUsed).toString(),
    expiresAt: order.expiresAt.toISOString(),
    seats: order.seats.map((s) => ({
      id: s.showtimeSeatId,
      seatCode: s.seatCode,
      price: s.price.toString(),
    })),
  };
}

@Injectable()
export class BookingService implements OnApplicationBootstrap {
  private running = false;
  private readonly logger = new Logger(BookingService.name);
  constructor(
    private readonly prisma: PrismaService,
    private readonly gateway: BookingGateway,
  ) {}

  private async lockShowtime(tx: Tx, id: string) {
    const rows = await tx.$queryRaw<
      { id: string }[]
    >`SELECT id FROM showtimes WHERE id=${id}::uuid FOR UPDATE`;
    if (!rows.length)
      throw new NotFoundException({
        code: "SHOWTIME_NOT_FOUND",
        message: "Không tìm thấy suất chiếu.",
      });
  }
  private async lockUsers(tx: Tx, ids: string[]) {
    const unique = [...new Set(ids)].sort();
    await tx.$queryRaw(
      Prisma.sql`SELECT id FROM users WHERE id IN (${Prisma.join(unique.map((id) => Prisma.sql`${id}::uuid`))}) ORDER BY id FOR UPDATE`,
    );
  }
  private async now(tx: Tx) {
    const [row] = await tx.$queryRaw<
      { now: Date }[]
    >`SELECT clock_timestamp() AS now`;
    return row.now;
  }
  private async finish(
    tx: Tx,
    order: Order,
    status: "EXPIRED" | "CANCELED",
    events: BookingEvent[],
  ) {
    if (order.status !== "PENDING_PAYMENT") return order;
    await tx.$queryRaw(
      Prisma.sql`SELECT id FROM showtime_seats WHERE id IN (${Prisma.join(order.seats.map((s) => Prisma.sql`${s.showtimeSeatId}::uuid`))}) ORDER BY id FOR UPDATE`,
    );
    await tx.showtimeSeat.updateMany({
      where: {
        id: { in: order.seats.map((s) => s.showtimeSeatId) },
        status: "HELD",
        heldByUserId: order.userId,
        holdExpiresAt: order.expiresAt,
      },
      data: { status: "AVAILABLE", heldByUserId: null, holdExpiresAt: null },
    });
    await tx.orderSeat.updateMany({
      where: { orderId: order.id, status: "HELD" },
      data: { status: "CANCELED" },
    });
    await tx.payment.updateMany({
      where: { orderId: order.id, status: "PENDING" },
      data: { status: "EXPIRED" },
    });
    if (order.creditUsed.greaterThan(0))
      await tx.user.update({
        where: { id: order.userId },
        data: { creditBalance: { increment: order.creditUsed } },
      });
    const result = await tx.order.update({
      where: { id: order.id },
      data: { status },
      include: { seats: true },
    });
    const data = {
      showtimeId: order.showtimeId,
      serverTime: new Date().toISOString(),
    };
    events.push({
      room: `showtime:${order.showtimeId}`,
      name: "SEAT_RELEASED",
      data,
    });
    events.push({
      room: `user:${order.userId}`,
      name: status === "EXPIRED" ? "ORDER_EXPIRED" : "SEAT_RELEASED",
      data: { ...data, orderId: order.id },
    });
    return result;
  }
  private async expired(
    tx: Tx,
    showtimeId: string,
    now: Date,
    userId?: string,
  ) {
    const due = await tx.order.findMany({
      where: { showtimeId, status: "PENDING_PAYMENT", expiresAt: { lte: now } },
      include: { seats: true },
      orderBy: { id: "asc" },
    });
    const ids = due.map((o) => o.userId);
    if (userId) ids.push(userId);
    if (ids.length) await this.lockUsers(tx, ids);
    return due;
  }
  async create(userId: string, key: string, body: CreateOrderDto) {
    const events: BookingEvent[] = [];
    const order = await this.prisma.$transaction(
      async (tx) => {
        await this.lockShowtime(tx, body.showtime_id);
        const now = await this.now(tx);
        for (const due of await this.expired(tx, body.showtime_id, now, userId))
          await this.finish(tx, due, "EXPIRED", events);
        const previous = await tx.order.findUnique({
          where: { id: key },
          include: { seats: true },
        });
        if (previous) {
          if (previous.userId !== userId) throw missing();
          if (
            previous.showtimeId !== body.showtime_id ||
            previous.seats
              .map((s) => s.showtimeSeatId)
              .sort()
              .join() !== [...body.seat_ids].sort().join()
          )
            throw conflict(
              "IDEMPOTENCY_CONFLICT",
              "Yêu cầu giữ ghế đã được dùng cho lựa chọn khác.",
            );
          return previous;
        }
        const user = await tx.user.findUnique({ where: { id: userId } });
        if (
          !user?.isActive ||
          user.role !== "CUSTOMER" ||
          !user.emailVerifiedAt
        )
          throw new ForbiddenException({
            code: "EMAIL_NOT_VERIFIED",
            message: "Hãy đăng nhập và xác minh email trước khi đặt vé.",
          });
        const slot = await tx.showtime.findUnique({
          where: { id: body.showtime_id },
          include: { auditorium: true },
        });
        if (
          !slot ||
          slot.status !== "ACTIVE" ||
          slot.auditorium.status !== "ACTIVE" ||
          slot.startAt <= now
        )
          throw conflict(
            "SHOWTIME_UNAVAILABLE",
            "Suất chiếu đã bắt đầu hoặc không còn nhận đặt chỗ.",
          );
        await tx.$queryRaw(
          Prisma.sql`SELECT id FROM showtime_seats WHERE id IN (${Prisma.join([...body.seat_ids].sort().map((id) => Prisma.sql`${id}::uuid`))}) ORDER BY id FOR UPDATE`,
        );
        const seats = await tx.showtimeSeat.findMany({
          where: { id: { in: body.seat_ids }, showtimeId: slot.id },
          orderBy: [{ rowName: "asc" }, { seatNumber: "asc" }],
        });
        if (
          seats.length !== body.seat_ids.length ||
          seats.some((s) => s.status !== "AVAILABLE")
        )
          throw conflict(
            "SEAT_UNAVAILABLE",
            "Một số ghế vừa được người khác giữ hoặc không còn khả dụng. Vui lòng chọn lại.",
          );
        if (!completeCouples(seats))
          throw conflict(
            "COUPLE_INCOMPLETE",
            "Vui lòng chọn đủ hai chỗ của ghế Couple.",
          );
        const total = seats
          .reduce((sum, s) => sum + BigInt(s.price.toString()), 0n)
          .toString();
        const holdTime = await this.now(tx);
        if (slot.startAt <= holdTime)
          throw conflict("SHOWTIME_UNAVAILABLE", "Suất chiếu đã bắt đầu.");
        const expiresAt = new Date(holdTime.getTime() + 600_000);
        const created = await tx.order.create({
          data: {
            id: key,
            userId,
            showtimeId: slot.id,
            comboId: null,
            comboQuantity: 0,
            subtotal: total,
            discount: "0",
            creditUsed: "0",
            totalAmount: total,
            expiresAt,
            seats: {
              create: seats.map((s) => ({
                showtimeSeatId: s.id,
                seatCode: s.seatCode,
                price: s.price,
              })),
            },
          },
          include: { seats: true },
        });
        const changed = await tx.showtimeSeat.updateMany({
          where: { id: { in: seats.map((s) => s.id) }, status: "AVAILABLE" },
          data: {
            status: "HELD",
            heldByUserId: userId,
            holdExpiresAt: expiresAt,
          },
        });
        if (changed.count !== seats.length)
          throw conflict("SEAT_UNAVAILABLE", "Ghế không còn khả dụng.");
        events.push({
          room: `showtime:${slot.id}`,
          name: "SEAT_HELD",
          data: { showtimeId: slot.id, serverTime: new Date().toISOString() },
        });
        return created;
      },
      { maxWait: 10000, timeout: 15000 },
    );
    this.gateway.publish(events);
    return { serverTime: new Date().toISOString(), item: publicOrder(order) };
  }

  private async transition(id: string, userId?: string, cancel = false) {
    const found = await this.prisma.order.findUnique({
      where: { id },
      select: { showtimeId: true, userId: true },
    });
    if (!found || (userId && found.userId !== userId)) throw missing();
    const events: BookingEvent[] = [];
    const order = await this.prisma.$transaction(
      async (tx) => {
        await this.lockShowtime(tx, found.showtimeId);
        await this.lockUsers(tx, [found.userId]);
        await tx.$queryRaw`SELECT id FROM orders WHERE id=${id}::uuid FOR UPDATE`;
        const current = await tx.order.findUnique({
          where: { id },
          include: { seats: true },
        });
        if (!current || current.userId !== found.userId) throw missing();
        const now = await this.now(tx);
        if (current.status === "PENDING_PAYMENT" && current.expiresAt <= now)
          return this.finish(tx, current, "EXPIRED", events);
        if (cancel && current.status === "PENDING_PAYMENT")
          return this.finish(tx, current, "CANCELED", events);
        if (
          cancel &&
          current.status !== "CANCELED" &&
          current.status !== "EXPIRED"
        )
          throw conflict(
            "ORDER_NOT_CANCELABLE",
            "Không thể hủy giữ chỗ của đơn này.",
          );
        return current;
      },
      { maxWait: 10000, timeout: 15000 },
    );
    this.gateway.publish(events);
    return { serverTime: new Date().toISOString(), item: publicOrder(order) };
  }
  get(id: string, userId: string) {
    return this.transition(id, userId);
  }
  cancel(id: string, userId: string) {
    return this.transition(id, userId, true);
  }

  onApplicationBootstrap() {
    return this.expire();
  }
  @Interval(1000)
  async expire() {
    if (this.running) return;
    this.running = true;
    try {
      const due = await this.prisma.order.findMany({
        where: { status: "PENDING_PAYMENT", expiresAt: { lte: new Date() } },
        select: { id: true },
        take: 100,
        orderBy: { expiresAt: "asc" },
      });
      for (const item of due) await this.transition(item.id);
    } catch {
      this.logger.error("Seat expiry failed; the next tick will retry.");
    } finally {
      this.running = false;
    }
  }
}
