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
