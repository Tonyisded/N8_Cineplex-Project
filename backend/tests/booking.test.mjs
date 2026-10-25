import "reflect-metadata";
import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { ValidationPipe } from "@nestjs/common";
import { Test } from "@nestjs/testing";
import { io } from "socket.io-client";
import { Prisma } from "../dist/generated/prisma/client.js";
import { seatTemplate, seatLayout } from "../dist/seats/layout.js";
import {
  BookingService,
  completeCouples,
} from "../dist/booking/booking.service.js";
import { BookingController } from "../dist/booking/booking.controller.js";
import { BookingGateway } from "../dist/booking/booking.gateway.js";
import { AuthService } from "../dist/auth/auth.service.js";
import {
  JwtGuard,
  RoleGuard,
  VerifiedEmailGuard,
} from "../dist/auth/guards.js";
import { PrismaService } from "../dist/prisma.service.js";

test("all seven layouts preserve capacities, pair boundaries, geometry and Gold independence", () => {
  for (const [type, capacity] of [
    ["STANDARD", 160],
    ["STANDARD", 145],
    ["STANDARD", 130],
    ["STANDARD", 112],
    ["IMAX", 496],
    ["SCREENX", 180],
    ["GOLD", 32],
  ]) {
    const seats = seatTemplate(type, capacity).map((s, i) => ({
      ...s,
      id: String(i),
    }));
    assert.equal(seats.length, capacity);
    assert.equal(new Set(seats.map((s) => s.seatCode)).size, capacity);
    assert.ok(seats.every((s) => s.rowName !== "I"));
    assert.ok(completeCouples(seats));
    const layout = seatLayout(type, capacity, seats);
    assert.ok(layout.key.endsWith("v1"));
    assert.equal(layout.seats.length, capacity);
    for (const p of layout.seats)
      assert.ok(
        p.x >= 0 &&
          p.y >= 0 &&
          p.x + 0.9 <= layout.width &&
          p.y + 0.9 <= layout.height,
      );
    const couples = seats.filter((s) => s.seatType === "COUPLE");
    if (couples.length) {
      assert.equal(completeCouples([couples[0]]), false);
      assert.equal(completeCouples(couples.slice(0, 2)), true);
    }
    if (type === "GOLD")
      assert.ok(
        seats.every((s) => s.seatType === "GOLD") &&
          layout.seats.every((p) => p.pairKey === null),
      );
    if (type === "SCREENX")
      assert.deepEqual(layout.screens, ["FRONT", "LEFT", "RIGHT"]);
    if (type === "IMAX") assert.ok(seats.every((s) => s.seatType === "VIP"));
  }
  const legacy = [
    {
      id: "legacy",
      seatCode: "I1",
      rowName: "I",
      seatNumber: 1,
      seatType: "NORMAL",
    },
  ];
  assert.equal(seatLayout("IMAX", 496, legacy).key, "IMAX-496-legacy");
  assert.equal(seatLayout("IMAX", 496, legacy).seats.length, 1);
  assert.equal(seatLayout("IMAX", 496, []).seats.length, 0);
});

