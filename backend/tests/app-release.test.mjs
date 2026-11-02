import "reflect-metadata";
import test from "node:test";
import assert from "node:assert/strict";
import { ConfigService } from "@nestjs/config";
import { Test } from "@nestjs/testing";
import {
  appVersionMiddleware,
  AppReleaseController,
} from "../dist/app-release.js";
import { PaymentsService } from "../dist/payments/payments.service.js";
const config = new ConfigService({
  ANDROID_MIN_VERSION_CODE: 7,
  ANDROID_LATEST_VERSION_CODE: 7,
  PUBLIC_BASE_URL: "https://example.invalid/api/v1",
  ANDROID_APK_SHA256: "a".repeat(64),
});
test("old/missing/malformed versions receive426 while webhook, recovery links and new app remain usable", () => {
  const middleware = appVersionMiddleware(config);
  for (const [path, version, pass] of [
    ["/orders", undefined, false],
    ["/orders", "6", false],
    ["/orders", "7", true],
    ["/orders", "8", true],
    ["/orders", "7oops", false],
    ["/webhooks/payos", undefined, true],
    ["/auth/verify-email", undefined, true],
    ["/auth/reset-password", undefined, true],
    ["/app/android/download", undefined, true],
    ["/app/version", undefined, true],
    ["/health", undefined, true],
    ["/assets/avatars/id/photo.webp", undefined, true],
    ["/webhooks/payos/evil", undefined, false],
  ]) {
    let next = false,
      status;
    const res = {
      setHeader() {},
      status(v) {
        status = v;
        return this;
      },
      json(v) {
        assert.equal(v.code, "APP_UPDATE_REQUIRED");
        assert.equal(v.minimumVersionCode, 7);
      },
    };
    middleware({ path, method: "GET", get: () => version }, res, () => {
      next = true;
    });
    assert.equal(next, pass, path);
    if (!pass) assert.equal(status, 426);
  }
});
test("public manifest remains reachable with HTTP no-store and download absent returns404", async () => {
  const module = await Test.createTestingModule({
    controllers: [AppReleaseController],
    providers: [{ provide: ConfigService, useValue: config }],
  }).compile();
  const app = module.createNestApplication();
  app.setGlobalPrefix("api/v1");
  app.use("/api/v1", appVersionMiddleware(config));
  await app.listen(0, "127.0.0.1");
  try {
    const base = await app.getUrl();
    const m = await fetch(base + "/api/v1/app/version");
    assert.equal(m.status, 200);
    assert.equal(m.headers.get("cache-control"), "no-store");
    const body = await m.json();
    assert.equal(body.minimumVersionCode, 7);
    assert.equal(body.latestVersionCode, 7);
    assert.equal(
      (await fetch(base + "/api/v1/app/android/download")).status,
      404,
    );
  } finally {
    await app.close();
  }
});
test("automatic payment GET reads only persisted owned state and never invokes provider or creates rows", async () => {
  const deadline = new Date("2030-01-01T00:00:00Z");
  const db = {
    order: {
      findFirst: async ({ where }) =>
        where.userId === "owner"
          ? {
              id: "order",
              status: "PAID",
              expiresAt: deadline,
              totalAmount: { toString: () => "0" },
              ticket: { id: "ticket" },
              payments: [],
            }
          : null,
    },
  };
  const provider = new Proxy(
    {},
    {
      get() {
        throw new Error("read-only status must not reach provider");
      },
    },
  );
  const service = new PaymentsService(db, config, provider, {});
  const result = await service.paymentStatus("order", "owner");
  assert.equal(result.item.status, "PAID");
  assert.equal(result.item.ticketId, "ticket");
  assert.equal(result.item.amount, "0");
  await assert.rejects(service.paymentStatus("order", "other"));
});
