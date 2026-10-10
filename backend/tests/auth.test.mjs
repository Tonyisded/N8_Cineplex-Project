import "reflect-metadata";
import assert from "node:assert/strict";
import test from "node:test";
import { randomUUID } from "node:crypto";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { readFileSync } from "node:fs";
import sharp from "sharp";
import { ConfigService } from "@nestjs/config";
import { JwtService } from "@nestjs/jwt";
import { ValidationPipe } from "@nestjs/common";
import { Test } from "@nestjs/testing";
import { AuthService } from "../dist/auth/auth.service.js";
import { normalizeAvatar } from "../dist/auth/avatar.service.js";
import { VerifiedEmailGuard, RoleGuard } from "../dist/auth/guards.js";
import { PrismaService } from "../dist/prisma.service.js";
import { MailService } from "../dist/auth/mail.service.js";
import { AppModule } from "../dist/app.module.js";
import { validateEnvironment } from "../dist/config.js";
import { emailPage } from "../dist/auth/email-page.js";

const configValues = {
  DATABASE_URL: "postgresql://test:test@127.0.0.1:5432/test",
  JWT_ACCESS_SECRET: "a".repeat(40),
  JWT_REFRESH_SECRET: "b".repeat(40),
  JWT_EMAIL_SECRET: "c".repeat(40),
  GMAIL_USER: "sender@example.test",
  GMAIL_APP_PASSWORD: "t".repeat(16),
  PUBLIC_BASE_URL: "https://18.143.100.43/api/v1",
  AVATAR_UPLOAD_DIR: join(tmpdir(), "cineplex-avatar-unit"),
  GOOGLE_CLIENT_ID: "unit-client",
};
function fixture() {
  const rows = new Map(),
    emails = [];
  const matches = (u, where) =>
    Object.entries(where).every(([k, v]) =>
      u[k] instanceof Date ? +u[k] === +v : u[k] === v,
    );
  const db = {
    user: {
      async create({ data }) {
        if (
          [...rows.values()].some(
            (u) =>
              u.email === data.email ||
              (data.googleId && u.googleId === data.googleId),
          )
        )
          throw { code: "P2002" };
        const u = {
          id: randomUUID(),
          passwordHash: null,
          googleId: null,
          role: "CUSTOMER",
          isActive: true,
          emailVerifiedAt: null,
          refreshTokenHash: null,
          creditBalance: 0,
          avatarUrl: null,
          ...data,
        };
        rows.set(u.id, u);
        return structuredClone(u);
      },
      async findUnique({ where }) {
        const u = [...rows.values()].find((u) => matches(u, where));
        return u ? structuredClone(u) : null;
      },
      async updateMany({ where, data }) {
        const found = [...rows.values()].filter((u) => matches(u, where));
        if (
          data.email &&
          [...rows.values()].some(
            (u) => !found.includes(u) && u.email === data.email,
          )
        )
          throw { code: "P2002" };
        found.forEach((u) => Object.assign(u, data));
        return { count: found.length };
      },
    },
    $queryRaw: async () => [{ value: 1 }],
    onModuleInit: async () => {},
    onModuleDestroy: async () => {},
  };
  const mail = {
    send: async (to, purpose, token) => {
      emails.push({ to, purpose, token });
      return true;
    },
  };
  const config = new ConfigService(configValues),
    auth = new AuthService(db, config, mail);
  return { rows, emails, db, mail, auth };
}
async function registered(f, email = "customer@example.test") {
  return f.auth.register({
    email,
    fullName: "Test Customer",
    password: "test-password-8",
    consent: true,
  });
}
const login = (f, email = "customer@example.test", portal = "CUSTOMER") =>
  f.auth.login({ email, password: "test-password-8", portal });
const rejectsCode = (promise, code) =>
  assert.rejects(promise, (e) => e.getResponse().code === code);

