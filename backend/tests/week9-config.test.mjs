import test from "node:test";
import assert from "node:assert/strict";
import { randomBytes } from "node:crypto";
import { validateEnvironment } from "../dist/config.js";
const base = () => ({
  DATABASE_URL: "postgresql://fixture:fixture@127.0.0.1:5432/fixture",
  JWT_ACCESS_SECRET: randomBytes(32).toString("hex"),
  JWT_REFRESH_SECRET: randomBytes(32).toString("hex"),
  JWT_EMAIL_SECRET: randomBytes(32).toString("hex"),
  GMAIL_USER: "fixture@example.invalid",
  GMAIL_APP_PASSWORD: randomBytes(8).toString("hex"),
  PUBLIC_BASE_URL: "https://example.invalid/api/v1",
  AVATAR_UPLOAD_DIR:
    process.platform === "win32" ? "D:/fixture" : "/tmp/fixture",
});
test("new integrations are disabled by default and require explicit valid configuration", () => {
  const e = base(),
    r = validateEnvironment(e);
  for (const key of [
    "PAYMENT_LIVE_ENABLED",
    "REFUND_PAYOUT_ENABLED",
    "TRANSACTION_EMAIL_ENABLED",
    "FCM_ENABLED",
  ]) {
    assert.equal(r[key], false);
    assert.throws(() => validateEnvironment({ ...e, [key]: "yes" }));
  }
  assert.throws(() =>
    validateEnvironment({ ...e, PAYMENT_LIVE_ENABLED: "true" }),
  );
  assert.throws(() =>
    validateEnvironment({ ...e, REFUND_PAYOUT_ENABLED: "true" }),
  );
  assert.throws(() => validateEnvironment({ ...e, FCM_ENABLED: "true" }));
});

test("Android compatibility configuration cannot enable blocking without a published verified APK", () => {
  const e = base();
  assert.equal(validateEnvironment(e).ANDROID_MIN_VERSION_CODE, 0);
  for (const config of [
    { ANDROID_MIN_VERSION_CODE: "-1" },
    { ANDROID_MIN_VERSION_CODE: "7x" },
    { ANDROID_MIN_VERSION_CODE: "8", ANDROID_LATEST_VERSION_CODE: "7" },
    { ANDROID_MIN_VERSION_CODE: "7" },
  ])
    assert.throws(() => validateEnvironment({ ...e, ...config }));
});

test("fixed proxy/retry is opt-in, validates credentials and cannot disable TLS", () => {
  const e = base();
  const enabled = {
    PAYOS_PAYOUT_PROXY_ENABLED: "true",
    PAYOS_PAYOUT_PROXY_SERVER: "http://proxy.fixture.invalid:3128",
  };
  assert.equal(validateEnvironment(e).REFUND_RETRY_ENABLED, false);
  assert.equal(validateEnvironment(e).PAYOS_PAYOUT_PROXY_ENABLED, false);
  assert.throws(() =>
    validateEnvironment({ ...e, REFUND_RETRY_ENABLED: "true" }),
  );
  for (const url of [
    "socks5://proxy.fixture.invalid",
    "http://user:password@proxy.fixture.invalid",
    "http://proxy.fixture.invalid/path",
    "http://proxy.fixture.invalid/?q=1",
  ])
    assert.throws(() =>
      validateEnvironment({ ...e, ...enabled, PAYOS_PAYOUT_PROXY_SERVER: url }),
    );
  assert.throws(() =>
    validateEnvironment({
      ...e,
      ...enabled,
      PAYOS_PAYOUT_PROXY_USERNAME: "fixture",
    }),
  );
  assert.throws(() =>
    validateEnvironment({
      ...e,
      ...enabled,
      NODE_TLS_REJECT_UNAUTHORIZED: "0",
    }),
  );
  assert.equal(
    validateEnvironment({ ...e, ...enabled, REFUND_RETRY_ENABLED: "true" })
      .REFUND_RETRY_ENABLED,
    true,
  );
});