function fixture() {
  const userId = randomUUID(),
    otherId = randomUUID(),
    showtimeId = randomUUID();
  const users = new Map(
    [userId, otherId].map((id) => [
      id,
      {
        id,
        role: "CUSTOMER",
        isActive: true,
        emailVerifiedAt: new Date(),
        creditBalance: new Prisma.Decimal(1000),
      },
    ]),
  );
  const slot = {
    id: showtimeId,
    status: "ACTIVE",
    startAt: new Date(Date.now() + 3600000),
    auditorium: { status: "ACTIVE" },
  };
  let seats = seatTemplate("GOLD", 32)
    .slice(0, 4)
    .map((s, i) => ({
      ...s,
      id: randomUUID(),
      showtimeId,
      seatType: i < 2 ? "COUPLE" : "NORMAL",
      price: new Prisma.Decimal(100000),
      status: "AVAILABLE",
      heldByUserId: null,
      holdExpiresAt: null,
    }));
  let orders = new Map();
  let phase = "IDLE";
  let committed = 0;
  const events = [];
  let tail = Promise.resolve();
  const withSeats = (o) => o && { ...o, seats: o.seats.map((s) => ({ ...s })) };
  const matches = (o, w = {}) =>
    (!w.id || o.id === w.id) &&
    (!w.showtimeId || o.showtimeId === w.showtimeId) &&
    (!w.status || o.status === w.status) &&
    (!w.expiresAt || o.expiresAt <= w.expiresAt.lte);
  const db = {
    $queryRaw: async (q, ...values) => {
      const text = Array.isArray(q) ? q.join("?") : q.sql;
      if (text.includes("clock_timestamp")) return [{ now: new Date() }];
      if (text.includes("FROM showtimes"))
        return values[0] === showtimeId ? [{ id: showtimeId }] : [];
      return [];
    },
    user: {
      findUnique: async (q) => users.get(q.where.id),
      update: async (q) => {
        const u = users.get(q.where.id);
        u.creditBalance = u.creditBalance.plus(q.data.creditBalance.increment);
        return u;
      },
    },
    showtime: {
      findUnique: async (q) => (q.where.id === showtimeId ? slot : null),
    },
    showtimeSeat: {
      findMany: async (q) =>
        seats
          .filter(
            (s) =>
              q.where.id.in.includes(s.id) &&
              (!q.where.showtimeId || s.showtimeId === q.where.showtimeId),
          )
          .map((s) => ({ ...s })),
      updateMany: async (q) => {
        let count = 0;
        for (const s of seats)
          if (
            q.where.id.in.includes(s.id) &&
            s.status === q.where.status &&
            (!q.where.heldByUserId ||
              s.heldByUserId === q.where.heldByUserId) &&
            (!q.where.holdExpiresAt ||
              s.holdExpiresAt?.getTime() === q.where.holdExpiresAt.getTime())
          ) {
            Object.assign(s, q.data);
            count++;
          }
        return { count };
      },
    },
    order: {
      findUnique: async (q) => withSeats(orders.get(q.where.id)),
      findMany: async (q) =>
        [...orders.values()].filter((o) => matches(o, q.where)).map(withSeats),
      create: async (q) => {
        const d = q.data;
        if (orders.has(d.id)) throw Error("duplicate");
        const o = {
          ...d,
          status: "PENDING_PAYMENT",
          subtotal: new Prisma.Decimal(d.subtotal),
          discount: new Prisma.Decimal(d.discount),
          creditUsed: new Prisma.Decimal(d.creditUsed),
          totalAmount: new Prisma.Decimal(d.totalAmount),
          seats: d.seats.create.map((s) => ({
            ...s,
            orderId: d.id,
            status: "HELD",
          })),
        };
        orders.set(o.id, o);
        return withSeats(o);
      },
      update: async (q) => {
        const o = orders.get(q.where.id);
        Object.assign(o, q.data);
        return withSeats(o);
      },
    },
    orderSeat: {
      updateMany: async (q) => {
        const o = orders.get(q.where.orderId);
        for (const s of o.seats)
          if (s.status === q.where.status) Object.assign(s, q.data);
        return { count: o.seats.length };
      },
    },
    payment: { updateMany: async () => ({ count: 0 }) },
    $transaction: async (fn) => {
      let unlock;
      const previous = tail;
      tail = new Promise((r) => (unlock = r));
      await previous;
      const savedSeats = seats.map((s) => ({ ...s })),
        savedOrders = new Map([...orders].map(([id, o]) => [id, withSeats(o)]));
      phase = "TRANSACTION";
      try {
        const result = await fn(db);
        phase = "COMMITTED";
        committed++;
        return result;
      } catch (e) {
        seats = savedSeats;
        orders = savedOrders;
        phase = "ROLLED_BACK";
        throw e;
      } finally {
        unlock();
      }
    },
  };
  const gateway = {
    publish: (incoming) => {
      assert.ok(committed > 0);
      events.push(...incoming);
    },
  };
  return {
    db,
    service: new BookingService(db, gateway),
    userId,
    otherId,
    showtimeId,
    users,
    slot,
    events,
    get seats() {
      return seats;
    },
    get orders() {
      return orders;
    },
  };
}
const body = (f, ids) => ({
  showtime_id: f.showtimeId,
  seat_ids: ids ?? f.seats.slice(0, 2).map((s) => s.id),
});

