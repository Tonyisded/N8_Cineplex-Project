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
  safeRefundError,
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
          !(
            ["REQUESTED", "PROCESSING", "UNKNOWN"].includes(
              row.operationState,
            ) ||
            (row.operationState === "NEEDS_REVIEW" &&
              row.snapshot.refundRetry.reconcileOnly)
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
    createPayout: async (body, key, beforeSend) => {
      await beforeSend?.();
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
    advance: () =>
      t.mock.timers.tick(
        Math.max(0, (row.nextAttemptAt?.getTime() ?? Date.now()) - Date.now()),
      ),
  };
}

test("v3 fresh success uses one POST, zero queries, finalizes once and preserves notices", async (t) => {
  const f = fixture(t);
  await f.service().processRefund(id);
  assert.equal(f.stats.creates.length, 1);
  assert.equal(f.stats.queries, 0);
  assert.equal(f.row.operationState, "SUCCEEDED");
  assert.equal(f.row.snapshot.refundRetry.sendAttempts, 1);
  assert.equal(f.ticket.status, "REFUNDED");
  assert.deepEqual(f.row.notificationState, { requested: { sentAt: "keep" } });
  assert.ok(
    !f.row.snapshot.refundRetry.requestCipher.includes(recipient.account),
  );
  f.advance();
  await f.service().processRefund(id);
  assert.equal(f.stats.creates.length, 1);
});
test("ambiguous POST429 never replays on empty query, retains key, budget and journal", async (t) => {
  const f = fixture(t, () => {
    throw Object.assign(new Error("SECRET"), { status: 429 });
  });
  await f.service().processRefund(id);
  const key = f.row.snapshot.refundRetry.key;
  for (let i = 0; i < 8; i++) {
    f.advance();
    await f.service().processRefund(id);
  }
  assert.equal(f.stats.creates.length, 1);
  assert.equal(f.row.snapshot.refundRetry.sendAttempts, 1);
  assert.equal(f.stats.queries, 8);
  assert.equal(f.row.snapshot.refundRetry.key, key);
  assert.ok(!JSON.stringify(f.row.snapshot).includes("SECRET"));
});
test("late success after response loss finalizes through authoritative list without another POST", async (t) => {
  const f = fixture(t, (stats) => {
    stats.results.push(payout());
    throw new Error("response lost");
  });
  await f.service().processRefund(id);
  f.advance();
  await f.service().processRefund(id);
  assert.equal(f.row.operationState, "SUCCEEDED");
  assert.equal(f.stats.creates.length, 1);
});
test("confirmed insufficient funds stops and waits for admin without balance polling", async (t) => {
  const f = fixture(t, () =>
    payout("FAILED", { errorCode: "PAYOUT_INSUFFICIENT_FUNDS" }),
  );
  await f.service().processRefund(id);
  assert.equal(f.row.snapshot.refundRetry.phase, "WAITING_FUNDS");
  assert.equal(f.row.operationState, "NEEDS_REVIEW");
  assert.equal(f.row.nextAttemptAt, null);
  for (let i = 0; i < 8; i++) await f.service().processRefund(id);
  assert.equal(f.stats.creates.length, 1);
  assert.equal(f.stats.queries, 0);
});
test("known pending ID is queried by GET only, no LIST and no duplicate POST", async (t) => {
  const f = fixture(t, () => payout("PROCESSING"));
  let gets = 0;
  f.provider.getPayout = async () => {
    gets++;
    return payout("PROCESSING");
  };
  await f.service().processRefund(id);
  for (let i = 0; i < 3; i++) {
    f.advance();
    await f.service().processRefund(id);
  }
  assert.equal(gets, 3);
  assert.equal(f.stats.queries, 0);
  assert.equal(f.stats.creates.length, 1);
});
test("query429 honors Retry-After600 without spending send budget", async (t) => {
  const f = fixture(t);
  f.row.snapshot.refundRetry.phase = "AWAITING_RESULT";
  f.provider.findPayout = async () => {
    f.stats.queries++;
    throw { status: 429, headers: new Headers({ "retry-after": "600" }) };
  };
  await f.service().processRefund(id);
  assert.equal(f.row.nextAttemptAt.getTime() - Date.now(), 600000);
  t.mock.timers.tick(599999);
  await f.service().processRefund(id);
  assert.equal(f.stats.queries, 1);
  assert.equal(f.row.snapshot.refundRetry.sendAttempts, 0);
});
test("wrong recipient or multiple successes require review and cannot restore a ticket", async (t) => {
  const f = fixture(t);
  f.row.snapshot.refundRetry.phase = "AWAITING_RESULT";
  f.stats.results = [payout("SUCCEEDED", { toAccountNumber: "wrong" })];
  await f.service().processRefund(id);
  assert.equal(f.row.status, "NEEDS_ADMIN");
  assert.equal(f.ticket.status, "REFUND_PENDING");
  assert.equal(f.row.nextAttemptAt, null);
  assert.equal(f.stats.creates.length, 0);
  assert.equal(
    refundRetryDecision(
      [payout(), { ...payout(), id: "other" }],
      initialRefundRetry(id),
      "REF-fixture",
      "100000",
      recipient,
    ).action,
    "REVIEW",
  );
});
test("kill switch prevents first dispatch", async (t) => {
  const f = fixture(t);
  f.config.set("REFUND_PAYOUT_ENABLED", false);
  await f.service().processRefund(id);
  assert.equal(f.stats.creates.length, 0);
  assert.equal(f.row.snapshot.refundRetry.sendAttempts, 0);
});
test("polling progresses3/10/30/60min measured after completion and stops below due", async (t) => {
  const f = fixture(t, () => {
    throw new Error("lost");
  });
  for (const seconds of [180, 600, 1800, 3600]) {
    await f.service().processRefund(id);
    const wait = f.row.nextAttemptAt.getTime() - Date.now();
    assert.ok(
      wait >= seconds * 1000 &&
        wait <= Math.min(3600000, seconds * 1000 + 10000),
    );
    const calls = f.stats.queries;
    t.mock.timers.tick(wait - 1);
    await f.service().processRefund(id);
    assert.equal(f.stats.queries, calls);
    t.mock.timers.tick(1);
  }
  assert.equal(f.stats.creates.length, 1);
});
test("historical four-send row stays queryable, never issues fifth POST, accepts late success", async (t) => {
  const f = fixture(t);
  Object.assign(f.row.snapshot.refundRetry, {
    version: 2,
    phase: undefined,
    sendAttempts: 4,
    dispatches: 1,
    dispatched: true,
    reconcileOnly: true,
    cycles: 4,
  });
  f.row.status = "NEEDS_ADMIN";
  f.row.operationState = "NEEDS_REVIEW";
  await f.service().processRefund(id);
  assert.equal(f.stats.creates.length, 0);
  f.stats.results = [payout()];
  f.advance();
  await f.service().processRefund(id);
  assert.equal(f.row.operationState, "SUCCEEDED");
  assert.equal(f.stats.creates.length, 0);
});
test("read failures leave send budget intact and bounded journal excludes raw messages and headers", async (t) => {
  const f = fixture(t);
  f.row.snapshot.refundRetry.phase = "AWAITING_RESULT";
  f.provider.findPayout = async () => {
    throw {
      status: 503,
      message: "SECRET_PAYLOAD",
      headers: new Headers({ authorization: "TOKEN_SECRET" }),
    };
  };
  for (let i = 0; i < 10; i++) {
    await f.service().processRefund(id);
    f.advance();
  }
  assert.equal(f.row.snapshot.refundRetry.sendAttempts, 0);
  assert.equal(f.row.snapshot.refundRetry.errors.length, 8);
  assert.equal(f.row.snapshot.refundRetry.lastError.httpStatus, 503);
  assert.ok(!JSON.stringify(f.row.snapshot).includes("SECRET"));
});
test("retry parser bounds remain strict", () => {
  assert.throws(() =>
    readRefundRetry({ ...initialRefundRetry(id), cycles: -1 }),
  );
  assert.throws(() =>
    readRefundRetry({ ...initialRefundRetry(id), sendAttempts: 5 }),
  );
  assert.equal(refundRetryAt(new Date(0), "600").getTime(), 600000);
});
test("persisted v1 metadata migrates conservatively, safe error serializer only includes allowlisted fields", () => {
  const state = readRefundRetry({
    version: 1,
    cycles: 4,
    key: id,
    baseIds: [],
    dispatched: true,
    dispatches: 1,
  });
  assert.equal(state.sendAttempts, 4);
  assert.throws(() =>
    readRefundRetry({ ...initialRefundRetry(id), sendAttempts: 5 }),
  );
  const e = safeRefundError(
    {
      status: 429,
      code: "606",
      message: "SECRET",
      headers: { secret: "TOKEN" },
    },
    "send",
    new Date(0),
    50,
    7,
    4,
  );
  assert.equal(e.httpStatus, 429);
  assert.equal(e.providerCode, "606");
  assert.ok(!JSON.stringify(e).includes("SECRET"));
  assert.equal(e.elapsedMs, 50);
});
