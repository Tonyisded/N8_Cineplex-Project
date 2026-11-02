import test from "node:test";
import assert from "node:assert/strict";
import { randomBytes } from "node:crypto";
import {
  canRefund,
  collectionProblem,
  payer,
  sealRecipient,
  openRecipient,
  ticketQrPayload,
  tokenHash,
} from "../dist/payments/payment-domain.js";
test("ticket QR is stable, opaque, purpose-bound, non-demo and requires server secret", () => {
  const secret = randomBytes(32).toString("hex");
  const qr = ticketQrPayload("ticket-id", secret);
  assert.equal(qr, ticketQrPayload("ticket-id", secret));
  assert.notEqual(qr, ticketQrPayload("other-id", secret));
  assert.match(qr, /^N8T1:/);
  assert.doesNotMatch(qr, /email|DEMO|bank/);
  assert.equal(tokenHash(qr).length, 64);
  assert.throws(() => ticketQrPayload("id", ""));
});
test("payer must be fully identified; encrypted destination rejects alteration", () => {
  assert.equal(payer({ counterAccountNumber: "123456" }), null);
  const r = payer({
    counterAccountBankId: "970422",
    counterAccountNumber: "123456789",
    counterAccountName: "TEST USER",
  });
  assert.ok(r);
  const key = randomBytes(32).toString("hex"),
    encrypted = sealRecipient(r, key);
  assert.deepEqual(openRecipient(encrypted, key), r);
  assert.ok(!encrypted.includes(r.account));
  assert.throws(() =>
    openRecipient(encrypted, randomBytes(32).toString("hex")),
  );
});
test("refund 45 minute boundary is inclusive and used/redeemed/pending are rejected", () => {
  const now = new Date("2026-11-02T00:00:00Z"),
    t = { status: "VALID", checkedInAt: null, comboRedeemedAt: null };
  const start = new Date(+now + 45 * 60000);
  assert.equal(canRefund("PAID", t, start, now), true);
  assert.equal(canRefund("PAID", t, new Date(+start - 1), now), false);
  assert.equal(
    canRefund("PAID", { ...t, checkedInAt: now }, start, now),
    false,
  );
  assert.equal(
    canRefund("PAID", { ...t, comboRedeemedAt: now }, start, now),
    false,
  );
  assert.equal(canRefund("REFUND_PENDING", t, start, now), false);
});
test("exact amount only; wrong/late never silently finalized", () => {
  const now = new Date();
  const future = new Date(+now + 1);
  assert.equal(collectionProblem("100001", 100001, true, future, now), null);
  for (const amount of [100000, 100002, -1, NaN, Number.MAX_SAFE_INTEGER + 1])
    assert.equal(
      collectionProblem("100001", amount, true, future, now),
      "WRONG_AMOUNT",
    );
  assert.equal(
    collectionProblem("100001", 100001, true, now, now),
    "LATE_PAYMENT",
  );
  assert.equal(
    collectionProblem("100001", 100001, false, future, now),
    "LATE_PAYMENT",
  );
});
import { ConfigService } from "@nestjs/config";
import { PayOS } from "@payos/node";
import { PayosAdapter } from "../dist/payments/payos.adapter.js";
import { PaymentsService } from "../dist/payments/payments.service.js";
test("SDK verified webhook rejects bad signature/currency/reference; probe issues no ticket", async () => {
  const keys = {
    PAYOS_COLLECT_CLIENT_ID: randomBytes(16).toString("hex"),
    PAYOS_COLLECT_API_KEY: randomBytes(32).toString("hex"),
    PAYOS_COLLECT_CHECKSUM_KEY: randomBytes(32).toString("hex"),
    PAYOS_COLLECT_ACCOUNT_NUMBER: "123456789",
  };
  const config = new ConfigService(keys),
    adapter = new PayosAdapter(config);
  const sdk = new PayOS({
    clientId: keys.PAYOS_COLLECT_CLIENT_ID,
    apiKey: keys.PAYOS_COLLECT_API_KEY,
    checksumKey: keys.PAYOS_COLLECT_CHECKSUM_KEY,
    logLevel: "off",
  });
  let query = 0;
  adapter.getLink = async () => {
    query++;
    return { id: "other-link", orderCode: 1000001, transactions: [] };
  };
  const service = new PaymentsService({}, config, adapter, {
    publish: () => {
      throw new Error("must not publish");
    },
  });
  const base = {
    code: "00",
    orderCode: 1000001,
    amount: 100000,
    reference: "ref",
    currency: "VND",
    accountNumber: "123456789",
    paymentLinkId: "link",
  };
  const signed = async (data) => ({
    data,
    signature: await sdk.crypto.createSignatureFromObj(
      data,
      keys.PAYOS_COLLECT_CHECKSUM_KEY,
    ),
  });
  await assert.rejects(service.webhook({ data: base, signature: "bad" }));
  await assert.rejects(
    service.webhook(await signed({ ...base, currency: "USD" })),
  );
  assert.equal(query, 0);
  await assert.rejects(service.webhook(await signed(base)));
  assert.equal(query, 1);
  assert.deepEqual(
    await service.webhook(
      await signed({ ...base, orderCode: 123, accountNumber: "probe-account" }),
    ),
    { received: true },
  );
  assert.equal(query, 1);
});
test("email snapshot combo unit price uses charged snapshot not changed catalog", () => {
  const service = new PaymentsService({}, new ConfigService({}), {}, {});
  const order = {
    user: { email: "fixture@example.invalid" },
    showtime: {
      movie: { title: "Fixture", posterUrl: null },
      startAt: new Date(),
      format: "STANDARD_2D",
      auditorium: { name: "R1" },
    },
    seats: [{ seatCode: "A1", price: "100000" }],
    combo: { name: "Combo", price: "999999" },
    comboQuantity: 3,
    subtotal: "130000",
    discount: "0",
    totalAmount: "130000",
    voucherCode: null,
  };
  assert.equal(
    service.snapshot(order, "ticket", "code").comboUnitPrice,
    "10000",
  );
});

test("collection accepts only configured physical or signed per-order virtual account", async () => {
  const { collectionAccountMatches } =
    await import("../dist/payments/payment-domain.js");
  assert.equal(
    collectionAccountMatches("VQRORDER1", "123456789", "VQRORDER1"),
    true,
  );
  assert.equal(
    collectionAccountMatches("123456789", "123456789", "VQRORDER1"),
    true,
  );
  assert.equal(
    collectionAccountMatches("VQRORDER2", "123456789", "VQRORDER1"),
    false,
  );
  assert.equal(collectionAccountMatches("", "123456789", null), false);
});

test("verified Vietcombank CITAD maps to payout BIN; unknown CITAD cannot receive a payout", () => {
  const input = {
    counterAccountBankId: "01203001",
    counterAccountNumber: "123456789",
    counterAccountName: "FIXTURE",
  };
  assert.equal(payer(input).bin, "970436");
  assert.equal(payer({ ...input, counterAccountBankId: "99999999" }), null);
});
