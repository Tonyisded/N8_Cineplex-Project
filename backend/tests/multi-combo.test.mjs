import test from "node:test";
import assert from "node:assert/strict";
import { BookingService } from "../dist/booking/booking.service.js";
import { PaymentsService } from "../dist/payments/payments.service.js";
import { Prisma } from "../dist/generated/prisma/client.js";
import { purchaseEmail } from "../dist/notifications/transaction-email.js";
import { fingerprint } from "../dist/booking/pricing.js";
const a = "11111111-1111-4111-8111-111111111111",
  b = "22222222-2222-4222-8222-222222222222";
const rows = new Map([
  [
    a,
    {
      id: a,
      name: "Combo A",
      price: new Prisma.Decimal(100000),
      status: "AVAILABLE",
    },
  ],
  [
    b,
    {
      id: b,
      name: "Nước B",
      price: new Prisma.Decimal(30000),
      status: "AVAILABLE",
    },
  ],
]);
const order = {
  id: "order",
  userId: "owner",
  seats: [{ price: "150000", seatCode: "A1" }],
};
const tx = {
  $queryRaw: async () => [],
  combo: { findUnique: async ({ where }) => rows.get(where.id) },
  user: {
    findUniqueOrThrow: async () => ({
      isActive: true,
      emailVerifiedAt: new Date(),
      role: "CUSTOMER",
    }),
  },
  order: { count: async () => 0 },
};
const service = new BookingService({}, {}, {});
service.now = async () => new Date();
test("multiple combos, quantity12, server pricing and voucher cover whole cart", async () => {
  const result = await service.pricing(tx, order, {
    combo_quantity: 0,
    combo_items: [
      { id: b, quantity: 2 },
      { id: a, quantity: 12 },
    ],
    voucher_code: "FIRST50",
  });
  assert.equal(result.subtotal, "1410000");
  assert.equal(result.discount, "705000");
  assert.equal(result.bankAmount, "705000");
  assert.equal(result.comboItems.length, 2);
  assert.equal(result.comboItems[0].quantity, 12);
  assert.equal(result.comboId, null);
  assert.notEqual(
    fingerprint(result.comboItems),
    fingerprint([{ ...result.comboItems[0], id: b }, result.comboItems[1]]),
  );
});
test("legacy quantity12 works; duplicate, fractional, mixed, null, unavailable and overflowing carts fail", async () => {
  const legacy = await service.pricing(tx, order, {
    combo_id: a,
    combo_quantity: 12,
  });
  assert.equal(legacy.subtotal, "1350000");
  for (const body of [
    { combo_quantity: 0, combo_items: null },
    { combo_id: a, combo_quantity: 1, combo_items: [] },
    {
      combo_quantity: 0,
      combo_items: [
        { id: a, quantity: 1 },
        { id: a.toUpperCase(), quantity: 2 },
      ],
    },
    { combo_quantity: 0, combo_items: [{ id: a, quantity: 1.5 }] },
    { combo_quantity: 0, combo_items: [{ id: a, quantity: 2147483648 }] },
  ])
    await assert.rejects(service.pricing(tx, order, body));
  rows.get(b).status = "UNAVAILABLE";
  await assert.rejects(
    service.pricing(tx, order, {
      combo_quantity: 0,
      combo_items: [{ id: b, quantity: 1 }],
    }),
  );
  rows.get(b).status = "AVAILABLE";
  const old = rows.get(a).price;
  rows.get(a).price = new Prisma.Decimal("99999999999999");
  try {
    await assert.rejects(
      service.pricing(tx, order, { combo_id: a, combo_quantity: 2 }),
    );
  } finally {
    rows.get(a).price = old;
  }
});
test("ticket and email retain each immutable combo line and do not use future menu prices", () => {
  const combos = [
    {
      id: a,
      name: "Combo A",
      unitPrice: "100000",
      quantity: 12,
      totalPrice: "1200000",
    },
    {
      id: b,
      name: "Nước B",
      unitPrice: "30000",
      quantity: 2,
      totalPrice: "60000",
    },
  ];
  const snapshot = new PaymentsService({}, {}, {}, {}).snapshot(
    {
      ...order,
      subtotal: "1410000",
      discount: "705000",
      totalAmount: "705000",
      comboQuantity: 0,
      comboItems: combos,
      user: { email: "fixture@example.invalid" },
      showtime: {
        startAt: new Date("2030-01-01"),
        format: "2D",
        movie: { title: "Fixture" },
        auditorium: { name: "Room" },
      },
      voucherCode: "FIRST50",
    },
    "ticket",
    "N8FIXTURE",
  );
  assert.deepEqual(snapshot.comboItems, combos);
  assert.equal(snapshot.comboUnitPrice, "1260000");
  const email = purchaseEmail(snapshot, Buffer.from("fixture"));
  assert.ok(email.html.includes("Combo A × 12"));
  assert.ok(email.html.includes("Nước B × 2"));
  assert.ok(email.html.includes("1.200.000"));
  assert.ok(email.html.includes("60.000"));
});

test("cart fingerprints survive JSONB object-key reordering without weakening array identity", () => {
  const before = [
    {
      id: a,
      name: "Combo A",
      unitPrice: "100000",
      quantity: 12,
      totalPrice: "1200000",
    },
  ];
  const after = [
    {
      quantity: 12,
      totalPrice: "1200000",
      id: a,
      unitPrice: "100000",
      name: "Combo A",
    },
  ];
  assert.equal(fingerprint(before), fingerprint(after));
  assert.notEqual(
    fingerprint(before),
    fingerprint([{ ...after[0], quantity: 11 }]),
  );
});
