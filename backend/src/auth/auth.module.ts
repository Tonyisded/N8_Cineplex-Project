import { Module } from "@nestjs/common";
import { AuthService } from "./auth.service.js";
import { AuthController } from "./auth.controller.js";
import { AvatarController, ProfileController } from "./profile.controller.js";
import { AvatarService } from "./avatar.service.js";
import { MailService } from "./mail.service.js";
import { JwtGuard, RoleGuard, VerifiedEmailGuard } from "./guards.js";
@Module({
  controllers: [AuthController, ProfileController, AvatarController],
  providers: [
    AuthService,
    MailService,
    AvatarService,
    JwtGuard,
    RoleGuard,
    VerifiedEmailGuard,
  ],
  exports: [AuthService, JwtGuard, RoleGuard, VerifiedEmailGuard],
})
export class AuthModule {}
