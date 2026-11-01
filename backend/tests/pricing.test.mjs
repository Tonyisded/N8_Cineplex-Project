import test from "node:test";
import assert from "node:assert/strict";
import {
  calculatePrice,
  signQuote,
  readQuote,
} from "../dist/booking/pricing.js";
test("promotion boundaries, exact VND and amount bounds", () => {
  assert.equal(calculatePrice("299999", "0", 0, "CINE20", "0").discount, "0");
  assert.equal(
    calculatePrice("300000", "0", 0, "CINE20", "999999").bankAmount,
    "0",
  );
  assert.equal(
    calculatePrice("300000", "0", 0, "CINE20", "999999").creditUsed,
    "280000",
  );
  assert.throws(() => calculatePrice("1", "0", 0, "invalid", "0"));
  assert.throws(() => calculatePrice("99999999999999", "1", 1, "", "0"));
  const token = signQuote({ id: "a" }, "secret");
  assert.deepEqual(readQuote(token, "secret"), { id: "a" });
  assert.throws(() => readQuote(token, "wrong"));
});

test("quantity, promotion and Credit matrix preserves exact money invariants", () => {
  for (const quantity of [0, 1, 9])
    for (const balance of [0, 150000, 320000, 2000000]) {
      const q = calculatePrice(
        "240000",
        "100000",
        quantity,
        " cine20 ",
        String(balance),
      );
      const subtotal = 240000 + 100000 * quantity;
      const discount = subtotal >= 300000 ? 20000 : 0;
      assert.equal(q.subtotal, String(subtotal));
      assert.equal(q.discount, String(discount));
      assert.equal(q.totalAmount, String(subtotal - discount));
      assert.equal(
        q.creditUsed,
        String(Math.min(balance, subtotal - discount)),
      );
      assert.equal(
        BigInt(q.bankAmount) + BigInt(q.creditUsed),
        BigInt(q.totalAmount),
      );
      assert.ok(BigInt(q.bankAmount) >= 0n);
    }
  for (const amount of ["299999", "300000", "300001"]) {
    assert.equal(
      calculatePrice(amount, "0", 0, "CINE20", "0").discount,
      BigInt(amount) >= 300000n ? "20000" : "0",
    );
  }
  assert.equal(
    calculatePrice("99999999999999", "0", 0, "", "0").totalAmount,
    "99999999999999",
  );
  assert.throws(() => calculatePrice("0", "0", 0, "", "-1"));
});
