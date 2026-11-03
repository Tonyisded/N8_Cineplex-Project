import test from "node:test";
import assert from "node:assert/strict";
import { randomBytes } from "node:crypto";
import { ConfigService } from "@nestjs/config";
import { PaymentsService } from "../dist/payments/payments.service.js";
import {
  initialRefundRetry,
  refundRetryDecision,
  refundRetryAt,
  readRefundRetry,
} from "../dist/payments/refund-retry.js";
import { sealRecipient } from "../dist/payments/payment-domain.js";
const recipient = { bin: "970436", account: "123456789", name: "FIXTURE" };
const id = "11111111-1111-4111-8111-111111111111";
function payout(state = "SUCCEEDED", extra = {}) {
  return {
    id: "payout1",
    referenceId: "REF-fixture",
    transactions: [
      {
        referenceId: "REF-fixture",
        amount: 100000,
        toBin: recipient.bin,
        toAccountNumber: recipient.account,
        state,
        errorCode: null,
        reference: null,
        transactionDatetime: null,
        ...extra,
      },
    ],
  };
}
function fixture(t, create) {
  t.mock.timers.enable({ apis: ["Date"], now: Date.UTC(2030, 0, 1) });
  const key = randomBytes(32).toString("hex");
  const row = {
    id,
    orderId: "order",
    kind: "REFUND",
    status: "PENDING",
    operationState: "REQUESTED",
    snapshot: { refundRetry: initialRefundRetry(id) },
    notificationState: { requested: { sentAt: "keep" } },
    expectedAmount: { toString: () => "100000" },
    recipientCipher: sealRecipient(recipient, key),
    paymentCode: "REF-fixture",
    providerObjectId: null,
    attempts: 0,
    leaseUntil: null,
    nextAttemptAt: null,
  };
  const order = { id: "order", status: "REFUND_PENDING" };
  const ticket = { status: "REFUND_PENDING" };
  const stats = { queries: 0, creates: [], results: [] };
  const update = ({ data }) => {
    for (const [k, v] of Object.entries(data))
      row[k] = k === "attempts" ? row.attempts + v.increment : v;
    return { ...row };
  };
  const db = {
    $queryRaw: async () => [{ now: new Date() }],
    payment: {
      findUnique: async () => ({ ...row }),
      findUniqueOrThrow: async () => ({ ...row }),
      update: async (x) => update(x),
      updateMany: async (x) => {
        if (
          !["REQUESTED", "PROCESSING", "UNKNOWN"].includes(
            row.operationState,
          ) ||
          (row.nextAttemptAt && row.nextAttemptAt > new Date()) ||
          (row.leaseUntil && row.leaseUntil > new Date())
        )
          return { count: 0 };
        update(x);
        return { count: 1 };
      },
    },
    order: { update: async ({ data }) => Object.assign(order, data) },
    ticket: { update: async ({ data }) => Object.assign(ticket, data) },
  };
  db.$transaction = async (f) => f(db);
  const config = new ConfigService({
    REFUND_PAYOUT_ENABLED: true,
    REFUND_RETRY_ENABLED: true,
    PAYMENT_DATA_ENCRYPTION_KEY: key,
  });
  const provider = {
    findPayout: async () => {
      stats.queries++;
      return structuredClone(stats.results);
    },
    createPayout: async (body, key) => {
      stats.creates.push({ body: { ...body }, key, time: Date.now() });
      return create ? create(stats, body, key) : payout();
    },
  };
  const service = () => {
    const s = new PaymentsService(db, config, provider, {});
    s.lockOrder = async () => order;
    return s;
  };
  return {
    row,
    order,
    ticket,
    stats,
    provider,
    config,
    service,
    advance: () => t.mock.timers.tick(180000),
  };
}
test("retry finalizes once, stops after success, leaves notifications and same business reference intact", async (t) => {
  const f = fixture(t);
  const s = f.service();
  await Promise.all([s.processRefund(id), s.processRefund(id)]);
  assert.equal(f.stats.creates.length, 1);
  assert.equal(f.row.operationState, "SUCCEEDED");
  assert.equal(f.order.status, "REFUNDED");
  assert.equal(f.ticket.status, "REFUNDED");
  assert.equal(f.row.snapshot.refundRetry.cycles, 1);
  f.advance();
  await f.service().processRefund(id);
  assert.equal(f.stats.creates.length, 1);
  assert.deepEqual(f.row.notificationState, { requested: { sentAt: "keep" } });
});
test("four durable unfunded attempts, >=180 seconds apart; restart does not reset budget; fifth call cannot dispatch", async (t) => {
  const f = fixture(t, (stats) => {
    const p = payout("FAILED", { errorCode: "PAYOUT_INSUFFICIENT_FUNDS" });
    p.id = "failed" + stats.creates.length;
    stats.results.push(p);
    return p;
  });
  for (let i = 0; i < 4; i++) {
    await f.service().processRefund(id);
    assert.equal(f.stats.creates.length, i + 1);
    if (i < 3) {
      assert.equal(f.row.status, "PENDING");
      t.mock.timers.tick(179999);
      await f.service().processRefund(id);
      assert.equal(f.stats.creates.length, i + 1);
      t.mock.timers.tick(1);
    }
  }
  assert.equal(f.row.status, "NEEDS_ADMIN");
  assert.equal(f.row.operationState, "NEEDS_REVIEW");
  assert.equal(f.row.snapshot.refundRetry.cycles, 4);
  assert.equal(new Set(f.stats.creates.map((x) => x.key)).size, 4);
  assert.ok(
    f.stats.creates.every(
      (x) =>
        x.body.referenceId === "REF-fixture" &&
        x.body.amount === 100000 &&
        x.body.toAccountNumber === recipient.account,
    ),
  );
  f.advance();
  await f.service().processRefund(id);
  assert.equal(f.stats.creates.length, 4);
});
test("timeout then empty query only replays the exact persisted key; never invents a second key", async (t) => {
  const f = fixture(t, () => {
    throw new Error("network uncertain");
  });
  await f.service().processRefund(id);
  f.advance();
  await f.service().processRefund(id);
  assert.equal(f.stats.creates.length, 2);
  assert.equal(f.stats.creates[0].key, f.stats.creates[1].key);
  assert.deepEqual(f.stats.creates[0].body, f.stats.creates[1].body);
  assert.equal(f.row.snapshot.refundRetry.dispatches, 1);
  assert.equal(f.row.status, "PENDING");
});
test("timeout after provider success is recovered by authoritative query, without another payout", async (t) => {
  const f = fixture(t, (stats) => {
    stats.results.push(payout());
    throw new Error("response lost");
  });
  await f.service().processRefund(id);
  assert.equal(f.row.status, "PENDING");
  f.advance();
  await f.service().processRefund(id);
  assert.equal(f.row.operationState, "SUCCEEDED");
  assert.equal(f.stats.creates.length, 1);
});
test("bank failure with a reference never gets a new payout; four reconciliation cycles then review", async (t) => {
  const f = fixture(t, (stats) => {
    const p = payout("FAILED", {
      errorCode: "PAYOUT_BANK_SYSTEM_ERROR",
      reference: "bank-reference",
      transactionDatetime: "2030-01-01T00:00:00Z",
    });
    stats.results.push(p);
    return p;
  });
  for (let i = 0; i < 4; i++) {
    await f.service().processRefund(id);
    if (i < 3) {
      assert.equal(f.row.status, "PENDING");
      f.advance();
    }
  }
  assert.equal(f.row.status, "NEEDS_ADMIN");
  assert.equal(f.stats.creates.length, 1);
  assert.equal(f.stats.queries, 4);
});
test("429 honors Retry-After, keeps frontend processing, and reaches review without payout on fourth failed query", async (t) => {
  const f = fixture(t);
  f.provider.findPayout = async () => {
    f.stats.queries++;
    throw { headers: new Headers({ "retry-after": "600" }) };
  };
  for (let i = 0; i < 4; i++) {
    await f.service().processRefund(id);
    if (i < 3) {
      assert.equal(f.row.status, "PENDING");
      f.advance();
      await f.service().processRefund(id);
      assert.equal(f.stats.queries, i + 1);
      t.mock.timers.tick(420000);
    }
  }
  assert.equal(f.row.status, "NEEDS_ADMIN");
  assert.equal(f.stats.creates.length, 0);
  assert.equal(f.stats.queries, 4);
});
test("invalid signed payout or duplicate successes requires immediate review, never credits a ticket", async (t) => {
  const f = fixture(t);
  f.stats.results = [payout("SUCCEEDED", { toAccountNumber: "9999999" })];
  await f.service().processRefund(id);
  assert.equal(f.row.status, "NEEDS_ADMIN");
  assert.equal(f.ticket.status, "REFUND_PENDING");
  assert.equal(f.stats.creates.length, 0);
  const s = { ...initialRefundRetry(id), cycles: 1 };
  assert.equal(
    refundRetryDecision(
      [payout(), { ...payout(), id: "other" }],
      s,
      "REF-fixture",
      "100000",
      recipient,
    ).action,
    "REVIEW",
  );
});
test("pending bank processing is queried only, never duplicated; kill switch blocks dispatch", async (t) => {
  const f = fixture(t);
  f.stats.results = [payout("PROCESSING")];
  await f.service().processRefund(id);
  assert.equal(f.stats.creates.length, 0);
  assert.equal(f.row.status, "PENDING");
  f.advance();
  f.stats.results = [];
  f.config.set("REFUND_PAYOUT_ENABLED", false);
  await f.service().processRefund(id);
  assert.equal(f.stats.creates.length, 0);
});
test("retry state validates bounds and server scheduling never retries earlier than180s", () => {
  assert.equal(refundRetryAt(new Date(0)).getTime(), 180000);
  assert.equal(refundRetryAt(new Date(0), "1").getTime(), 180000);
  assert.equal(refundRetryAt(new Date(0), "600").getTime(), 600000);
  assert.equal(refundRetryAt(new Date(0), "invalid").getTime(), 180000);
  for (const cycles of [-1, 5, 1.5])
    assert.throws(() => readRefundRetry({ ...initialRefundRetry(id), cycles }));
});

test("180 second delay starts after response/error completion, not request start", async (t) => {
  const f = fixture(t, () => {
    t.mock.timers.tick(15000);
    throw new Error("slow timeout");
  });
  await f.service().processRefund(id);
  assert.equal(f.row.nextAttemptAt.getTime() - Date.now(), 180000);
  t.mock.timers.tick(179999);
  await f.service().processRefund(id);
  assert.equal(f.stats.creates.length, 1);
});
