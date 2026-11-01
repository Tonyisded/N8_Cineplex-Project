import { Controller, Get, Req, UseGuards } from "@nestjs/common";
import { ApiBearerAuth, ApiTags } from "@nestjs/swagger";
import {
  JwtGuard,
  RoleGuard,
  Roles,
  type AuthRequest,
} from "../auth/guards.js";
import { BookingService } from "./booking.service.js";
@ApiTags("Vouchers")
@ApiBearerAuth()
@UseGuards(JwtGuard, RoleGuard)
@Roles("CUSTOMER")
@Controller("vouchers")
export class VouchersController {
  constructor(private readonly booking: BookingService) {}
  @Get() list(@Req() req: AuthRequest) {
    return this.booking.vouchers(req.user.id);
  }
}