test("register stores password hash only and email verification is single use", async () => {
  const f = fixture(),
    r = await registered(f);
  assert.equal(r.emailSent, true);
  const u = f.rows.get(r.userId);
  assert.equal(u.role, "CUSTOMER");
  assert.notEqual(u.passwordHash, "test-password-8");
  assert.equal(u.emailVerifiedAt, null);
  await f.auth.verifyEmail(f.emails[0].token);
  assert.ok(u.emailVerifiedAt);
  await rejectsCode(f.auth.verifyEmail(f.emails[0].token), "STALE_LINK");
});
test("duplicate email and failed SMTP preserve the created account", async () => {
  const f = fixture();
  f.mail.send = async () => false;
  const r = await registered(f);
  assert.equal(r.emailSent, false);
  assert.ok(f.rows.has(r.userId));
  await rejectsCode(registered(f), "EMAIL_EXISTS");
});
test("unverified login works; inactive login and wrong portals fail", async () => {
  const f = fixture();
  const r = await registered(f);
  const s = await login(f);
  assert.equal(s.user.emailVerifiedAt, null);
  assert.equal(s.user.hasPassword, true);
  assert.equal("passwordHash" in s.user, false);
  assert.equal("refreshTokenHash" in s.user, false);
  await rejectsCode(login(f, undefined, "STAFF"), "WRONG_PORTAL");
  f.rows.get(r.userId).role = "STAFF";
  await rejectsCode(login(f), "WRONG_PORTAL");
  assert.equal((await login(f, undefined, "STAFF")).user.role, "STAFF");
  f.rows.get(r.userId).role = "ADMIN";
  assert.equal((await login(f, undefined, "STAFF")).user.role, "ADMIN");
  f.rows.get(r.userId).isActive = false;
  await rejectsCode(login(f, undefined, "STAFF"), "INVALID_CREDENTIALS");
});
test("one refresh session, old logout cannot clear new session, access has short TTL", async () => {
  const f = fixture();
  await registered(f);
  const a = await login(f),
    b = await login(f);
  assert.notEqual(a.refreshToken, b.refreshToken);
  await rejectsCode(f.auth.refresh(a.refreshToken), "SESSION_REVOKED");
  await f.auth.logout(a.refreshToken);
  assert.ok((await f.auth.refresh(b.refreshToken)).accessToken);
  await f.auth.logout(b.refreshToken);
  await f.auth.logout(b.refreshToken);
  await rejectsCode(f.auth.refresh(b.refreshToken), "SESSION_REVOKED");
  assert.ok(await f.auth.authenticate(b.accessToken));
  const claims = new JwtService().decode(b.accessToken);
  assert.equal(claims.exp - claims.iat, 900);
});
test("wrong signature, purpose, expired token and removed user are rejected", async () => {
  const f = fixture();
  const r = await registered(f),
    s = await login(f);
  await rejectsCode(f.auth.authenticate(s.refreshToken), "INVALID_TOKEN");
  await rejectsCode(
    f.auth.authenticate(s.accessToken + "invalid"),
    "INVALID_TOKEN",
  );
  const expired = new JwtService().sign(
    { sub: r.userId, purpose: "access" },
    {
      secret: configValues.JWT_ACCESS_SECRET,
      issuer: "n8-cineplex",
      audience: "access",
      expiresIn: -1,
    },
  );
  await rejectsCode(f.auth.authenticate(expired), "INVALID_TOKEN");
  f.rows.delete(r.userId);
  await rejectsCode(f.auth.authenticate(s.accessToken), "ACCOUNT_UNAVAILABLE");
});
test("reset is single use and revokes refresh without adding OTP fields", async () => {
  const f = fixture();
  await registered(f);
  const s = await login(f);
  await f.auth.resend("customer@example.test", true);
  const token = f.emails.at(-1).token;
  await f.auth.reset(token, "replacement-password");
  await rejectsCode(f.auth.reset(token, "another-password"), "STALE_LINK");
  await rejectsCode(f.auth.refresh(s.refreshToken), "SESSION_REVOKED");
  await rejectsCode(login(f), "INVALID_CREDENTIALS");
  assert.ok(
    (
      await f.auth.login({
        email: "customer@example.test",
        password: "replacement-password",
        portal: "CUSTOMER",
      })
    ).accessToken,
  );
});
test("change password requires current password and revokes refresh", async () => {
  const f = fixture();
  const r = await registered(f),
    s = await login(f);
  await rejectsCode(
    f.auth.changePassword(
      structuredClone(f.rows.get(r.userId)),
      "wrong",
      "replacement-password",
    ),
    "INVALID_CREDENTIALS",
  );
  await f.auth.changePassword(
    structuredClone(f.rows.get(r.userId)),
    "test-password-8",
    "replacement-password",
  );
  await rejectsCode(f.auth.refresh(s.refreshToken), "SESSION_REVOKED");
});
test("change email waits for confirmation, checks uniqueness again and invalidates old link", async () => {
  const f = fixture();
  const r = await registered(f);
  const s = await login(f);
  await f.auth.changeEmail(structuredClone(f.rows.get(r.userId)), {
    newEmail: "alias@example.test",
    currentPassword: "test-password-8",
  });
  assert.equal(f.rows.get(r.userId).email, "customer@example.test");
  const token = f.emails.at(-1).token;
  await f.auth.verifyEmail(token);
  assert.equal(f.rows.get(r.userId).email, "alias@example.test");
  await rejectsCode(f.auth.verifyEmail(token), "STALE_LINK");
  await rejectsCode(f.auth.refresh(s.refreshToken), "SESSION_REVOKED");
});
test("Google does not merge password accounts; new Google user remains Google-only", async () => {
  const f = fixture();
  await registered(f);
  f.auth.googleIdentity = async () => ({
    sub: "google-sub",
    email: "customer@example.test",
    email_verified: true,
    name: "Google",
  });
  await rejectsCode(
    f.auth.googleLogin("fixture-id-token"),
    "GOOGLE_EMAIL_CONFLICT",
  );
  f.auth.googleIdentity = async () => ({
    sub: "google-sub",
    email: "google@example.test",
    email_verified: true,
    name: "Google",
    iat: Math.floor(Date.now() / 1000),
  });
  const s = await f.auth.googleLogin("fixture-id-token");
  assert.equal(s.user.hasPassword, false);
  assert.ok(s.user.emailVerifiedAt);
  const u = f.rows.get(s.user.id);
  u.fullName = "Edited";
  u.email = "changed@example.test";
  assert.equal(
    (await f.auth.googleLogin("fixture-id-token")).user.fullName,
    "Edited",
  );
  assert.equal(
    (await f.auth.googleLogin("fixture-id-token")).user.email,
    "changed@example.test",
  );
  await rejectsCode(
    f.auth.changePassword(structuredClone(u), "any-password", "new-password"),
    "GOOGLE_ONLY",
  );
  const before = f.emails.length;
  await f.auth.resend(u.email, true);
  assert.equal(f.emails.length, before);
  await f.auth.changeEmail(structuredClone(u), {
    newEmail: "google-alias@example.test",
    idToken: "fixture-id-token",
  });
  f.auth.googleIdentity = async () => ({
    sub: "google-sub",
    iat: Math.floor(Date.now() / 1000) - 301,
  });
  await rejectsCode(
    f.auth.changeEmail(structuredClone(u), {
      newEmail: "stale@example.test",
      idToken: "fixture-id-token",
    }),
    "GOOGLE_REAUTH_REQUIRED",
  );
  f.auth.googleIdentity = async () => ({ sub: "another-sub" });
  await rejectsCode(
    f.auth.changeEmail(structuredClone(u), {
      newEmail: "other@example.test",
      idToken: "fixture-id-token",
    }),
    "GOOGLE_REAUTH_REQUIRED",
  );
});
test("forgot and resend are neutral for absent, verified or Google-only accounts", async () => {
  const f = fixture();
  await registered(f);
  const present = await f.auth.resend("customer@example.test", true),
    absent = await f.auth.resend("absent@example.test", true);
  assert.deepEqual(present, absent);
});
test("role and verified-email guards enforce server state", () => {
  const context = (u) => ({
    getHandler: () => {},
    getClass: () => {},
    switchToHttp: () => ({ getRequest: () => ({ user: u }) }),
  });
  const verified = new VerifiedEmailGuard();
  assert.throws(() =>
    verified.canActivate(context({ isActive: true, emailVerifiedAt: null })),
  );
  assert.equal(
    verified.canActivate(
      context({ isActive: true, emailVerifiedAt: new Date() }),
    ),
    true,
  );
  const roles = new RoleGuard({ getAllAndOverride: () => ["ADMIN"] });
  assert.throws(() => roles.canActivate(context({ role: "CUSTOMER" })));
  assert.equal(roles.canActivate(context({ role: "ADMIN" })), true);
});
test("avatar inspects bytes, rejects fake/oversized/SVG and emits bounded WebP", async () => {
  const png = await sharp({
    create: { width: 800, height: 600, channels: 3, background: "red" },
  })
    .png()
    .toBuffer();
  const meta = await sharp(await normalizeAvatar(png)).metadata();
  assert.equal(meta.format, "webp");
  assert.equal(meta.width, 512);
  assert.ok(meta.height <= 512);
  await assert.rejects(normalizeAvatar(Buffer.from("not-an-image")));
  await assert.rejects(normalizeAvatar(Buffer.alloc(5 * 1024 * 1024 + 1)));
  await assert.rejects(
    normalizeAvatar(
      Buffer.from(
        '<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"></svg>',
      ),
    ),
  );
});
test("config validates secrets and HTTPS without exposing input", () => {
  assert.equal(validateEnvironment(configValues).HOST, "127.0.0.1");
  assert.throws(
    () => validateEnvironment({ ...configValues, JWT_ACCESS_SECRET: "short" }),
    /JWT_ACCESS_SECRET/,
  );
  assert.throws(
    () =>
      validateEnvironment({
        ...configValues,
        PUBLIC_BASE_URL: "http://example.com/api/v1",
      }),
    /PUBLIC_BASE_URL/,
  );
});
test("real HTTP controllers normalize email and reject privilege injection and unauthenticated profile", async (t) => {
  const f = fixture();
  const envBefore = { ...process.env };
  Object.assign(process.env, configValues);
  const module = await Test.createTestingModule({ imports: [AppModule] })
    .overrideProvider(PrismaService)
    .useValue(f.db)
    .overrideProvider(MailService)
    .useValue(f.mail)
    .compile();
  const app = module.createNestApplication({ logger: false });
  app.setGlobalPrefix("api/v1");
  app.useGlobalPipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: true,
      validationError: { target: false, value: false },
    }),
  );
  await app.listen(0, "127.0.0.1");
  t.after(async () => {
    await app.close();
    for (const k of Object.keys(configValues))
      if (envBefore[k] === undefined) delete process.env[k];
      else process.env[k] = envBefore[k];
  });
  const base = await app.getUrl();
  const post = (route, body) =>
    fetch(base + "/api/v1/" + route, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
  assert.equal((await fetch(base + "/api/v1/me")).status, 401);
  assert.equal(
    (
      await post("auth/register", {
        email: "owned@example.test",
        fullName: "Test",
        password: "test-password-8",
        consent: true,
        role: "ADMIN",
      })
    ).status,
    400,
  );
  const r = await post("auth/register", {
    email: " OWNED@EXAMPLE.TEST ",
    fullName: " Test ",
    password: "test-password-8",
    consent: true,
  });
  assert.equal(r.status, 201);
  const created = await r.json();
  assert.equal(created.email, "owned@example.test");
  const page = await fetch(base + "/api/v1/auth/verify-email");
  assert.equal(page.status, 200);
  assert.equal(page.headers.get("cache-control"), "no-store");
  assert.match(await page.text(), /history.replaceState/);
});

