import test from "node:test";
import assert from "node:assert/strict";
import { ConfigService } from "@nestjs/config";
import { PaymentsService } from "../dist/payments/payments.service.js";
import { initialRefundRetry } from "../dist/payments/refund-retry.js";
const id = "11111111-1111-4111-8111-111111111111";
function fixture(overrides = {}) {
  const row = {
    id,
    kind: "REFUND",
    status: "NEEDS_ADMIN",
    operationState: "NEEDS_REVIEW",
    expectedAmount: { toString: () => "179000" },
    paidAt: null,
    leaseUntil: null,
    providerObjectId: "signed-failed-fixture",
    notificationState: { requested: { sentAt: "keep" } },
    snapshot: {
      refundRetry: {
        ...initialRefundRetry(id),
        phase: "WAITING_FUNDS",
        sendAttempts: 1,
        dispatched: true,
        dispatches: 1,
        requestCipher: "encrypted-immutable-body",
        requestHash: "immutable-hash",
        ...overrides,
      },
    },
  };
  const db = {
    $queryRaw: async () => [{ now: new Date() }],
    payment: {
      findUniqueOrThrow: async () => ({ ...row }),
      update: async ({ data }) => Object.assign(row, data),
    },
  };
  db.$transaction = async (f) => f(db);
  return {
    row,
    service: new PaymentsService(
      db,
      new ConfigService({}),
      new Proxy(
        {},
        {
          get: () => {
            throw Error("No provider calls allowed during resume");
          },
        },
      ),
      {},
    ),
  };
}
test("confirmed funded resume persists one valid UUIDv4 idempotency key, not a suffixed key", async () => {
  const { row, service } = fixture();
  await service.resumeFunds(id, "actor", "approval", "Funds confirmed fixture");
  const key = row.snapshot.refundRetry.key;
  assert.match(
    key,
    /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
  );
  assert.equal(key.length, 36);
  assert.notEqual(key, id);
  assert.equal(row.snapshot.refundRetry.sendAttempts, 1);
  assert.equal(
    row.snapshot.refundRetry.requestCipher,
    "encrypted-immutable-body",
  );
  assert.equal(row.snapshot.refundRetry.requestHash, "immutable-hash");
  assert.deepEqual(row.notificationState, { requested: { sentAt: "keep" } });
  await service.resumeFunds(id, "actor", "approval", "Funds confirmed fixture");
  assert.equal(row.snapshot.refundRetry.key, key);
  await assert.rejects(
    service.resumeFunds(
      id,
      "actor",
      "approval",
      "Different input must not alter key",
    ),
  );
  assert.equal(row.snapshot.refundRetry.key, key);
  await assert.rejects(
    service.resumeFunds(
      id,
      "actor",
      "second-approval",
      "Do not requeue an active request",
    ),
  );
  assert.equal(row.snapshot.refundRetry.key, key);
});
test("ambiguous result cannot mint a new key", async () => {
  const { row, service } = fixture({
    phase: "AWAITING_RESULT",
    reconcileOnly: true,
  });
  await assert.rejects(
    service.resumeFunds(
      id,
      "actor",
      "approval",
      "Cannot change uncertain payout",
    ),
  );
  assert.equal(row.snapshot.refundRetry.key, id);
  assert.equal(row.snapshot.refundRetry.sendAttempts, 1);
});
test("standard four-send budget is not reset or extended by key-format fix", async () => {
  const { row, service } = fixture({ sendAttempts: 4, dispatches: 4 });
  await assert.rejects(
    service.resumeFunds(id, "actor", "approval", "No fifth automatic send"),
  );
  assert.equal(row.snapshot.refundRetry.key, id);
  assert.equal(row.snapshot.refundRetry.sendAttempts, 4);
});
