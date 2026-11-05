import { BookingService } from "../../dist/booking/booking.service.js";
import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID, randomBytes } from "node:crypto";
import { ConfigService } from "@nestjs/config";
import { PrismaService } from "../../dist/prisma.service.js";
import { PayosAdapter } from "../../dist/payments/payos.adapter.js";
import { PaymentsService } from "../../dist/payments/payments.service.js";
import { FcmController } from "../../dist/notifications/fcm.controller.js";
const url = process.env.DATABASE_URL;
if (!url || !url.includes("w9_test"))
  throw new Error("Only disposable w9_test database allowed");
const config = new ConfigService({
  DATABASE_URL: url,
  JWT_ACCESS_SECRET: randomBytes(32).toString("hex"),
  PAYMENT_LIVE_ENABLED: true,
  REFUND_PAYOUT_ENABLED: true,
  TICKET_QR_SECRET: randomBytes(32).toString("hex"),
  PAYMENT_DATA_ENCRYPTION_KEY: randomBytes(32).toString("hex"),
  PAYOS_COLLECT_ACCOUNT_NUMBER: "123456789",
  PUBLIC_BASE_URL: "https://fixture.invalid/api/v1",
});
const db = new PrismaService(config);
const events = [];
let providerCalls = 0,
  payoutCalls = 0;