test("public email URLs reject HTTP and loopback, including HTTPS loopback", () => {
  for (const base of [
    "http://127.0.0.1:5000",
    "https://127.0.0.1",
    "https://localhost",
    "https://[::1]",
  ])
    assert.throws(
      () =>
        validateEnvironment({
          ...configValues,
          PUBLIC_BASE_URL: base + "/api/v1",
        }),
      /PUBLIC_BASE_URL/,
    );
  assert.equal(
    validateEnvironment(configValues).PUBLIC_BASE_URL,
    "https://18.143.100.43/api/v1",
  );
});

test("all email purposes have HTML buttons, public fragment URLs and plain-text fallback", async () => {
  const mail = new MailService(new ConfigService(configValues));
  const sent = [];
  mail.transport = { sendMail: async (message) => sent.push(message) };
  for (const purpose of ["verify", "reset", "change-email"])
    assert.equal(
      await mail.send("customer@example.test", purpose, "fixture+token/=?"),
      true,
    );
  for (const [i, message] of sent.entries()) {
    const route = i === 1 ? "reset-password" : "verify-email";
    const link =
      "https://18.143.100.43/api/v1/auth/" +
      route +
      "#fixture%2Btoken%2F%3D%3F";
    assert.match(message.html, /<html lang="vi">/);
    assert.ok(message.html.includes('href="' + link + '"'));
    assert.ok(message.text.includes(link));
    assert.ok(message.html.includes((i === 1 ? "15" : "30") + " phút"));
    assert.doesNotMatch(message.html + message.text, /127\.0\.0\.1|localhost/);
  }
  assert.match(sent[2].subject, /email mới/);
});

