import "reflect-metadata";
import test from "node:test";
import assert from "node:assert/strict";
import { UnauthorizedException, ValidationPipe } from "@nestjs/common";
import { Test } from "@nestjs/testing";
import { SwaggerModule, DocumentBuilder } from "@nestjs/swagger";
import { AuthService } from "../dist/auth/auth.service.js";
import {
  JwtGuard,
  RoleGuard,
  VerifiedEmailGuard,
} from "../dist/auth/guards.js";
import { PaymentsService } from "../dist/payments/payments.service.js";
import {
  OrderPaymentsController,
  TicketsController,
  PayosWebhookController,
  PaymentIssuesController,
} from "../dist/payments/payments.controller.js";
const id = "81000000-0000-4000-8000-000000000001";
const userId = "81000000-0000-4000-8000-000000000002";
const now = "2026-11-02T00:00:00.000Z";
const payment = {
  id,
  orderId: id,
  status: "PENDING",
  operationState: "PROCESSING",
  amount: "100001",
  expiresAt: now,
  paymentLinkId: "link",
  qrCode: "provider-qr",
  checkoutUrl: "https://pay.payos.vn/web/fixture",
  ticketId: null,
};
const refund = {
  id,
  status: "PENDING",
  operationState: "REQUESTED",
  amount: "100001",
  accountMasked: "970422 ••••1234",
  reason: null,
  paidAt: null,
};
const ticket = {
  id,
  orderId: id,
  status: "VALID",
  bookingCode: "N8FIXTURE",
  issuedAt: now,
  expiresAt: now,
  snapshot: {
    movieTitle: "Fixture",
    startAt: now,
    subtotal: "100001",
    discount: "0",
    totalAmount: "100001",
    comboQuantity: 0,
    seatCodes: ["A1"],
  },
  refundEligible: true,
  refundReason: null,
};
const envelope = (item) => ({ serverTime: now, item });
test("payment/ticket/refund routes, guards, HTTP statuses and Swagger contracts without live services", async (t) => {
  const calls = [];
  const service = {
    orders: async (uid) => {
      calls.push(["orders", uid]);
      return { serverTime: now, items: [] };
    },
    start: async (...args) => {
      calls.push(["start", ...args]);
      return envelope(payment);
    },
    check: async () => envelope(payment),
    tickets: async (uid, cursor) => {
      calls.push(["tickets", uid, cursor]);
      return { serverTime: now, items: [ticket], nextCursor: null };
    },
    ticket: async () => envelope(ticket),
    qr: async () => envelope({ payload: "N8T1:fixture" }),
    refund: async () => envelope(refund),
    refundState: async () => envelope(refund),
    issues: async () => ({ items: [] }),
    resolve: async (...args) => {
      calls.push(["resolve", ...args]);
      return envelope(refund);
    },
    webhook: async () => ({ received: true }),
  };
  const auth = {
    authenticate: async (token) => {
      if (!["customer", "admin"].includes(token))
        throw new UnauthorizedException();
      return {
        id: userId,
        role: token === "admin" ? "ADMIN" : "CUSTOMER",
        isActive: true,
        emailVerifiedAt: new Date(now),
      };
    },
  };
  const module = await Test.createTestingModule({
    controllers: [
      OrderPaymentsController,
      TicketsController,
      PayosWebhookController,
      PaymentIssuesController,
    ],
    providers: [
      JwtGuard,
      RoleGuard,
      VerifiedEmailGuard,
      { provide: AuthService, useValue: auth },
      { provide: PaymentsService, useValue: service },
    ],
  }).compile();
  const app = module.createNestApplication({ logger: false });
  app.setGlobalPrefix("api/v1");
  app.useGlobalPipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
    }),
  );
  await app.listen(0, "127.0.0.1");
  t.after(() => app.close());
  const base = (await app.getUrl()) + "/api/v1";
  const request = (path, options = {}, role = "customer") =>
    fetch(base + path, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...(role ? { Authorization: "Bearer " + role } : {}),
        ...options.headers,
      },
    });
  await t.test(
    "actual HTTP enforces owner session/role and 200/202 without accepting client price",
    async () => {
      assert.equal((await request("/orders", {}, null)).status, 401);
      assert.equal((await request("/orders", {}, "admin")).status, 403);
      const response = await request("/orders/" + id + "/payment", {
        method: "POST",
        body: JSON.stringify({ amount: "1", paid: true }),
      });
      assert.equal(response.status, 200);
      assert.equal((await response.json()).item.amount, "100001");
      assert.deepEqual(
        calls.find((c) => c[0] === "start"),
        ["start", id, userId],
      );
      assert.equal(
        (await request("/tickets/" + id + "/refund", { method: "POST" }))
          .status,
        202,
      );
      assert.equal((await request("/tickets/" + id + "/qr")).status, 200);
      assert.equal(
        (await request("/orders/not-a-uuid/payment", { method: "POST" }))
          .status,
        400,
      );
      assert.equal((await request("/admin/payment-issues")).status, 403);
      assert.equal(
        (await request("/admin/payment-issues", {}, "admin")).status,
        200,
      );
      assert.equal(
        (
          await request(
            "/admin/payment-issues/" + id + "/resolve",
            {
              method: "POST",
              body: JSON.stringify({
                action: "MARK_RESOLVED",
                note: "fixture reference",
              }),
            },
            "admin",
          )
        ).status,
        200,
      );
      assert.equal(
        (
          await request(
            "/admin/payment-issues/" + id + "/resolve",
            {
              method: "POST",
              body: JSON.stringify({
                action: "MANUAL_FINALIZE",
                note: "fixture reference",
              }),
            },
            "admin",
          )
        ).status,
        400,
      );
      assert.equal(
        (
          await request(
            "/admin/payment-issues/" + id + "/resolve",
            {
              method: "POST",
              body: JSON.stringify({ action: "MARK_RESOLVED", note: "x" }),
            },
            "admin",
          )
        ).status,
        400,
      );
      assert.equal(
        (await request("/webhooks/payos", { method: "POST", body: "{}" }, null))
          .status,
        200,
      );
    },
  );
  await t.test(
    "Swagger documents string VND, states, IDs, pagination and admin request constraints",
    () => {
      const spec = SwaggerModule.createDocument(
        app,
        new DocumentBuilder().addBearerAuth().build(),
      );
      const schema = (path, method, code) =>
        spec.paths["/api/v1" + path][method].responses[code].content[
          "application/json"
        ].schema;
      const pay = schema("/orders/{id}/payment", "post", "200").properties.item;
      assert.equal(pay.properties.amount.type, "string");
      assert.match(pay.properties.amount.pattern, /0-9/);
      assert.equal(pay.properties.orderId.format, "uuid");
      assert.ok(pay.properties.operationState.enum.includes("UNKNOWN"));
      assert.equal(pay.properties.qrCode.nullable, true);
      const tickets = schema("/tickets", "get", "200");
      assert.equal(tickets.properties.nextCursor.nullable, true);
      assert.equal(
        tickets.properties.items.items.properties.snapshot.properties
          .totalAmount.type,
        "string",
      );
      assert.ok(
        tickets.properties.items.items.properties.status.enum.includes(
          "REFUND_PENDING",
        ),
      );
      const refunds = schema("/tickets/{id}/refund", "post", "202").properties
        .item;
      assert.equal(refunds.properties.amount.type, "string");
      assert.equal(refunds.properties.accountMasked.nullable, true);
      assert.ok(
        refunds.properties.operationState.enum.includes("NEEDS_REVIEW"),
      );
      const qr = schema("/tickets/{id}/qr", "get", "200").properties.item;
      assert.equal(qr.properties.payload.type, "string");
      assert.equal(
        spec.components.schemas.ResolveDto.properties.note.minLength,
        8,
      );
      assert.equal(
        spec.components.schemas.ResolveDto.properties.note.maxLength,
        1000,
      );
      assert.deepEqual(
        spec.components.schemas.ResolveDto.properties.action.enum,
        ["MARK_BANK_REFUNDED", "MARK_RESOLVED"],
      );
      assert.ok(spec.paths["/api/v1/orders/{id}/payment"].post.security);
      assert.equal(
        spec.paths["/api/v1/webhooks/payos"].post.security,
        undefined,
      );
    },
  );
});
