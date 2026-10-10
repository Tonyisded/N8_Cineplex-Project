import {
  Body,
  Controller,
  Get,
  HttpCode,
  Post,
  Req,
  Res,
  UseGuards,
} from "@nestjs/common";
import { ApiBearerAuth, ApiTags } from "@nestjs/swagger";
import { Throttle } from "@nestjs/throttler";
import type { Response } from "express";
import { AuthService } from "./auth.service.js";
import {
  ChangeEmailDto,
  EmailDto,
  GoogleDto,
  LoginDto,
  PasswordDto,
  RefreshDto,
  RegisterDto,
  ResetDto,
  TokenDto,
} from "./dto.js";
import { JwtGuard, type AuthRequest } from "./guards.js";
import { emailPage } from "./email-page.js";

@ApiTags("Auth")
@Controller("auth")
@Throttle({ default: { limit: 10, ttl: 60000 } })
export class AuthController {
  constructor(private readonly auth: AuthService) {}
  @Post("register") @Throttle({ default: { limit: 3, ttl: 60000 } }) register(
    @Body() body: RegisterDto,
  ) {
    return this.auth.register(body);
  }
  @Post("login") @HttpCode(200) login(@Body() body: LoginDto) {
    return this.auth.login(body);
  }
  @Post("google") @HttpCode(200) google(@Body() body: GoogleDto) {
    return this.auth.googleLogin(body.idToken);
  }
  @Post("refresh") @HttpCode(200) refresh(@Body() body: RefreshDto) {
    return this.auth.refresh(body.refreshToken);
  }
  @Post("logout") @HttpCode(200) logout(@Body() body: RefreshDto) {
    return this.auth.logout(body.refreshToken);
  }
  @Post("verify-email") @HttpCode(200) verify(@Body() body: TokenDto) {
    return this.auth.verifyEmail(body.token);
  }
  @Post("resend-verification")
  @Throttle({ default: { limit: 3, ttl: 60000 } })
  resend(@Body() body: EmailDto) {
    return this.auth.resend(body.email);
  }
  @Post("forgot-password")
  @Throttle({ default: { limit: 3, ttl: 60000 } })
  forgot(@Body() body: EmailDto) {
    return this.auth.resend(body.email, true);
  }
  @Post("reset-password") @HttpCode(200) reset(@Body() body: ResetDto) {
    return this.auth.reset(body.token, body.newPassword);
  }
  @Post("change-password")
  @HttpCode(200)
  @UseGuards(JwtGuard)
  @ApiBearerAuth()
  change(@Req() req: AuthRequest, @Body() body: PasswordDto) {
    return this.auth.changePassword(
      req.user,
      body.currentPassword,
      body.newPassword,
    );
  }
  @Get("verify-email") page(@Res() res: Response) {
    emailPage(res, false);
  }
  @Get("reset-password") resetPage(@Res() res: Response) {
    emailPage(res, true);
  }
}
