import { isIP } from "node:net";

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
  return { ...env, HOST: host, PORT: port };
}
