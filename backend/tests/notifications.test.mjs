import "reflect-metadata";
import test from "node:test";
import assert from "node:assert/strict";
import { randomBytes, randomUUID } from "node:crypto";
import {
  purchaseEmail,
  refundEmail,
  escapeHtml,
} from "../dist/notifications/transaction-email.js";
import {
  NotificationsService,
  notificationRetryAt,
  refundNoticeEvents,
} from "../dist/notifications/notifications.service.js";
import { FcmService } from "../dist/notifications/fcm.service.js";
const snapshot = () => ({
  movieTitle: "Phim <script>alert(1)</script> & bạn",
  posterUrl: "https://images.example.test/poster.png",
  startAt: "2026-12-01T10:00:00Z",
  format: "2D",
  auditorium: "Phòng 1",
  seatCodes: ["A1", "A2"],
  seats: [
    { seatCode: "A1", price: "120000" },
    { seatCode: "A2", price: "120000" },
  ],
  comboName: "Combo đôi",
  comboQuantity: 1,
  comboUnitPrice: "100000",
  subtotal: "340000",
  discount: "20000",
  totalAmount: "320000",
  voucherCode: "CINE20",
  email: "buyer@example.test",
  bookingCode: "N8TESTONLY",
  ticketId: randomUUID(),
});
const config = () => {
  const values = {
    TRANSACTION_EMAIL_ENABLED: true,
    FCM_ENABLED: false,
    TICKET_QR_SECRET: randomBytes(32).toString("hex"),
  };
  return {
    get: (key) => values[key],
    getOrThrow: (key) => {
      assert.ok(key in values);
      return values[key];
    },
  };
};
test("purchase email is escaped Vietnamese, priced snapshot with poster, quantities and CID QR", () => {
  const s = snapshot(),
    mail = purchaseEmail(s, Buffer.from("synthetic-png"));
  assert.match(mail.html, /Đặt vé thành công/);
  assert.match(mail.html, /THÔNG TIN GIAO DỊCH/);
  assert.match(mail.html, /&lt;script&gt;/);
  assert.doesNotMatch(mail.html, /<script>/);
  assert.match(mail.html, /https:\/\/images.example.test\/poster.png/);
  assert.match(mail.html, /2 ghế · 1 vé điện tử/);
  assert.match(mail.html, /120[.,]000 đ × 2/);
  assert.match(mail.html, /320[.,]000 đ/);
  assert.match(mail.html, /cid:ticket-qr/);
  assert.equal(mail.attachments[0].cid, "ticket-qr");
  assert.match(mail.text, /Giảm: 20[.,]000 đ/);
  assert.doesNotMatch(mail.text, /Gi\?m|V\? c/);
  assert.equal(escapeHtml(`"'<>&`), "&quot;&#39;&lt;&gt;&amp;");
});
test("unsafe/missing poster, no combo, zero price and already refunded receipt remain usable as plain receipt", () => {
  const s = {
    ...snapshot(),
    posterUrl: "javascript:alert(1)",
    comboQuantity: 0,
    totalAmount: "0",
  };
  const mail = purchaseEmail(s, Buffer.alloc(0), false);
  assert.doesNotMatch(mail.html, /javascript:|cid:ticket-qr|Combo đôi ×/);
  assert.match(mail.html, /VÉ KHÔNG CÒN HIỆU LỰC/);
  assert.equal(mail.attachments.length, 0);
  assert.match(mail.html, /0 đ/);
  assert.match(mail.text, /không còn hiệu lực/);
});
test("refund pending/review never claim money returned; completion includes masked recipient and reference", () => {
  const s = snapshot(),
    r = {
      id: randomUUID(),
      amount: "320000",
      recipientLabel: "970422 ••••1234",
      reference: "synthetic-ref",
    };
  const requested = refundEmail(s, "requested", r),
    review = refundEmail(s, "review", r),
    done = refundEmail(s, "completed", r);
  assert.match(requested.text, /chưa phải xác nhận tiền đã về/);
  assert.doesNotMatch(requested.subject, /Hoàn tiền thành công/);
  assert.match(review.text, /chưa hoàn tất/);
  assert.match(done.subject, /Hoàn tiền thành công/);
  assert.match(done.html, /••••1234/);
  assert.match(done.text, /synthetic-ref/);
  assert.doesNotMatch(done.html, /cid:ticket-qr/);
});
test("ticket email failure persists backoff without touching order/payment/ticket validity", async () => {
  const writes = [],
    messages = [],
    s = snapshot();
  const db = {
    ticket: {
      findMany: async () => [{ id: s.ticketId }],
      updateMany: async () => ({ count: 1 }),
      findUniqueOrThrow: async () => ({
        id: s.ticketId,
        snapshot: s,
        status: "VALID",
        expiresAt: new Date(Date.now() + 86400000),
        emailAttempts: 0,
        emailSentAt: null,
        fcmSentAt: null,
        order: { user: { id: randomUUID(), fcmToken: null } },
      }),
      update: async (x) => {
        writes.push(x.data);
        return {};
      },
    },
    $queryRaw: async () => [],
  };
  const worker = new NotificationsService(
    db,
    config(),
    {
      sendTransaction: async (...x) => {
        messages.push(x);
        return false;
      },
    },
    { send: async () => assert.fail("FCM disabled") },
  );
  await worker.tick();
  assert.equal(messages.length, 1);
  assert.ok(writes.some((x) => x.emailAttempts?.increment === 1));
  assert.ok(writes.some((x) => x.emailNextAttemptAt instanceof Date));
  assert.ok(writes.every((x) => !("status" in x)));
  assert.ok(writes.every((x) => !("paidAt" in x)));
});
test("quick refund still sends purchase receipt without usable QR", async () => {
  const s = snapshot(),
    messages = [];
  const db = {
    ticket: {
      findMany: async () => [{ id: s.ticketId }],
      updateMany: async () => ({ count: 1 }),
      findUniqueOrThrow: async () => ({
        id: s.ticketId,
        snapshot: s,
        status: "REFUNDED",
        expiresAt: new Date(Date.now() + 86400000),
        emailAttempts: 0,
        emailSentAt: null,
        fcmSentAt: null,
        order: { user: { id: randomUUID(), fcmToken: null } },
      }),
      update: async () => ({}),
    },
    $queryRaw: async () => [],
  };
  await new NotificationsService(
    db,
    config(),
    {
      sendTransaction: async (_to, m) => {
        messages.push(m);
        return true;
      },
    },
    { send: async () => false },
  ).tick();
  assert.equal(messages.length, 1);
  assert.match(messages[0].html, /VÉ KHÔNG CÒN HIỆU LỰC/);
  assert.equal(messages[0].attachments.length, 0);
});
test("refund retry persists individual notices and never writes financial state", async () => {
  const s = snapshot(),
    id = randomUUID(),
    writes = [],
    messages = [];
  const p = {
    id,
    kind: "REFUND",
    operationState: "SUCCEEDED",
    notificationState: { recipientLabel: "970422 ••••1234" },
    expectedAmount: { toString: () => "320000" },
    paidAt: new Date(),
    providerTransactionId: "synthetic-ref",
    order: {
      ticket: { id: s.ticketId, snapshot: s },
      user: { id: randomUUID(), fcmToken: null },
    },
  };
  const payment = {
    findUniqueOrThrow: async () => p,
    update: async (x) => {
      writes.push(structuredClone(x.data));
      p.notificationState = structuredClone(x.data.notificationState);
      return p;
    },
  };
  const db = {
    ticket: { findMany: async () => [] },
    payment,
    $queryRaw: async () => [{ id }],
    $transaction: async (fn) => fn({ $queryRaw: async () => [], payment }),
  };
  const worker = new NotificationsService(
    db,
    config(),
    {
      sendTransaction: async (_to, m) => {
        messages.push(m);
        return true;
      },
    },
    { send: async () => false },
  );
  await worker.tick();
  assert.equal(messages.length, 2);
  assert.match(messages[0].subject, /tiếp nhận/);
  assert.match(messages[1].subject, /Hoàn tiền thành công/);
  assert.ok(p.notificationState.requested.sentAt);
  assert.ok(p.notificationState.completed.sentAt);
  assert.equal(p.notificationState.recipientLabel, "970422 ••••1234");
  assert.ok(
    writes.every(
      (x) => Object.keys(x).length === 1 && "notificationState" in x,
    ),
  );
  await worker.tick();
  assert.equal(messages.length, 2);
});
test("flags default disabled, retry cap is bounded and refund event selection is explicit", async () => {
  const worker = new NotificationsService({}, { get: () => false }, {}, {});
  await worker.tick();
  const fcm = new FcmService({ get: () => false }, {});
  assert.equal(await fcm.send("u", "token", "title", "body", "t"), false);
  const now = new Date();
  assert.equal(notificationRetryAt(99, now) - now, 3600000);
  assert.deepEqual(refundNoticeEvents("UNKNOWN"), ["requested"]);
  assert.deepEqual(refundNoticeEvents("NEEDS_REVIEW"), ["requested", "review"]);
  assert.deepEqual(refundNoticeEvents("SUCCEEDED"), ["requested", "completed"]);
});
