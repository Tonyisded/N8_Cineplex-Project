import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
  SetMetadata,
  UnauthorizedException,
} from "@nestjs/common";
import { Reflector } from "@nestjs/core";
import type { Request } from "express";
import type { User } from "../generated/prisma/client.js";
import { AuthService } from "./auth.service.js";
export type AuthRequest = Request & { user: User };
@Injectable()
export class JwtGuard implements CanActivate {
  constructor(private readonly auth: AuthService) {}
  async canActivate(context: ExecutionContext) {
    const req = context.switchToHttp().getRequest<AuthRequest>();
    const match = /^Bearer ([^\s]+)$/i.exec(req.headers.authorization ?? "");
    if (!match)
      throw new UnauthorizedException({
        code: "AUTH_REQUIRED",
        message: "Vui lòng đăng nhập.",
      });
    req.user = await this.auth.authenticate(match[1]);
    return true;
  }
}
export const Roles = (...roles: User["role"][]) => SetMetadata("roles", roles);
@Injectable()
export class RoleGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}
  canActivate(context: ExecutionContext) {
    const roles = this.reflector.getAllAndOverride<User["role"][]>("roles", [
      context.getHandler(),
      context.getClass(),
    ]);
    if (
      roles &&
      !roles.includes(
        context.switchToHttp().getRequest<AuthRequest>().user.role,
      )
    )
      throw new ForbiddenException({
        code: "ROLE_FORBIDDEN",
        message: "Không có quyền thực hiện.",
      });
    return true;
  }
}
@Injectable()
export class VerifiedEmailGuard implements CanActivate {
  canActivate(context: ExecutionContext) {
    const u = context.switchToHttp().getRequest<AuthRequest>().user;
    if (!u?.isActive || !u.emailVerifiedAt)
      throw new ForbiddenException({
        code: "EMAIL_NOT_VERIFIED",
        message: "Hãy xác minh email trước khi đặt vé.",
      });
    return true;
  }
}