test("hold is atomic, uses DB prices, lasts exactly 600 seconds and publishes after commit", async () => {
  const f = fixture();
  const key = randomUUID();
  const before = Date.now();
  const result = await f.service.create(f.userId, key, body(f));
  assert.equal(result.item.status, "PENDING_PAYMENT");
  assert.equal(result.item.totalAmount, "200000");
  assert.equal(result.item.creditUsed, "0");
  assert.equal(f.users.get(f.userId).creditBalance.toString(), "1000");
  assert.ok(new Date(result.item.expiresAt).getTime() >= before + 600000);
  assert.ok(new Date(result.item.expiresAt).getTime() <= Date.now() + 600000);
  assert.ok(f.seats.slice(0, 2).every((s) => s.status === "HELD"));
  assert.equal(f.events.length, 1);
  const retry = await f.service.create(f.userId, key, body(f));
  assert.deepEqual(retry.item, result.item);
  assert.equal(f.orders.size, 1);
  assert.equal(f.events.length, 1);
  await assert.rejects(
    f.service.create(f.userId, key, body(f, [f.seats[2].id])),
    (e) => e.getResponse().code === "IDEMPOTENCY_CONFLICT",
  );
  assert.equal(f.events.length, 1);
});
test("Couple incomplete and mixed-seat conflicts never partially hold or emit", async () => {
  const f = fixture();
  await assert.rejects(
    f.service.create(f.userId, randomUUID(), body(f, [f.seats[0].id])),
    (e) => e.getResponse().code === "COUPLE_INCOMPLETE",
  );
  assert.equal(f.orders.size, 0);
  assert.equal(f.events.length, 0);
  f.seats[2].status = "SOLD";
  await assert.rejects(
    f.service.create(
      f.userId,
      randomUUID(),
      body(f, [f.seats[2].id, f.seats[3].id]),
    ),
    (e) => e.getResponse().code === "SEAT_UNAVAILABLE",
  );
  assert.equal(f.seats[3].status, "AVAILABLE");
  assert.equal(f.events.length, 0);
});
test("competing requests have one winner; ownership and inactive/unverified customers are enforced", async () => {
  const f = fixture();
  const result = await Promise.allSettled([
    f.service.create(f.userId, randomUUID(), body(f)),
    f.service.create(f.otherId, randomUUID(), body(f)),
  ]);
  assert.equal(result.filter((r) => r.status === "fulfilled").length, 1);
  assert.equal(f.orders.size, 1);
  const [id] = f.orders.keys();
  await assert.rejects(
    f.service.get(id, f.otherId),
    (e) => e.getStatus() === 404,
  );
  const g = fixture();
  g.users.get(g.userId).emailVerifiedAt = null;
  await assert.rejects(
    g.service.create(g.userId, randomUUID(), body(g)),
    (e) => e.getStatus() === 403,
  );
  assert.equal(g.events.length, 0);
  g.users.get(g.userId).emailVerifiedAt = new Date();
  g.slot.status = "CANCELED";
  await assert.rejects(
    g.service.create(g.userId, randomUUID(), body(g)),
    (e) => e.getResponse().code === "SHOWTIME_UNAVAILABLE",
  );
});
test("cancel and expiry release once, retain history and never free another hold or PAID seats", async () => {
  const f = fixture();
  const key = randomUUID();
  await f.service.create(f.userId, key, body(f));
  await f.service.cancel(key, f.userId);
  const count = f.events.length;
  await f.service.cancel(key, f.userId);
  assert.equal(f.events.length, count);
  assert.ok(f.seats.slice(0, 2).every((s) => s.status === "AVAILABLE"));
  assert.ok(f.orders.get(key).seats.every((s) => s.status === "CANCELED"));
  const next = randomUUID();
  await f.service.create(f.otherId, next, body(f));
  await f.service.cancel(key, f.userId);
  assert.ok(f.seats.slice(0, 2).every((s) => s.heldByUserId === f.otherId));
  const order = f.orders.get(next);
  order.expiresAt = new Date(Date.now() - 1);
  f.seats.slice(0, 2).forEach((s) => (s.holdExpiresAt = order.expiresAt));
  await f.service.expire();
  assert.equal(order.status, "EXPIRED");
  const count2 = f.events.length;
  await f.service.expire();
  assert.equal(f.events.length, count2);
  const paid = randomUUID();
  await f.service.create(f.userId, paid, body(f));
  f.orders.get(paid).status = "PAID";
  await f.service.expire();
  assert.ok(f.seats.slice(0, 2).every((s) => s.status === "HELD"));
  await assert.rejects(
    f.service.cancel(paid, f.userId),
    (e) => e.getResponse().code === "ORDER_NOT_CANCELABLE",
  );
});
test("lazy expiry before a new hold preserves refunded credit exactly once", async () => {
  const f = fixture();
  const key = randomUUID();
  await f.service.create(f.userId, key, body(f));
  const o = f.orders.get(key);
  o.creditUsed = new Prisma.Decimal(100);
  o.expiresAt = new Date(Date.now() - 1);
  f.seats.slice(0, 2).forEach((s) => (s.holdExpiresAt = o.expiresAt));
  await f.service.create(f.otherId, randomUUID(), body(f));
  assert.equal(f.users.get(f.userId).creditBalance.toString(), "1100");
  await f.service.expire();
  assert.equal(f.users.get(f.userId).creditBalance.toString(), "1100");
  assert.equal(o.status, "EXPIRED");
});

