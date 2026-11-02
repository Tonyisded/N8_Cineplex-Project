import { isIP } from "node:net";
import path from "node:path";

export function validateEnvironment(env: Record<string, unknown>) {
  const host = env.HOST ?? "127.0.0.1";
  const rawPort = String(env.PORT ?? "5000");
  if (typeof host !== "string" || (host !== "localhost" && !isIP(host))) {
    throw new Error("HOST must be localhost or a valid IP address");
  }
  const port = Number(rawPort);
  if (
    !/^\d+$/.test(rawPort) ||
    !Number.isInteger(port) ||
    port < 1 ||
    port > 65535
  ) {
    throw new Error("PORT must be an integer from 1 to 65535");
  }
  if (typeof env.DATABASE_URL !== "string" || !env.DATABASE_URL) {
    throw new Error("DATABASE_URL is required");
  }
  try {
    const url = new URL(env.DATABASE_URL);
    if (
      !["postgres:", "postgresql:"].includes(url.protocol) ||
      !url.hostname ||
      url.pathname.length < 2
    ) {
      throw new Error("invalid");
    }
  } catch {
    throw new Error("DATABASE_URL must be a valid PostgreSQL connection URL");
  }
  const rawCrawlerEnabled = env.CRAWLER_ENABLED ?? "false";
  for (const key of [
    "JWT_ACCESS_SECRET",
    "JWT_REFRESH_SECRET",
    "JWT_EMAIL_SECRET",
  ]) {
    if (typeof env[key] !== "string" || (env[key] as string).length < 32)
      throw new Error(`${key} must contain at least 32 characters`);
  }
  if (
    new Set([
      env.JWT_ACCESS_SECRET,
      env.JWT_REFRESH_SECRET,
      env.JWT_EMAIL_SECRET,
    ]).size !== 3
  )
    throw new Error("JWT secrets must be distinct");
  if (
    typeof env.GMAIL_USER !== "string" ||
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(env.GMAIL_USER)
  )
    throw new Error("GMAIL_USER must be configured");
  if (
    typeof env.GMAIL_APP_PASSWORD !== "string" ||
    env.GMAIL_APP_PASSWORD.replace(/\s/g, "").length !== 16
  )
    throw new Error("GMAIL_APP_PASSWORD must be configured");
  let publicBase: URL;
  try {
    publicBase = new URL(String(env.PUBLIC_BASE_URL));
  } catch {
    throw new Error("PUBLIC_BASE_URL must be configured");
  }
  if (
    publicBase.username ||
    publicBase.password ||
    publicBase.search ||
    publicBase.hash ||
    !publicBase.pathname.endsWith("/api/v1") ||
    publicBase.protocol !== "https:" ||
    ["127.0.0.1", "localhost", "[::1]"].includes(publicBase.hostname)
  )
    throw new Error(
      "PUBLIC_BASE_URL must use public HTTPS, not localhost, and end in /api/v1",
    );
  if (
    typeof env.AVATAR_UPLOAD_DIR !== "string" ||
    !path.isAbsolute(env.AVATAR_UPLOAD_DIR)
  )
    throw new Error("AVATAR_UPLOAD_DIR must be an absolute writable path");
  if (!["true", "false"].includes(String(env.TRUST_PROXY ?? "false")))
    throw new Error("TRUST_PROXY must be true or false");
  if (!["true", "false"].includes(String(rawCrawlerEnabled))) {
    throw new Error("CRAWLER_ENABLED must be true or false");
  }
  const switches = [
    "PAYMENT_LIVE_ENABLED",
    "REFUND_PAYOUT_ENABLED",
    "TRANSACTION_EMAIL_ENABLED",
    "FCM_ENABLED",
  ];
  for (const key of switches)
    if (!["true", "false"].includes(String(env[key] ?? "false")))
      throw new Error(`${key} must be true or false`);
  const enabled = (key: string) => String(env[key] ?? "false") === "true";
  if (enabled("PAYMENT_LIVE_ENABLED") || enabled("REFUND_PAYOUT_ENABLED")) {
    for (const key of ["TICKET_QR_SECRET", "PAYMENT_DATA_ENCRYPTION_KEY"]) {
      if (typeof env[key] !== "string" || (env[key] as string).length < 32)
        throw new Error(`${key} must be configured`);
    }
    if (!/^[a-f0-9]{64}$/i.test(String(env.PAYMENT_DATA_ENCRYPTION_KEY)))
      throw new Error("PAYMENT_DATA_ENCRYPTION_KEY must be 64 hex characters");
  }
  for (const [flag, prefix] of [
    ["PAYMENT_LIVE_ENABLED", "PAYOS_COLLECT"],
    ["REFUND_PAYOUT_ENABLED", "PAYOS_PAYOUT"],
  ]) {
    if (enabled(flag))
      for (const suffix of ["CLIENT_ID", "API_KEY", "CHECKSUM_KEY"]) {
        const key = `${prefix}_${suffix}`;
        if (typeof env[key] !== "string" || !(env[key] as string).trim())
          throw new Error(`${key} must be configured`);
      }
  }
  if (
    enabled("PAYMENT_LIVE_ENABLED") &&
    !/^\d+$/.test(String(env.PAYOS_COLLECT_ACCOUNT_NUMBER ?? ""))
  )
    throw new Error("PAYOS_COLLECT_ACCOUNT_NUMBER must be configured");
  if (enabled("FCM_ENABLED"))
    for (const key of [
      "FIREBASE_PROJECT_ID",
      "FIREBASE_CLIENT_EMAIL",
      "FIREBASE_PRIVATE_KEY",
    ]) {
      if (typeof env[key] !== "string" || !(env[key] as string).trim())
        throw new Error(`${key} must be configured`);
    }
  const minimum = Number(env.ANDROID_MIN_VERSION_CODE ?? 0),
    latest = Number(env.ANDROID_LATEST_VERSION_CODE ?? 7);
  if (
    !Number.isSafeInteger(minimum) ||
    minimum < 0 ||
    !Number.isSafeInteger(latest) ||
    latest < Math.max(1, minimum)
  )
    throw new Error("Invalid Android release versions");
  const apkPath = String(env.ANDROID_APK_PATH ?? ""),
    apkHash = String(env.ANDROID_APK_SHA256 ?? "");
  if (
    (minimum > 0 || apkPath || apkHash) &&
    (!path.isAbsolute(apkPath) ||
      path.extname(apkPath).toLowerCase() !== ".apk" ||
      !/^[a-f0-9]{64}$/.test(apkHash))
  )
    throw new Error("Published Android APK path/hash must be configured");
  return {
    ...env,
    ANDROID_MIN_VERSION_CODE: minimum,
    ANDROID_LATEST_VERSION_CODE: latest,
    ANDROID_APK_PATH: apkPath,
    ANDROID_APK_SHA256: apkHash,
    ...Object.fromEntries(switches.map((key) => [key, enabled(key)])),
    HOST: host,
    PORT: port,
    CRAWLER_ENABLED: String(rawCrawlerEnabled) === "true",
    PUBLIC_BASE_URL: publicBase.href.replace(/\/$/, ""),
    TRUST_PROXY: String(env.TRUST_PROXY ?? "false") === "true",
  };
}
