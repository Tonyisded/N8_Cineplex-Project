import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  ServiceUnavailableException,
  UnauthorizedException,
} from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { JwtService } from "@nestjs/jwt";
import { createHash, createHmac, randomUUID } from "node:crypto";
import argon2 from "argon2";
import { OAuth2Client } from "google-auth-library";
import { PrismaService } from "../prisma.service.js";
import type { User } from "../generated/prisma/client.js";
import { MailService } from "./mail.service.js";
import type { ChangeEmailDto, LoginDto, RegisterDto } from "./dto.js";

export const profile = (u: User) => ({
  id: u.id,
  email: u.email,
  fullName: u.fullName,
  avatarUrl: u.avatarUrl,
  role: u.role,
  emailVerifiedAt: u.emailVerifiedAt,
  hasPassword: u.passwordHash !== null,
});
const failure = (code: string, message: string) => ({ code, message });
const digest = (s: string) => createHash("sha256").update(s).digest("hex");
type Purpose = "access" | "refresh" | "verify" | "reset" | "change-email";
type Claims = {
  sub: string;
  purpose: Purpose;
  state?: string;
  newEmail?: string;
};
@Injectable()
export class AuthService {
  private readonly jwt = new JwtService();
  private readonly google = new OAuth2Client();
  private readonly dummyHash = argon2.hash(randomUUID());
  constructor(
    private readonly db: PrismaService,
    private readonly config: ConfigService,
    private readonly mail: MailService,
  ) {}
  private secret(p: Purpose) {
    return this.config.getOrThrow<string>(
      p === "access"
        ? "JWT_ACCESS_SECRET"
        : p === "refresh"
          ? "JWT_REFRESH_SECRET"
          : "JWT_EMAIL_SECRET",
    );
  }
  private sign(u: User, purpose: Purpose, extras: object = {}) {
    return this.jwt.sign(
      { sub: u.id, purpose, ...extras },
      {
        secret: this.secret(purpose),
        algorithm: "HS256",
        issuer: "n8-cineplex",
        audience: purpose,
        expiresIn:
          purpose === "access"
            ? 900
            : purpose === "refresh"
              ? 604800
              : purpose === "reset"
                ? 900
                : 1800,
        jwtid: randomUUID(),
      },
    );
  }
  private decode(token: string, purpose: Purpose): Claims {
    try {
      const c = this.jwt.verify<Claims>(token, {
        secret: this.secret(purpose),
        algorithms: ["HS256"],
        issuer: "n8-cineplex",
        audience: purpose,
      });
      if (
        c.purpose !== purpose ||
        !/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/i.test(c.sub)
      )
        throw new Error();
      return c;
    } catch {
      throw new UnauthorizedException(
        failure("INVALID_TOKEN", "Liên kết hoặc phiên không hợp lệ/hết hạn."),
      );
    }
  }
  private state(u: User) {
    return createHmac("sha256", this.secret("verify"))
      .update(
        JSON.stringify([
          u.id,
          u.email,
          u.passwordHash,
          u.googleId,
          u.emailVerifiedAt?.toISOString() ?? null,
        ]),
      )
      .digest("hex");
  }
  private unchanged(u: User) {
    return {
      id: u.id,
      email: u.email,
      passwordHash: u.passwordHash,
      googleId: u.googleId,
      emailVerifiedAt: u.emailVerifiedAt,
      isActive: true,
    };
  }
  private async active(id: string) {
    const u = await this.db.user.findUnique({ where: { id } });
    if (!u?.isActive)
      throw new UnauthorizedException(
        failure(
          "ACCOUNT_UNAVAILABLE",
          "Tài khoản hoặc phiên không còn hợp lệ.",
        ),
      );
    return u;
  }
  async authenticate(token: string) {
    return this.active(this.decode(token, "access").sub);
  }
  private async email(
    u: User,
    purpose: "verify" | "reset" | "change-email",
    newEmail?: string,
  ) {
    return this.mail.send(
      newEmail ?? u.email,
      purpose,
      this.sign(u, purpose, {
        state: this.state(u),
        ...(newEmail ? { newEmail } : {}),
      }),
    );
  }
  async register(input: RegisterDto) {
    let u: User;
    try {
      u = await this.db.user.create({
        data: {
          email: input.email,
          fullName: input.fullName,
          passwordHash: await argon2.hash(input.password),
          role: "CUSTOMER",
        },
      });
    } catch (e) {
      if ((e as { code?: string }).code === "P2002")
        throw new ConflictException(
          failure("EMAIL_EXISTS", "Email đã được đăng ký."),
        );
      throw e;
    }
    const sent = await this.email(u, "verify");
    return {
      userId: u.id,
      email: u.email,
      emailSent: sent,
      message: sent
        ? "Đăng ký thành công. Hãy kiểm tra email để xác minh."
        : "Đã tạo tài khoản nhưng chưa gửi được thư. Hãy dùng gửi lại xác minh.",
    };
  }
  private async session(u: User) {
    const refreshToken = this.sign(u, "refresh");
    const result = await this.db.user.updateMany({
      where: { ...this.unchanged(u), role: u.role },
      data: { refreshTokenHash: digest(refreshToken) },
    });
    if (!result.count)
      throw new UnauthorizedException(
        failure(
          "AUTH_STATE_CHANGED",
          "Tài khoản đã thay đổi. Vui lòng đăng nhập lại.",
        ),
      );
    return {
      accessToken: this.sign(u, "access"),
      refreshToken,
      expiresIn: 900,
      user: profile(u),
    };
  }
  async login(input: LoginDto) {
    const u = await this.db.user.findUnique({ where: { email: input.email } });
    const valid = await argon2
      .verify(u?.passwordHash ?? (await this.dummyHash), input.password)
      .catch(() => false);
    if (!u?.isActive || !u.passwordHash || !valid)
      throw new UnauthorizedException(
        failure("INVALID_CREDENTIALS", "Sai email hoặc mật khẩu."),
      );
    if ((input.portal === "CUSTOMER") !== (u.role === "CUSTOMER"))
      throw new ForbiddenException(
        failure(
          "WRONG_PORTAL",
          "Vui lòng dùng đúng cổng đăng nhập của tài khoản.",
        ),
      );
    return this.session(u);
  }
  private async googleIdentity(idToken: string) {
    const audience = this.config.get<string>("GOOGLE_CLIENT_ID");
    if (!audience)
      throw new ServiceUnavailableException(
        failure("GOOGLE_NOT_CONFIGURED", "Google Login chưa được cấu hình."),
      );
    try {
      const payload = (
        await this.google.verifyIdToken({ idToken, audience })
      ).getPayload();
      if (!payload?.sub || !payload.email || !payload.email_verified)
        throw new Error();
      return payload;
    } catch {
      throw new UnauthorizedException(
        failure(
          "INVALID_GOOGLE_TOKEN",
          "Không xác minh được tài khoản Google.",
        ),
      );
    }
  }
  async googleLogin(idToken: string) {
    const p = await this.googleIdentity(idToken);
    let u = await this.db.user.findUnique({ where: { googleId: p.sub } });
    if (!u) {
      const email = p.email!.trim().toLowerCase();
      if (await this.db.user.findUnique({ where: { email } }))
        throw new ConflictException(
          failure(
            "GOOGLE_EMAIL_CONFLICT",
            "Email đã có tài khoản. Hãy đăng nhập bằng email và mật khẩu; không ghép tài khoản.",
          ),
        );
      try {
        u = await this.db.user.create({
          data: {
            email,
            googleId: p.sub,
            fullName: (p.name || email).slice(0, 100),
            avatarUrl: p.picture?.startsWith("https://") ? p.picture : null,
            emailVerifiedAt: new Date(),
            role: "CUSTOMER",
          },
        });
      } catch (e) {
        if ((e as { code?: string }).code !== "P2002") throw e;
        u = await this.db.user.findUnique({ where: { googleId: p.sub } });
        if (!u)
          throw new ConflictException(
            failure(
              "GOOGLE_EMAIL_CONFLICT",
              "Email đã có tài khoản; không ghép tài khoản.",
            ),
          );
      }
    }
    if (!u.isActive || u.role !== "CUSTOMER")
      throw new ForbiddenException(
        failure(
          "GOOGLE_ACCOUNT_FORBIDDEN",
          "Tài khoản không dùng được cổng khách hàng.",
        ),
      );
    return this.session(u);
  }
  async refresh(token: string) {
    const u = await this.active(this.decode(token, "refresh").sub);
    if (u.refreshTokenHash !== digest(token))
      throw new UnauthorizedException(
        failure(
          "SESSION_REVOKED",
          "Phiên đã kết thúc. Vui lòng đăng nhập lại.",
        ),
      );
    return { accessToken: this.sign(u, "access"), expiresIn: 900 };
  }
  async logout(token: string) {
    const c = this.decode(token, "refresh");
    await this.db.user.updateMany({
      where: { id: c.sub, refreshTokenHash: digest(token) },
      data: { refreshTokenHash: null },
    });
    return { message: "Đã đăng xuất." };
  }
  async resend(email: string, reset = false) {
    const u = await this.db.user.findUnique({ where: { email } });
    if (u?.isActive && (reset ? !!u.passwordHash : !u.emailVerifiedAt))
      await this.email(u, reset ? "reset" : "verify");
    return {
      message:
        "Nếu tài khoản phù hợp, hệ thống sẽ gửi thư. Hãy kiểm tra hộp thư và thư rác.",
    };
  }
  private async emailUser(
    token: string,
    purpose: "verify" | "reset" | "change-email",
  ) {
    const c = this.decode(token, purpose),
      u = await this.active(c.sub);
    if (c.state !== this.state(u))
      throw new UnauthorizedException(
        failure(
          "STALE_LINK",
          "Liên kết đã được sử dụng hoặc tài khoản đã thay đổi.",
        ),
      );
    return { c, u };
  }
  async verifyEmail(token: string) {
    let purpose: "verify" | "change-email" = "verify";
    try {
      this.decode(token, "verify");
    } catch {
      this.decode(token, "change-email");
      purpose = "change-email";
    }
    const { c, u } = await this.emailUser(token, purpose);
    if (purpose === "verify" && u.emailVerifiedAt)
      throw new BadRequestException(
        failure("ALREADY_VERIFIED", "Email đã được xác minh."),
      );
    try {
      const result = await this.db.user.updateMany({
        where: this.unchanged(u),
        data: {
          emailVerifiedAt: new Date(),
          ...(purpose === "change-email"
            ? { email: c.newEmail!, refreshTokenHash: null }
            : {}),
        },
      });
      if (!result.count)
        throw new ConflictException(
          failure(
            "STALE_LINK",
            "Tài khoản đã thay đổi. Hãy yêu cầu liên kết mới.",
          ),
        );
    } catch (e) {
      if ((e as { code?: string }).code === "P2002")
        throw new ConflictException(
          failure("EMAIL_EXISTS", "Email mới đã có tài khoản."),
        );
      throw e;
    }
    return {
      message:
        purpose === "verify"
          ? "Email đã được xác minh. Bạn có thể quay lại ứng dụng."
          : "Email đã được đổi. Vui lòng đăng nhập lại.",
    };
  }
  async reset(token: string, password: string) {
    const { u } = await this.emailUser(token, "reset");
    if (!u.passwordHash)
      throw new ForbiddenException(
        failure(
          "GOOGLE_ONLY",
          "Tài khoản Google không dùng mật khẩu Cineplex.",
        ),
      );
    const r = await this.db.user.updateMany({
      where: this.unchanged(u),
      data: {
        passwordHash: await argon2.hash(password),
        refreshTokenHash: null,
      },
    });
    if (!r.count)
      throw new ConflictException(
        failure(
          "STALE_LINK",
          "Liên kết đã được dùng hoặc tài khoản đã thay đổi.",
        ),
      );
    return { message: "Đã đặt mật khẩu mới. Vui lòng đăng nhập lại." };
  }
  async changePassword(u: User, current: string, password: string) {
    if (!u.passwordHash)
      throw new ForbiddenException(
        failure(
          "GOOGLE_ONLY",
          "Mật khẩu tài khoản này được quản lý tại Google.",
        ),
      );
    if (!(await argon2.verify(u.passwordHash, current).catch(() => false)))
      throw new UnauthorizedException(
        failure("INVALID_CREDENTIALS", "Mật khẩu hiện tại không đúng."),
      );
    const r = await this.db.user.updateMany({
      where: this.unchanged(u),
      data: {
        passwordHash: await argon2.hash(password),
        refreshTokenHash: null,
      },
    });
    if (!r.count)
      throw new ConflictException(
        failure("AUTH_STATE_CHANGED", "Tài khoản đã thay đổi. Hãy thử lại."),
      );
    return { message: "Đã đổi mật khẩu. Vui lòng đăng nhập lại." };
  }
  async changeEmail(u: User, input: ChangeEmailDto) {
    if (u.passwordHash) {
      if (
        !input.currentPassword ||
        !(await argon2
          .verify(u.passwordHash, input.currentPassword)
          .catch(() => false))
      )
        throw new UnauthorizedException(
          failure("INVALID_CREDENTIALS", "Mật khẩu hiện tại không đúng."),
        );
    } else {
      const identity = input.idToken
        ? await this.googleIdentity(input.idToken)
        : null;
      const now = Math.floor(Date.now() / 1000);
      if (
        identity?.sub !== u.googleId ||
        !identity.iat ||
        identity.iat < now - 300 ||
        identity.iat > now + 60
      )
        throw new UnauthorizedException(
          failure(
            "GOOGLE_REAUTH_REQUIRED",
            "Hãy xác thực lại đúng tài khoản Google.",
          ),
        );
    }
    if (input.newEmail === u.email)
      throw new BadRequestException(
        failure("SAME_EMAIL", "Hãy nhập email mới."),
      );
    if (await this.db.user.findUnique({ where: { email: input.newEmail } }))
      throw new ConflictException(
        failure("EMAIL_EXISTS", "Email mới đã có tài khoản."),
      );
    if (!(await this.email(u, "change-email", input.newEmail)))
      throw new ServiceUnavailableException(
        failure(
          "EMAIL_SEND_FAILED",
          "Chưa gửi được thư. Email hiện tại chưa thay đổi; hãy thử lại.",
        ),
      );
    return {
      message:
        "Hãy xác nhận liên kết gửi đến email mới. Email hiện tại chưa thay đổi.",
    };
  }
}