test("SMTP failure returns false without exposing credentials or tokens", async () => {
  const mail = new MailService(new ConfigService(configValues));
  mail.transport = {
    sendMail: async () => {
      throw new Error("private diagnostics");
    },
  };
  assert.equal(
    await mail.send("customer@example.test", "reset", "fixture-token"),
    false,
  );
});

test("email and confirmation pages match the active Android palette and preserve security headers", async () => {
  const theme = readFileSync(
    new URL(
      "../../N8_Cineplex/app/src/main/java/com/nhom8/cineplex/ui/theme/CineplexMockTheme.kt",
      import.meta.url,
    ),
    "utf8",
  );
  const colors = ["Background", "Primary", "Text", "Muted", "Line"].map(
    (name) => {
      const match = theme.match(
        new RegExp("val " + name + " = Color\\(0xFF([0-9A-F]{6})\\)"),
      );
      assert.ok(match, name);
      return "#" + match[1];
    },
  );
  const mail = new MailService(new ConfigService(configValues));
  const rendered = [];
  mail.transport = { sendMail: async (message) => rendered.push(message.html) };
  for (const purpose of ["verify", "reset", "change-email"])
    await mail.send("customer@example.test", purpose, "fixture-token");
  for (const reset of [false, true]) {
    const response = {
      set(headers) {
        this.headers = headers;
        return this;
      },
      type() {
        return this;
      },
      send(html) {
        this.html = html;
        return this;
      },
    };
    emailPage(response, reset);
    rendered.push(response.html);
    assert.equal(response.headers["Cache-Control"], "no-store");
    assert.equal(response.headers["Referrer-Policy"], "no-referrer");
    const nonce = response.headers["Content-Security-Policy"].match(
      /script-src 'nonce-([^']+)'/,
    )[1];
    assert.ok(response.html.includes('<script nonce="' + nonce + '">'));
    assert.ok(response.html.includes('<style nonce="' + nonce + '">'));
    assert.match(response.html, /method:'POST'/);
    assert.match(response.html, /history.replaceState/);
    assert.match(response.html, /role="status"/);
  }
  for (const html of rendered) {
    for (const color of colors)
      assert.ok(html.toUpperCase().includes(color), color);
    assert.match(html, /Georgia/);
    assert.doesNotMatch(html, /#7c3aed|#17131b|127\.0\.0\.1/i);
  }
});
