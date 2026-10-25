import {
  BadRequestException,
  Body,
  Controller,
  Get,
  Headers,
  Param,
  ParseUUIDPipe,
  Post,
  Req,
  UseGuards,
} from "@nestjs/common";
import { isUUID } from "class-validator";
import {
  ApiBearerAuth,
  ApiConflictResponse,
  ApiHeader,
  ApiOperation,
  ApiTags,
} from "@nestjs/swagger";
import {
  JwtGuard,
  RoleGuard,
  Roles,
  VerifiedEmailGuard,
  type AuthRequest,
} from "../auth/guards.js";
import { BookingService } from "./booking.service.js";
import { CreateOrderDto } from "./dto.js";

@ApiTags("Orders")
@ApiBearerAuth()
@UseGuards(JwtGuard, RoleGuard)
@Roles("CUSTOMER")
@Controller("orders")
export class BookingController {
  constructor(private readonly booking: BookingService) {}
  @Post()
  @UseGuards(VerifiedEmailGuard)
  @ApiOperation({
    summary:
      "Hold a complete seat selection for 600 seconds; no payment/credit/combo in this release",
  })
  @ApiHeader({
    name: "Idempotency-Key",
    required: true,
    description: "UUID retained across retries; used as order ID",
  })
  @ApiConflictResponse({
    description:
      "SEAT_UNAVAILABLE, COUPLE_INCOMPLETE, SHOWTIME_UNAVAILABLE or IDEMPOTENCY_CONFLICT",
  })
  create(
    @Req() req: AuthRequest,
    @Headers("idempotency-key") key: string,
    @Body() body: CreateOrderDto,
  ) {
    if (typeof key !== "string" || !isUUID(key))
      throw new BadRequestException({
        code: "INVALID_IDEMPOTENCY_KEY",
        message: "Idempotency-Key phải là UUID.",
      });
    return this.booking.create(req.user.id, key.toLowerCase(), {
      ...body,
      showtime_id: body.showtime_id.toLowerCase(),
      seat_ids: body.seat_ids.map((id) => id.toLowerCase()),
    });
  }
  @Get(":id")
  @ApiOperation({ summary: "Read your hold, applying expiry if due" })
  get(@Req() req: AuthRequest, @Param("id", new ParseUUIDPipe()) id: string) {
    return this.booking.get(id, req.user.id);
  }
  @Post(":id/cancel")
  @ApiOperation({
    summary: "Cancel your pending hold; repeated cancellation is safe",
  })
  cancel(
    @Req() req: AuthRequest,
    @Param("id", new ParseUUIDPipe()) id: string,
  ) {
    return this.booking.cancel(id, req.user.id);
  }
}
