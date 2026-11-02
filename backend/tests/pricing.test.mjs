import test from "node:test";
import assert from "node:assert/strict";
import { signQuote, readQuote } from "../dist/booking/pricing.js";
import { voucherPrice } from "../dist/booking/vouchers.js";
test("signed quote roundtrip, wrong key and tampering", () => {
  const token = signQuote({ version: 2, id: "a" }, "fixture-key");
  assert.deepEqual(readQuote(token, "fixture-key"), { version: 2, id: "a" });
  assert.throws(() => readQuote(token, "wrong"));
  assert.throws(() => readQuote(token + "x", "fixture-key"));
});
test("voucher quantity and VND matrix; amount paid never uses an internal balance", () => {
  for (const quantity of [0, 1, 9])
    for (const code of ["", "FIRST50", "CINE20"]) {
      const subtotal = 240000n + 100000n * BigInt(quantity);
      if (code === "CINE20" && subtotal < 300000n) {
        assert.throws(() => voucherPrice("240000", "100000", quantity, code));
        continue;
      }
      const q = voucherPrice("240000", "100000", quantity, code);
      const discount =
        code === "FIRST50" ? subtotal / 2n : code === "CINE20" ? 20000n : 0n;
      assert.equal(q.bankAmount, String(subtotal - discount));
      assert.equal(q.totalAmount, q.bankAmount);
      assert.ok(!("creditUsed" in q));
    }
  assert.throws(() => voucherPrice("99999999999999", "1", 1, ""));
  assert.equal(voucherPrice("100001", "0", 0, "FIRST50").bankAmount, "50001");
});
