import test from "node:test";
import assert from "node:assert/strict";
import { voucherPrice, VOUCHERS } from "../dist/booking/vouchers.js";
import { BookingService } from "../dist/booking/booking.service.js";
import { Prisma } from "../dist/generated/prisma/client.js";
test("FIRST50 halves full bill; rounds discount down; CINE20 minimum; no stacking", () => {
  assert.equal(
    voucherPrice("240000", "100000", 1, "FIRST50").bankAmount,
    "170000",
  );
  assert.equal(voucherPrice("100001", "0", 0, "FIRST50").bankAmount, "50001");
  assert.equal(voucherPrice("300000", "0", 0, "CINE20").bankAmount, "280000");
  assert.equal(voucherPrice("300000", "0", 0, "").bankAmount, "300000");
  for (const code of ["FAKE", "FIRST50,CINE20", "first50"])
    assert.throws(() => voucherPrice("300000", "0", 0, code));
  assert.throws(() => voucherPrice("299999", "0", 0, "CINE20"));
  assert.throws(() => voucherPrice("99999999999999", "1", 1, "FIRST50"));
});
test("owned list excludes first voucher after purchase or another active reservation", async () => {
  for (const [used, reserved] of [
    [0, 0],
    [1, 0],
    [0, 1],
  ]) {
    let n = 0;
    const service = new BookingService(
      { order: { count: async () => (n++ === 0 ? used : reserved) } },
      {},
      {},
    );
    const result = await service.vouchers("user");
    assert.equal(
      result.items.some((v) => v.code === "FIRST50"),
      !used && !reserved,
    );
    assert.ok(result.items.some((v) => v.code === "CINE20"));
  }
});
test("server revalidates first purchase and same-order reservation; stored balance never reduces voucher checkout", async () => {
  const service = new BookingService({}, {}, {});
  service.now = async () => new Date("2030-01-01");
  const order = {
    id: "order",
    userId: "owner",
    creditUsed: new Prisma.Decimal(150000),
    seats: [{ price: new Prisma.Decimal(240000) }],
  };
  for (const [used, reserved] of [
    [0, 0],
    [1, 0],
    [0, 1],
  ]) {
    let n = 0;
    const tx = {
      user: {
        findUniqueOrThrow: async () => ({
          isActive: true,
          emailVerifiedAt: new Date(),
          role: "CUSTOMER",
          creditBalance: new Prisma.Decimal(150000),
        }),
      },
      order: {
        count: async ({ where }) => {
          assert.equal(where.userId, "owner");
          if (n++ === 0) return used;
          assert.equal(where.id.not, "order");
          return reserved;
        },
      },
    };
    const run = () =>
      service.pricing(tx, order, {
        combo_quantity: 0,
        voucher_code: "FIRST50",
      });
    if (used || reserved) await assert.rejects(run);
    else {
      const q = await run();
      assert.equal(q.bankAmount, "120000");
      assert.equal(q.creditUsed, "0");
      assert.equal(q.voucherCode, "FIRST50");
    }
  }
});