const provider = {
  createLink: async (d) => {
    providerCalls++;
    return {
      ...d,
      paymentLinkId: "link-" + d.orderCode,
      currency: "VND",
      accountNumber: "123456789",
      accountName: "FIXTURE",
      bin: "970422",
      checkoutUrl: "https://pay.payos.vn/web/fixture",
      qrCode: "fixture-qr",
    };
  },
  getLink: async (code) => ({
    id: "link-" + code,
    orderCode: code,
    amount: 100000,
    transactions: [],
  }),
  cancelLink: async () => ({}),
  createPayout: async (d, key, beforeSend) => {
    await beforeSend?.();
    payoutCalls++;
    return {
      id: "payout-" + key,
      referenceId: d.referenceId,
      transactions: [
        {
          referenceId: d.referenceId,
          amount: d.amount,
          toBin: d.toBin,
          toAccountNumber: d.toAccountNumber,
          state: "SUCCEEDED",
        },
      ],
    };
  },
  findPayout: async () => [],
  getPayout: async () => {
    throw new Error("unexpected");
  },
};
const service = new PaymentsService(db, config, provider, {
  publish: (e) => events.push(...e),
});
async function fixture(amount = "100000") {
  const uid = randomUUID(),
    mid = randomUUID(),
    aid = randomUUID(),
    sid = randomUUID(),
    seat = randomUUID(),
    oid = randomUUID(),
    now = new Date();
  await db.user.create({
    data: {
      id: uid,
      email: uid + "@fixture.invalid",
      fullName: "Offline fixture",
      emailVerifiedAt: now,
    },
  });
  await db.movie.create({
    data: {
      id: mid,
      title: "Fixture movie",
      sourceUrl: "https://fixture.invalid/" + mid,
      durationMinutes: 90,
      ageRating: "P",
    },
  });
  await db.auditorium.create({
    data: { id: aid, name: "Fixture", type: "STANDARD", capacity: 1 },
  });
  await db.showtime.create({
    data: {
      id: sid,
      movieId: mid,
      auditoriumId: aid,
      startAt: new Date(+now + 2 * 3600000),
      endAt: new Date(+now + 3 * 3600000),
      format: "STANDARD_2D",
      basePrice: amount,
      sourceKey: sid,
    },
  });
  const expiry = new Date(+now + 600000);
  await db.showtimeSeat.create({
    data: {
      id: seat,
      showtimeId: sid,
      seatCode: "A1",
      rowName: "A",
      seatNumber: 1,
      seatType: "NORMAL",
      price: amount,
      status: "HELD",
      heldByUserId: uid,
      holdExpiresAt: expiry,
    },
  });
  await db.order.create({
    data: {
      id: oid,
      userId: uid,
      showtimeId: sid,
      subtotal: amount,
      totalAmount: amount,
      expiresAt: expiry,
      checkoutConfirmedAt: now,
      seats: {
        create: { showtimeSeatId: seat, seatCode: "A1", price: amount },
      },
    },
  });
  return { uid, oid, sid, seat };
}
async function receipt(f, reference, amount = 100000) {
  const p = await db.payment.findUniqueOrThrow({
    where: { intentOrderId: f.oid },
  });
  await service.receive(
    p,
    {
      reference,
      amount,
      accountNumber: "123456789",
      description: p.paymentCode,
      counterAccountBankId: "970422",
      counterAccountNumber: "987654321",
      counterAccountName: "PAYER FIXTURE",
    },
    p.providerObjectId,
  );
}
test("PostgreSQL transaction and concurrency integration, isolated fake financial transport", async (t) => {
  await db.$connect();
  try {
    await t.test(
      "schema removed Credit, keeps business tables plus governor and inbox",
      async () => {
        const cols =
          await db.$queryRaw`SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND column_name IN ('credit_balance','credit_used')`;
        assert.equal(cols.length, 0);
        const tables =
          await db.$queryRaw`SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename<>'_prisma_migrations'`;
        assert.equal(tables.length, 12);
        assert.ok(tables.some((t) => t.tablename === "provider_api_governor"));
        assert.ok(tables.some((t) => t.tablename === "provider_webhook_inbox"));
      },
    );
    await t.test(
      "runtime role cannot bypass sequence ACL and can recover pre-intent failure",
      async () => {
        const [role] =
          await db.$queryRaw`SELECT rolsuper, has_sequence_privilege(current_user, 'payos_order_code_seq', 'USAGE') AS usage FROM pg_roles WHERE rolname=current_user`;
        assert.equal(role.rolsuper, false);
        assert.equal(role.usage, true);
        const f = await fixture();
        const create = provider.createLink;
        provider.createLink = async (d) => ({
          ...(await create(d)),
          accountNumber: "VQRORDER1",
        });
        try {
          const untouched = await service.check(f.oid, f.uid);
          assert.equal(
            await db.payment.count({ where: { intentOrderId: f.oid } }),
            0,
          );
          const result = await service.start(f.oid, f.uid);
          assert.equal(result.item.qrCode, "fixture-qr");
          assert.equal(result.item.accountNumber, "VQRORDER1");
          assert.equal(
            result.item.expiresAt,
            (
              await db.order.findUnique({ where: { id: f.oid } })
            ).expiresAt.toISOString(),
          );
          const intent = await db.payment.findUniqueOrThrow({
            where: { intentOrderId: f.oid },
          });
          await service.receive(
            intent,
            {
              reference: randomUUID(),
              amount: 100000,
              accountNumber: "VQRORDER1",
              description: intent.paymentCode,
              counterAccountBankId: "970422",
              counterAccountNumber: "987654321",
              counterAccountName: "PAYER FIXTURE",
            },
            intent.providerObjectId,
          );
          const ticket = await db.ticket.findUniqueOrThrow({
            where: { orderId: f.oid },
          });
          const refund = await service.refund(ticket.id, f.uid);
          await service.processRefund(refund.item.id);
          assert.equal(
            (await db.payment.findUnique({ where: { id: refund.item.id } }))
              .operationState,
            "SUCCEEDED",
          );
          const controller = new FcmController(db);
          assert.deepEqual(
            await controller.put(
              { user: { id: f.uid } },
              { token: "isolated-fixture-token" },
            ),
            { registered: true },
          );
          assert.deepEqual(await controller.remove({ user: { id: f.uid } }), {
            registered: false,
          });
        } finally {
          provider.createLink = create;
        }
      },
    );
    await t.test(
      "expired empty reservations are deleted, provider and financial records survive",
      async () => {
        const empty = await fixture();
        await db.orderSeat.updateMany({
          where: { orderId: empty.oid },
          data: { status: "CANCELED" },
        });
        await db.order.update({
          where: { id: empty.oid },
          data: { status: "EXPIRED", expiresAt: new Date(0) },
        });
        const linked = await fixture();
        await service.start(linked.oid, linked.uid);
        await db.orderSeat.updateMany({
          where: { orderId: linked.oid },
          data: { status: "CANCELED" },
        });
        await db.order.update({
          where: { id: linked.oid },
          data: { status: "EXPIRED", expiresAt: new Date(0) },
        });
        const count = await service.pruneExpiredOrders();
        assert.ok(count >= 1);
        assert.equal(
          await db.order.findUnique({ where: { id: empty.oid } }),
          null,
        );
        assert.equal(
          await db.orderSeat.count({ where: { orderId: empty.oid } }),
          0,
        );
        assert.ok(await db.order.findUnique({ where: { id: linked.oid } }));
        assert.equal(
          await db.payment.count({ where: { orderId: linked.oid } }),
          1,
        );
        assert.equal((await service.orders(linked.uid)).items.length, 0);
        assert.equal(await service.pruneExpiredOrders(), 0);
      },
    );
    await t.test(
      "zero price finalizes once without provider collection",
      async () => {
        const f = await fixture("0"),
          before = providerCalls;
        const rows = await Promise.all([
          service.start(f.oid, f.uid),
          service.start(f.oid, f.uid),
        ]);
        assert.equal(providerCalls, before);
        assert.equal(await db.ticket.count({ where: { orderId: f.oid } }), 1);
        assert.equal(
          await db.payment.count({ where: { orderId: f.oid, kind: "INTENT" } }),
          0,
        );
        assert.equal(rows[0].item.ticketId, rows[1].item.ticketId);
      },
    );
    await t.test(
      "parallel starts and receipts finalize exactly once, refund/resale retains history",
      async () => {
        const f = await fixture(),
          before = providerCalls;
        await Promise.all([
          service.start(f.oid, f.uid),
          service.start(f.oid, f.uid),
        ]);
        assert.equal(providerCalls, before + 1);
        await Promise.all([receipt(f, randomUUID()), receipt(f, randomUUID())]);
        assert.equal(await db.ticket.count({ where: { orderId: f.oid } }), 1);
        assert.equal(
          (await db.order.findUnique({ where: { id: f.oid } })).status,
          "PAID",
        );
        const replay = await service.start(f.oid, f.uid);
        assert.equal(replay.item.amount, "100000");
        const ticket = await db.ticket.findUniqueOrThrow({
          where: { orderId: f.oid },
        });
        const responses = await Promise.all([
          service.refund(ticket.id, f.uid),
          service.refund(ticket.id, f.uid),
        ]);
        assert.equal(responses[0].item.id, responses[1].item.id);
        const p = await db.payment.findUniqueOrThrow({
          where: { refundOrderId: f.oid },
        });
        const beforePayout = payoutCalls;
        await Promise.all([
          service.processRefund(p.id),
          service.processRefund(p.id),
        ]);
        assert.equal(payoutCalls, beforePayout + 1);
        assert.equal(
          (await db.ticket.findUnique({ where: { id: ticket.id } })).status,
          "REFUNDED",
        );
        assert.equal(
          (await db.showtimeSeat.findUnique({ where: { id: f.seat } })).status,
          "AVAILABLE",
        );
        assert.equal(
          await db.orderSeat.count({
            where: { orderId: f.oid, status: "REFUNDED" },
          }),
          1,
        );
        const replacement = randomUUID();
        await db.order.create({
          data: {
            id: replacement,
            userId: f.uid,
            showtimeId: f.sid,
            subtotal: "100000",
            totalAmount: "100000",
            expiresAt: new Date(Date.now() + 600000),
            seats: {
              create: {
                showtimeSeatId: f.seat,
                seatCode: "A1",
                price: "100000",
              },
            },
          },
        });
        assert.equal(
          await db.orderSeat.count({ where: { showtimeSeatId: f.seat } }),
          2,
        );
        await assert.rejects(service.qr(ticket.id, f.uid));
      },
    );
    await t.test(
      "wrong amount followed by exact amount stays issue, no ticket",
      async () => {
        const f = await fixture();
        await service.start(f.oid, f.uid);
        await receipt(f, randomUUID(), 99999);
        await receipt(f, randomUUID(), 100000);
        assert.equal(await db.ticket.count({ where: { orderId: f.oid } }), 0);
        assert.equal(
          (await db.payment.findUnique({ where: { intentOrderId: f.oid } }))
            .status,
          "NEEDS_ADMIN",
        );
      },
    );
    await t.test(
      "same bank reference duplicated concurrently records one receipt and ticket",
      async () => {
        const f = await fixture();
        await service.start(f.oid, f.uid);
        const ref = randomUUID();
        await Promise.all([receipt(f, ref), receipt(f, ref)]);
        assert.equal(
          await db.payment.count({
            where: { providerTransactionId: "payos:" + ref },
          }),
          1,
        );
        assert.equal(await db.ticket.count({ where: { orderId: f.oid } }), 1);
      },
    );
    await t.test(
      "expired hold receives money as issue without revive",
      async () => {
        const f = await fixture();
        await service.start(f.oid, f.uid);
        await db.order.update({
          where: { id: f.oid },
          data: { expiresAt: new Date(Date.now() - 1) },
        });
        await receipt(f, randomUUID());
        assert.equal(await db.ticket.count({ where: { orderId: f.oid } }), 0);
        assert.equal(
          (await db.payment.findUnique({ where: { intentOrderId: f.oid } }))
            .status,
          "NEEDS_ADMIN",
        );
      },
    );
    await t.test(
      "used ticket and foreign owner cannot refund/read QR",
      async () => {
        const f = await fixture("0");
        const p = await service.start(f.oid, f.uid);
        await db.ticket.update({
          where: { id: p.item.ticketId },
          data: { status: "USED", checkedInAt: new Date() },
        });
        await assert.rejects(service.refund(p.item.ticketId, f.uid));
        await assert.rejects(service.ticket(p.item.ticketId, randomUUID()));
        await assert.rejects(service.qr(p.item.ticketId, f.uid));
      },
    );
    await t.test(
      "missing payer metadata holds refund for review with no payout",
      async () => {
        const f = await fixture();
        await service.start(f.oid, f.uid);
        const intent = await db.payment.findUniqueOrThrow({
          where: { intentOrderId: f.oid },
        });
        await service.receive(
          intent,
          {
            reference: randomUUID(),
            amount: 100000,
            accountNumber: "123456789",
            description: "fixture",
          },
          intent.providerObjectId,
        );
        const ticket = await db.ticket.findUniqueOrThrow({
          where: { orderId: f.oid },
        });
        const before = payoutCalls;
        const r = await service.refund(ticket.id, f.uid);
        await service.processRefund(r.item.id);
        assert.equal(r.item.operationState, "NEEDS_REVIEW");
        assert.equal(payoutCalls, before);
        assert.equal(
          (await db.ticket.findUnique({ where: { id: ticket.id } })).status,
          "REFUND_PENDING",
        );
      },
    );
    await t.test(
      "payout accepted then timeout recovers by same reference without second transfer",
      async () => {
        const f = await fixture();
        await service.start(f.oid, f.uid);
        await receipt(f, randomUUID());
        const ticket = await db.ticket.findUniqueOrThrow({
          where: { orderId: f.oid },
        });
        const r = await service.refund(ticket.id, f.uid);
        const create = provider.createPayout,
          find = provider.findPayout;
        let accepted,
          calls = 0;
        provider.createPayout = async (d, k, beforeSend) => {
          await beforeSend?.();
          calls++;
          accepted = await create(d, k);
          throw new Error("response lost");
        };
        provider.findPayout = async (reference) =>
          accepted && accepted.referenceId === reference ? [accepted] : [];
        try {
          await service.processRefund(r.item.id);
          assert.equal(
            (await db.payment.findUnique({ where: { id: r.item.id } }))
              .operationState,
            "UNKNOWN",
          );
          // Only accelerate due times inside this disposable fixture, never production.
          await db.payment.update({
            where: { id: r.item.id },
            data: { nextAttemptAt: new Date(0) },
          });
          await service.processRefund(r.item.id);
          assert.equal(calls, 1);
          assert.equal(
            (await db.payment.findUnique({ where: { id: r.item.id } }))
              .operationState,
            "SUCCEEDED",
          );
          assert.equal(
            (await db.ticket.findUnique({ where: { id: ticket.id } })).status,
            "REFUNDED",
          );
        } finally {
          provider.createPayout = create;
          provider.findPayout = find;
        }
      },
    );
    await t.test(
      "collection create accepted then timeout restores existing hosted checkout without new create",
      async () => {
        const f = await fixture();
        const create = provider.createLink,
          get = provider.getLink;
        let accepted,
          calls = 0;
        provider.createLink = async (d) => {
          calls++;
          accepted = await create(d);
          throw new Error("response lost");
        };
        provider.getLink = async (code) => ({
          id: accepted.paymentLinkId,
          status: "PENDING",
          orderCode: code,
          amount: 100000,
          transactions: [],
        });
        try {
          const a = await service.start(f.oid, f.uid);
          assert.equal(a.item.operationState, "UNKNOWN");
          await db.payment.updateMany({
            where: { intentOrderId: f.oid },
            data: { nextAttemptAt: new Date(0) },
          });
          const b = await service.check(f.oid, f.uid);
          assert.equal(b.item.operationState, "PROCESSING");
          assert.equal(b.item.paymentLinkId, accepted.paymentLinkId);
          assert.match(b.item.checkoutUrl, /https:\/\/pay.payos.vn\/web\//);
          assert.equal(calls, 1);
        } finally {
          provider.createLink = create;
          provider.getLink = get;
        }
      },
    );
    await t.test(
      "already expired provider link stops retries without recanceling or losing review",
      async () => {
        const f = await fixture();
        await service.start(f.oid, f.uid);
        const p = await db.payment.findUniqueOrThrow({
          where: { intentOrderId: f.oid },
        });
        const get = provider.getLink,
          cancel = provider.cancelLink;
        provider.getLink = async (code) => ({
          id: p.providerObjectId,
          orderCode: code,
          status: "EXPIRED",
          amount: 100000,
          transactions: [],
        });
        provider.cancelLink = async () => {
          throw new Error("must not cancel an expired link");
        };
        try {
          await db.payment.update({
            where: { id: p.id },
            data: {
              expiresAt: new Date(0),
              nextAttemptAt: new Date(0),
              status: "EXPIRED",
              operationState: "UNKNOWN",
            },
          });
          assert.equal(
            (await service.check(f.oid, f.uid)).item.operationState,
            "EXPIRED",
          );
          await db.payment.update({
            where: { id: p.id },
            data: { status: "NEEDS_ADMIN", operationState: "NEEDS_REVIEW" },
          });
          const review = await service.check(f.oid, f.uid);
          assert.equal(review.item.status, "NEEDS_ADMIN");
          assert.equal(review.item.operationState, "NEEDS_REVIEW");
        } finally {
          provider.getLink = get;
          provider.cancelLink = cancel;
        }
      },
    );
    await t.test(
      "fault inside finalization rolls back money receipt, seats, order and ticket",
      async () => {
        const f = await fixture();
        await service.start(f.oid, f.uid);
        const intent = await db.payment.findUniqueOrThrow({
          where: { intentOrderId: f.oid },
        });
        const dbProxy = new Proxy(db, {
          get(target, key) {
            if (key === "$transaction")
              return (callback, options) =>
                target.$transaction(
                  (tx) =>
                    callback(
                      new Proxy(tx, {
                        get(t, k) {
                          if (k === "ticket")
                            return {
                              ...t.ticket,
                              create: async () => {
                                throw new Error("injected ticket failure");
                              },
                            };
                          return t[k];
                        },
                      }),
                    ),
                  options,
                );
            return target[key];
          },
        });
        const broken = new PaymentsService(dbProxy, config, provider, {
          publish: () => {
            throw new Error("no publish before commit");
          },
        });
        const ref = randomUUID();
        await assert.rejects(
          broken.receive(
            intent,
            {
              reference: ref,
              amount: 100000,
              accountNumber: "123456789",
              description: "fixture",
            },
            intent.providerObjectId,
          ),
        );
        assert.equal(
          await db.payment.count({
            where: { providerTransactionId: "payos:" + ref },
          }),
          0,
        );
        assert.equal(await db.ticket.count({ where: { orderId: f.oid } }), 0);
        assert.equal(
          (await db.order.findUnique({ where: { id: f.oid } })).status,
          "PENDING_PAYMENT",
        );
        assert.equal(
          (await db.showtimeSeat.findUnique({ where: { id: f.seat } })).status,
          "HELD",
        );
        await receipt(f, ref);
        assert.equal(await db.ticket.count({ where: { orderId: f.oid } }), 1);
      },
    );
    await t.test(
      "failed payout remains refund debt and ticket cannot revive",
      async () => {
        const f = await fixture();
        await service.start(f.oid, f.uid);
        await receipt(f, randomUUID());
        const ticket = await db.ticket.findUniqueOrThrow({
          where: { orderId: f.oid },
        });
        const r = await service.refund(ticket.id, f.uid);
        const create = provider.createPayout;
        provider.createPayout = async (d, k, beforeSend) => {
          await beforeSend?.();
          const result = await create(d, k);
          result.transactions[0].state = "FAILED";
          return result;
        };
        try {
          await service.processRefund(r.item.id);
          const p = await db.payment.findUnique({ where: { id: r.item.id } });
          assert.equal(p.status, "NEEDS_ADMIN");
          assert.equal(p.operationState, "NEEDS_REVIEW");
          assert.equal(
            (await db.order.findUnique({ where: { id: f.oid } })).status,
            "REFUND_PENDING",
          );
          assert.equal(
            (await db.ticket.findUnique({ where: { id: ticket.id } })).status,
            "REFUND_PENDING",
          );
          await assert.rejects(
            service.resolve(
              p.id,
              randomUUID(),
              "MARK_BANK_REFUNDED",
              "manual ref fixture",
            ),
          );
        } finally {
          provider.createPayout = create;
        }
      },
    );
    await t.test(
      "intent cannot be manually bank-refunded as second issue",
      async () => {
        const f = await fixture();
        await service.start(f.oid, f.uid);
        await receipt(f, randomUUID(), 99999);
        const intent = await db.payment.findUniqueOrThrow({
          where: { intentOrderId: f.oid },
        });
        await assert.rejects(
          service.resolve(
            intent.id,
            randomUUID(),
            "MARK_BANK_REFUNDED",
            "manual reference fixture",
          ),
        );
        const issues = await service.issues();
        assert.ok(issues.items.every((p) => p.kind !== "INTENT"));
      },
    );
    await t.test(
      "fixed-proxy retry marker: PostgreSQL concurrent workers create/finalize exactly once",
      async () => {
        const create = provider.createPayout,
          find = provider.findPayout;
        config.set("REFUND_RETRY_ENABLED", true);
        const results = [];
        let calls = 0;
        provider.findPayout = async () => results;
        provider.createPayout = async (d, key, beforeSend) => {
          await beforeSend?.();
          calls++;
          const p = await create(d, key);
          results.push(p);
          return p;
        };
        try {
          const f = await fixture();
          await service.start(f.oid, f.uid);
          await receipt(f, randomUUID());
          const ticket = await db.ticket.findUniqueOrThrow({
            where: { orderId: f.oid },
          });
          const r = await service.refund(ticket.id, f.uid);
          await Promise.all(
            Array.from({ length: 6 }, () => service.processRefund(r.item.id)),
          );
          const p = await db.payment.findUniqueOrThrow({
            where: { id: r.item.id },
          });
          assert.equal(calls, 1);
          assert.equal(p.operationState, "SUCCEEDED");
          assert.equal(p.snapshot.refundRetry.cycles, 1);
          assert.equal(
            (await db.ticket.findUniqueOrThrow({ where: { id: ticket.id } }))
              .status,
            "REFUNDED",
          );
          assert.ok(
            !JSON.stringify(
              (await service.refundState(ticket.id, f.uid)).item,
            ).includes("refundRetry"),
          );
        } finally {
          config.set("REFUND_RETRY_ENABLED", false);
          provider.createPayout = create;
          provider.findPayout = find;
        }
      },
    );
    await t.test(
      "durable retry budget and delay in real PostgreSQL; fixture-only due acceleration",
      async () => {
        const create = provider.createPayout,
          find = provider.findPayout;
        config.set("REFUND_RETRY_ENABLED", true);
        const results = [];
        let calls = 0;
        const keys = [];
        provider.findPayout = async () => results;
        provider.createPayout = async (d, key, beforeSend) => {
          await beforeSend?.();
          calls++;
          keys.push(key);
          const p = {
            id: "retry-fixture-" + key,
            referenceId: d.referenceId,
            transactions: [
              {
                referenceId: d.referenceId,
                amount: d.amount,
                toBin: d.toBin,
                toAccountNumber: d.toAccountNumber,
                state: "FAILED",
                errorCode: "PAYOUT_INSUFFICIENT_FUNDS",
                reference: null,
                transactionDatetime: null,
              },
            ],
          };
          results.push(p);
          return p;
        };
        try {
          const f = await fixture();
          await service.start(f.oid, f.uid);
          await receipt(f, randomUUID());
          const ticket = await db.ticket.findUniqueOrThrow({
            where: { orderId: f.oid },
          });
          const r = await service.refund(ticket.id, f.uid);
          for (let i = 1; i <= 4; i++) {
            await Promise.all([
              service.processRefund(r.item.id),
              service.processRefund(r.item.id),
            ]);
            const p = await db.payment.findUniqueOrThrow({
              where: { id: r.item.id },
            });
            assert.equal(calls, i);
            assert.equal(p.snapshot.refundRetry.cycles, i);
            assert.equal(p.status, "NEEDS_ADMIN");
            assert.equal(p.snapshot.refundRetry.phase, "WAITING_FUNDS");
            assert.equal(p.snapshot.refundRetry.sendAttempts, i);
            assert.equal(p.nextAttemptAt, null);
            await service.processRefund(r.item.id);
            assert.equal(calls, i);
            if (i < 4)
              await service.resumeFunds(
                r.item.id,
                randomUUID(),
                randomUUID(),
                "Fixture funds added",
              );
          }
          await service.processRefund(r.item.id);
          assert.equal(calls, 4);
          assert.equal(new Set(keys).size, 4);
          assert.equal(
            (await db.ticket.findUniqueOrThrow({ where: { id: ticket.id } }))
              .status,
            "REFUND_PENDING",
          );
          assert.equal(
            await db.payment.count({ where: { refundOrderId: f.oid } }),
            1,
          );
        } finally {
          config.set("REFUND_RETRY_ENABLED", false);
          provider.createPayout = create;
          provider.findPayout = find;
        }
      },
    );
    await t.test(
      "timeout after successful provider dispatch recovers after process restart with no second debit",
      async () => {
        const create = provider.createPayout,
          find = provider.findPayout;
        config.set("REFUND_RETRY_ENABLED", true);
        const results = [];
        let calls = 0;
        provider.findPayout = async () => results;
        provider.createPayout = async (d, key, beforeSend) => {
          await beforeSend?.();
          calls++;
          results.push(await create(d, key));
          throw new Error("lost response fixture");
        };
        try {
          const f = await fixture();
          await service.start(f.oid, f.uid);
          await receipt(f, randomUUID());
          const ticket = await db.ticket.findUniqueOrThrow({
            where: { orderId: f.oid },
          });
          const r = await service.refund(ticket.id, f.uid);
          await service.processRefund(r.item.id);
          assert.equal(
            (await db.payment.findUniqueOrThrow({ where: { id: r.item.id } }))
              .status,
            "PENDING",
          );
          await db.payment.update({
            where: { id: r.item.id },
            data: { nextAttemptAt: new Date(0) },
          });
          await new PaymentsService(db, config, provider, {
            publish: () => {},
          }).processRefund(r.item.id);
          assert.equal(calls, 1);
          assert.equal(
            (await db.payment.findUniqueOrThrow({ where: { id: r.item.id } }))
              .operationState,
            "SUCCEEDED",
          );
        } finally {
          config.set("REFUND_RETRY_ENABLED", false);
          provider.createPayout = create;
          provider.findPayout = find;
        }
      },
    );
    await t.test(
      "after four sends, real PostgreSQL review row remains queryable and late success finalizes without fifth send",
      async () => {
        const create = provider.createPayout,
          find = provider.findPayout;
        config.set("REFUND_RETRY_ENABLED", true);
        let visible = false,
          calls = 0,
          result;
        const keys = [];
        provider.findPayout = async () => (visible ? [result] : []);
        provider.createPayout = async (d, key, beforeSend) => {
          await beforeSend?.();
          calls++;
          keys.push(key);
          result = await create(d, key);
          throw Object.assign(new Error("fixture response lost"), {
            status: 429,
            code: "429",
          });
        };
        try {
          const f = await fixture();
          await service.start(f.oid, f.uid);
          await receipt(f, randomUUID());
          const ticket = await db.ticket.findUniqueOrThrow({
            where: { orderId: f.oid },
          });
          const r = await service.refund(ticket.id, f.uid);
          await service.processRefund(r.item.id);
          const historical = await db.payment.findUniqueOrThrow({
            where: { id: r.item.id },
          });
          // Seed durable metadata from an earlier release; v3 intentionally never performs four ambiguous POSTs.
          await db.payment.update({
            where: { id: r.item.id },
            data: {
              status: "NEEDS_ADMIN",
              operationState: "NEEDS_REVIEW",
              nextAttemptAt: new Date(0),
              snapshot: {
                ...historical.snapshot,
                refundRetry: {
                  ...historical.snapshot.refundRetry,
                  version: 2,
                  sendAttempts: 4,
                  reconcileOnly: true,
                },
              },
            },
          });
          const p = await db.payment.findUniqueOrThrow({
            where: { id: r.item.id },
          });
          assert.equal(p.operationState, "NEEDS_REVIEW");
          assert.equal(p.snapshot.refundRetry.sendAttempts, 4);
          assert.equal(p.snapshot.refundRetry.reconcileOnly, true);
          assert.equal(p.snapshot.refundRetry.lastError.httpStatus, 429);
          assert.equal(new Set(keys).size, 1);
          const due = await db.payment.findMany({
            where: {
              id: p.id,
              operationState: "NEEDS_REVIEW",
              snapshot: {
                path: ["refundRetry", "reconcileOnly"],
                equals: true,
              },
            },
          });
          assert.equal(due.length, 1);
          visible = true;
          await new PaymentsService(db, config, provider, {
            publish: () => {},
          }).processRefund(p.id);
          assert.equal(calls, 1);
          assert.equal(
            (await db.payment.findUniqueOrThrow({ where: { id: p.id } }))
              .operationState,
            "SUCCEEDED",
          );
          assert.equal(
            (await db.ticket.findUniqueOrThrow({ where: { id: ticket.id } }))
              .status,
            "REFUNDED",
          );
        } finally {
          provider.createPayout = create;
          provider.findPayout = find;
          config.set("REFUND_RETRY_ENABLED", false);
        }
      },
    );

    await t.test(
      "multi-cart12+2 checkout is idempotent and payment/refund retain historical combo lines",
      async () => {
        const f = await fixture("150000"),
          a = randomUUID(),
          b = randomUUID();
        await db.combo.create({
          data: {
            id: a,
            name: "Fixture combo A",
            description: "isolated",
            price: 100000,
          },
        });
        await db.combo.create({
          data: {
            id: b,
            name: "Fixture drink B",
            description: "isolated",
            price: 30000,
          },
        });
        const booking = new BookingService(db, { publish: () => {} }, config);
        const q = await booking.quote(f.oid, f.uid, {
          combo_quantity: 0,
          combo_items: [
            { id: a, quantity: 12 },
            { id: b, quantity: 2 },
          ],
          voucher_code: "FIRST50",
        });
        assert.equal(q.item.subtotal, "1410000");
        assert.equal(q.item.bankAmount, "705000");
        const first = await booking.checkout(f.oid, f.uid, q.item.quoteToken);
        const again = await booking.checkout(f.oid, f.uid, q.item.quoteToken);
        assert.deepEqual(first.item.comboItems, again.item.comboItems);
        assert.equal(first.item.comboItems.length, 2);
        assert.equal(
          (await db.order.findUniqueOrThrow({ where: { id: f.oid } }))
            .checkoutRevision,
          1,
        );
        await db.combo.update({
          where: { id: a },
          data: { price: 999999, status: "UNAVAILABLE" },
        });
        await service.start(f.oid, f.uid);
        await receipt(f, randomUUID(), 705000);
        const ticket = await db.ticket.findUniqueOrThrow({
          where: { orderId: f.oid },
        });
        assert.equal(
          ticket.snapshot.comboItems.find((x) => x.id === a).unitPrice,
          "100000",
        );
        assert.equal(
          ticket.snapshot.comboItems.find((x) => x.id === a).quantity,
          12,
        );
        const refund = await service.refund(ticket.id, f.uid);
        await service.processRefund(refund.item.id);
        const restored = await db.ticket.findUniqueOrThrow({
          where: { id: ticket.id },
        });
        assert.equal(restored.status, "REFUNDED");
        assert.deepEqual(
          restored.snapshot.comboItems,
          ticket.snapshot.comboItems,
        );
        assert.equal(
          (
            await db.payment.findUniqueOrThrow({
              where: { id: refund.item.id },
            })
          ).expectedAmount.toString(),
          "705000",
        );
      },
    );
    await t.test(
      "real refund journal preserves unsigned-success HTTP evidence and never finalizes money or ticket",
      async () => {
        const f = await fixture("179000");
        await service.start(f.oid, f.uid);
        await receipt(f, randomUUID(), 179000);
        const ticket = await db.ticket.findUniqueOrThrow({
          where: { orderId: f.oid },
        });
        config.set("REFUND_RETRY_ENABLED", true);
        const marker = "SECRET_RECIPIENT_TOKEN_MARKER";
        for (const name of ["CLIENT_ID", "API_KEY", "CHECKSUM_KEY"])
          config.set("PAYOS_PAYOUT_" + name, randomBytes(32).toString("hex"));
        const adapter = new PayosAdapter(config, {
          execute: async (_channel, _operation, _interval, call) => call(),
        });
        let calls = 0;
        adapter.payoutTransport = () => ({
          fetch: async () => {
            calls++;
            return new Response(
              JSON.stringify({
                code: "00",
                desc: marker,
                data: { id: "untrusted", transactions: [] },
              }),
              {
                status: 200,
                headers: {
                  "content-type": "application/json",
                  "x-secret": marker,
                },
              },
            );
          },
        });
        try {
          const refund = await service.refund(ticket.id, f.uid);
          const before = await db.payment.findUniqueOrThrow({
            where: { id: refund.item.id },
          });
          const worker = new PaymentsService(db, config, adapter, {
            publish: () => {},
          });
          await worker.processRefund(refund.item.id);
          const payment = await db.payment.findUniqueOrThrow({
            where: { id: refund.item.id },
          });
          const event = payment.snapshot.refundRetry.lastError;
          assert.equal(event.kind, "InvalidSignatureError");
          assert.equal(event.httpStatus, 200);
          assert.equal(event.signaturePresent, false);
          assert.ok(
            !JSON.stringify(payment.snapshot.refundRetry).includes(marker),
          );
          assert.equal(payment.snapshot.refundRetry.sendAttempts, 1);
          assert.equal(payment.paidAt, null);
          assert.equal(payment.operationState, "PROCESSING");
          assert.equal(calls, 1);
          assert.deepEqual(payment.notificationState, before.notificationState);
          assert.equal(
            (await db.ticket.findUniqueOrThrow({ where: { id: ticket.id } }))
              .status,
            "REFUND_PENDING",
          );
          assert.equal(
            (await db.order.findUniqueOrThrow({ where: { id: f.oid } })).status,
            "REFUND_PENDING",
          );
        } finally {
          config.set("REFUND_RETRY_ENABLED", false);
          await adapter.onModuleDestroy();
        }
      },
    );
  } finally {
    await db.$disconnect();
  }
});