test("real HTTP booking validates JWT/UUID/DTO/key, refuses status/price injection and exposes only your order", async (t) => {
  const f = fixture();
  const module = await Test.createTestingModule({
    controllers: [BookingController],
    providers: [
      { provide: BookingService, useValue: f.service },
      {
        provide: AuthService,
        useValue: {
          authenticate: async (token) => {
            if (token !== "customer") throw Error("bad token");
            return f.users.get(f.userId);
          },
        },
      },
      JwtGuard,
      RoleGuard,
      VerifiedEmailGuard,
    ],
  }).compile();
  const app = module.createNestApplication({ logger: false });
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );
  await app.listen(0, "127.0.0.1");
  t.after(() => app.close());
  const url = await app.getUrl();
  const post = (data, key = randomUUID(), auth = "Bearer customer") =>
    fetch(url + "/orders", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: auth,
        "Idempotency-Key": key,
      },
      body: JSON.stringify(data),
    });
  assert.equal((await post(body(f), "bad-key")).status, 400);
  assert.equal(
    (await post({ ...body(f), status: "PAID", total_amount: 1 })).status,
    400,
  );
  assert.equal((await post(body(f), randomUUID(), "")).status, 401);
  assert.equal(
    (await post({ ...body(f), seat_ids: ["not-uuid"] })).status,
    400,
  );
  const r = await post(body(f));
  assert.equal(r.status, 201);
  const o = await r.json();
  assert.ok(o.item.expiresAt);
  assert.equal(o.item.userId, undefined);
  const c = await fetch(url + `/orders/${o.item.id}/cancel`, {
    method: "POST",
    headers: { Authorization: "Bearer customer" },
  });
  assert.equal(c.status, 201);
  assert.equal((await c.json()).item.status, "CANCELED");
});

test("real Socket.IO handshake rejects invalid/staff tokens, isolates user rooms and allows only showtime UUID joins", async (t) => {
  const showtimeId = randomUUID();
  const user1 = randomUUID(),
    user2 = randomUUID();
  const token = (id) =>
    `x.${Buffer.from(JSON.stringify({ sub: id, exp: Math.floor(Date.now() / 1000) + 60 })).toString("base64url")}.x`;
  const aToken = token(user1),
    bToken = token(user2),
    staffToken = token("staff");
  const tokens = new Map([
    [aToken, { id: user1, role: "CUSTOMER" }],
    [bToken, { id: user2, role: "CUSTOMER" }],
    [staffToken, { id: "staff", role: "STAFF" }],
  ]);
  const module = await Test.createTestingModule({
    providers: [
      BookingGateway,
      {
        provide: AuthService,
        useValue: {
          authenticate: async (value) => {
            const u = tokens.get(value);
            if (!u) throw Error("invalid");
            return u;
          },
        },
      },
      {
        provide: PrismaService,
        useValue: {
          showtime: {
            findUnique: async (q) =>
              q.where.id === showtimeId ? { id: showtimeId } : null,
          },
        },
      },
    ],
  }).compile();
  const app = module.createNestApplication({ logger: false });
  await app.listen(0, "127.0.0.1");
  t.after(() => app.close());
  const url = await app.getUrl();
  const connect = (auth) =>
    new Promise((resolve, reject) => {
      const s = io(url, {
        auth: { token: auth },
        forceNew: true,
        reconnection: false,
        timeout: 1500,
      });
      t.after(() => s.close());
      s.once("connect", () => resolve(s));
      s.once("connect_error", reject);
    });
  await assert.rejects(connect("invalid"));
  await assert.rejects(connect(staffToken));
  const a = await connect(aToken),
    b = await connect(bToken);
  const ack = (s, id) =>
    new Promise((resolve) => s.emit("JOIN_SHOWTIME", id, resolve));
  assert.equal((await ack(a, `user:${user2}`)).ok, false);
  assert.equal((await ack(a, showtimeId)).ok, true);
  assert.equal((await ack(b, showtimeId)).ok, true);
  let privateLeaks = 0;
  b.on("ORDER_EXPIRED", () => privateLeaks++);
  const received = new Promise((resolve) => a.once("ORDER_EXPIRED", resolve));
  module.get(BookingGateway).publish([
    {
      room: `user:${user1}`,
      name: "ORDER_EXPIRED",
      data: { orderId: "owned" },
    },
  ]);
  assert.deepEqual(await received, { orderId: "owned" });
  await new Promise((r) => setTimeout(r, 30));
  assert.equal(privateLeaks, 0);
});
