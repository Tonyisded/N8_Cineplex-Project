import {
  Body,
  Controller,
  Get,
  HttpCode,
  Param,
  ParseUUIDPipe,
  Post,
  Query,
  Req,
  UseGuards,
} from "@nestjs/common";
import {
  ApiAcceptedResponse,
  ApiBearerAuth,
  ApiOkResponse,
  ApiOperation,
  ApiProperty,
  ApiQuery,
  ApiTags,
} from "@nestjs/swagger";
import type { SchemaObject } from "@nestjs/swagger";
import { IsIn, IsString, IsUUID, MaxLength, MinLength } from "class-validator";
import {
  JwtGuard,
  RoleGuard,
  Roles,
  VerifiedEmailGuard,
  type AuthRequest,
} from "../auth/guards.js";
import { PaymentsService } from "./payments.service.js";
const uuid: SchemaObject = { type: "string", format: "uuid" };
const money: SchemaObject = {
  type: "string",
  pattern: "^[0-9]+$",
  description: "Integer VND serialized as a decimal string",
};
const timestamp: SchemaObject = { type: "string", format: "date-time" };
const optionalText: SchemaObject = { type: "string", nullable: true };
const paymentSchema: SchemaObject = {
  type: "object",
  required: ["orderId", "status", "operationState", "amount", "expiresAt"],
  properties: {
    id: uuid,
    orderId: uuid,
    status: {
      type: "string",
      enum: ["PENDING", "PAID", "EXPIRED", "NEEDS_ADMIN", "RESOLVED"],
    },
    operationState: {
      type: "string",
      enum: [
        "REQUESTED",
        "PROCESSING",
        "UNKNOWN",
        "SUCCEEDED",
        "EXPIRED",
        "NEEDS_REVIEW",
        "FAILED",
      ],
    },
    amount: money,
    expiresAt: timestamp,
    ticketId: { ...uuid, nullable: true },
    paymentLinkId: optionalText,
    checkoutUrl: { ...optionalText, format: "uri" },
    qrCode: optionalText,
    description: optionalText,
    accountName: optionalText,
    accountNumber: optionalText,
    bin: optionalText,
  },
};
const refundSchema: SchemaObject = {
  type: "object",
  required: [
    "id",
    "status",
    "operationState",
    "amount",
    "accountMasked",
    "reason",
    "paidAt",
  ],
  properties: {
    id: uuid,
    status: { type: "string", enum: ["PENDING", "NEEDS_ADMIN", "RESOLVED"] },
    operationState: {
      type: "string",
      enum: [
        "REQUESTED",
        "PROCESSING",
        "UNKNOWN",
        "SUCCEEDED",
        "NEEDS_REVIEW",
        "FAILED",
      ],
    },
    amount: money,
    accountMasked: optionalText,
    reason: optionalText,
    paidAt: { ...timestamp, nullable: true },
  },
};
const snapshotSchema: SchemaObject = {
  type: "object",
  properties: {
    movieTitle: { type: "string" },
    posterUrl: { ...optionalText, format: "uri" },
    startAt: timestamp,
    format: { type: "string" },
    auditorium: { type: "string" },
    seatCodes: { type: "array", items: { type: "string" } },
    seats: {
      type: "array",
      items: {
        type: "object",
        properties: { seatCode: { type: "string" }, price: money },
      },
    },
    comboName: optionalText,
    comboQuantity: { type: "integer", minimum: 0, maximum: 9 },
    comboUnitPrice: money,
    subtotal: money,
    discount: money,
    totalAmount: money,
    voucherCode: optionalText,
  },
};
const ticketSchema: SchemaObject = {
  type: "object",
  required: [
    "id",
    "orderId",
    "status",
    "bookingCode",
    "issuedAt",
    "expiresAt",
    "snapshot",
    "refundEligible",
    "refundReason",
  ],
  properties: {
    id: uuid,
    orderId: uuid,
    status: {
      type: "string",
      enum: [
        "VALID",
        "USED",
        "REFUND_PENDING",
        "REFUNDED",
        "CANCELLED",
        "EXPIRED",
      ],
    },
    bookingCode: { type: "string" },
    issuedAt: timestamp,
    expiresAt: timestamp,
    snapshot: snapshotSchema,
    refundEligible: { type: "boolean" },
    refundReason: optionalText,
    accountMasked: optionalText,
    refund: { ...refundSchema, nullable: true },
  },
};
const itemEnvelope = (item: SchemaObject): SchemaObject => ({
  type: "object",
  required: ["serverTime", "item"],
  properties: { serverTime: timestamp, item },
});
const orderSchema: SchemaObject = {
  type: "object",
  properties: {
    id: uuid,
    showtimeId: uuid,
    status: { type: "string" },
    totalAmount: money,
    bankAmount: money,
    expiresAt: timestamp,
    seats: {
      type: "array",
      items: {
        type: "object",
        properties: { id: uuid, seatCode: { type: "string" }, price: money },
      },
    },
    payment: { ...paymentSchema, nullable: true },
    ticket: {
      type: "object",
      nullable: true,
      properties: { id: uuid, status: { type: "string" } },
    },
  },
};
class ResolveDto {
  @ApiProperty({
    enum: ["MARK_BANK_REFUNDED", "MARK_RESOLVED"],
    description:
      "Never manually finalizes an order; uncertain payouts remain blocked",
  })
  @IsIn(["MARK_BANK_REFUNDED", "MARK_RESOLVED"])
  action!: string;
  @ApiProperty({
    minLength: 8,
    maxLength: 1000,
    description:
      "Bank reference and verified handling note; no credentials or unmasked account data",
  })
  @IsString()
  @MinLength(8)
  @MaxLength(1000)
  note!: string;
}
class ResumeFundsDto {
  @ApiProperty({ format: "uuid" })
  @IsUUID()
  requestId!: string;
  @ApiProperty({ enum: ["FUNDS_ADDED"] })
  @IsIn(["FUNDS_ADDED"])
  reason!: string;
  @ApiProperty({ minLength: 8, maxLength: 1000 })
  @IsString()
  @MinLength(8)
  @MaxLength(1000)
  note!: string;
}
@ApiTags("Payments")
@ApiBearerAuth()
@UseGuards(JwtGuard, RoleGuard, VerifiedEmailGuard)
@Roles("CUSTOMER")
@Controller("orders")
export class OrderPaymentsController {
  constructor(private readonly payments: PaymentsService) {}
  @Get()
  @ApiOperation({ summary: "Your orders and payment/ticket state" })
  @ApiOkResponse({
    schema: {
      type: "object",
      required: ["serverTime", "items"],
      properties: {
        serverTime: timestamp,
        items: { type: "array", items: orderSchema },
      },
    },
  })
  list(@Req() req: AuthRequest) {
    return this.payments.orders(req.user.id);
  }
  @Post(":id/payment")
  @HttpCode(200)
  @ApiOkResponse({ schema: itemEnvelope(paymentSchema) })
  @ApiOperation({
    summary:
      "Start/reuse one payment on confirmed checkout; never trusts client amount",
  })
  start(@Param("id", new ParseUUIDPipe()) id: string, @Req() req: AuthRequest) {
    return this.payments.start(id, req.user.id);
  }
  @Get(":id/payment")
  @ApiOperation({
    summary:
      "Read persisted payment state only; never call provider/create/refund",
  })
  @ApiOkResponse({
    schema: { type: "object", properties: { item: paymentSchema } },
  })
  status(@Param("id", ParseUUIDPipe) id: string, @Req() req: AuthRequest) {
    return this.payments.paymentStatus(id, req.user.id);
  }
  @Post(":id/payment/check")
  @HttpCode(200)
  @ApiOkResponse({ schema: itemEnvelope(paymentSchema) })
  @ApiOperation({
    summary:
      "Query the existing provider operation, never creates a new payment",
  })
  check(@Param("id", new ParseUUIDPipe()) id: string, @Req() req: AuthRequest) {
    return this.payments.check(id, req.user.id);
  }
}
@ApiTags("Tickets")
@ApiBearerAuth()
@UseGuards(JwtGuard, RoleGuard, VerifiedEmailGuard)
@Roles("CUSTOMER")
@Controller("tickets")
export class TicketsController {
  constructor(private readonly payments: PaymentsService) {}
  @Get()
  @ApiQuery({
    name: "cursor",
    required: false,
    type: String,
    format: "uuid",
    description:
      "Previous nextCursor; stable issuedAt/id descending page, 20 tickets",
  })
  @ApiOkResponse({
    schema: {
      type: "object",
      required: ["serverTime", "items", "nextCursor"],
      properties: {
        serverTime: timestamp,
        items: { type: "array", items: ticketSchema },
        nextCursor: { ...uuid, nullable: true },
      },
    },
  })
  list(@Req() req: AuthRequest, @Query("cursor") cursor?: string) {
    return this.payments.tickets(req.user.id, cursor);
  }
  @Get(":id")
  @ApiOkResponse({ schema: itemEnvelope(ticketSchema) })
  get(@Param("id", new ParseUUIDPipe()) id: string, @Req() req: AuthRequest) {
    return this.payments.ticket(id, req.user.id);
  }
  @Get(":id/qr")
  @ApiOkResponse({
    schema: itemEnvelope({
      type: "object",
      required: ["payload"],
      properties: {
        payload: {
          type: "string",
          description:
            "Opaque N8T1 token; VALID ticket only, not a bank payment QR",
        },
      },
    }),
  })
  qr(@Param("id", new ParseUUIDPipe()) id: string, @Req() req: AuthRequest) {
    return this.payments.qr(id, req.user.id);
  }
  @Post(":id/refund")
  @HttpCode(202)
  @ApiAcceptedResponse({
    description:
      "Refund recorded, not necessarily bank-settled; operationState SUCCEEDED alone means completion",
    schema: itemEnvelope(refundSchema),
  })
  @ApiOperation({
    summary:
      "Cancel usable ticket and record full refund obligation; 202 is not bank success",
  })
  refund(
    @Param("id", new ParseUUIDPipe()) id: string,
    @Req() req: AuthRequest,
  ) {
    return this.payments.refund(id, req.user.id);
  }
  @Get(":id/refund")
  @ApiOkResponse({ schema: itemEnvelope(refundSchema) })
  refundState(
    @Param("id", new ParseUUIDPipe()) id: string,
    @Req() req: AuthRequest,
  ) {
    return this.payments.refundState(id, req.user.id);
  }
}
@ApiTags("payOS Webhook")
@Controller()
export class PayosWebhookController {
  constructor(private readonly payments: PaymentsService) {}
  @Post("webhooks/payos") @HttpCode(200) webhook(@Body() body: unknown) {
    return this.payments.webhook(body);
  }
  @Get("payments/return") returned() {
    return {
      message:
        "Quay lại ứng dụng và kiểm tra trạng thái đơn. Trang này không xác nhận đã trả tiền.",
    };
  }
  @Get("payments/cancel") cancelled() {
    return {
      message:
        "Quay lại ứng dụng để kiểm tra hoặc hủy giữ ghế. Trang này không xác nhận hoàn tiền.",
    };
  }
}
@ApiTags("Payment issues")
@ApiBearerAuth()
@UseGuards(JwtGuard, RoleGuard)
@Roles("ADMIN")
@Controller("admin/payment-issues")
export class PaymentIssuesController {
  constructor(private readonly payments: PaymentsService) {}
  @Get()
  @ApiOkResponse({
    schema: {
      type: "object",
      required: ["items"],
      properties: {
        items: {
          type: "array",
          items: {
            ...refundSchema,
            properties: {
              ...refundSchema.properties,
              orderId: { ...uuid, nullable: true },
              kind: { type: "string", enum: ["RECEIPT", "REFUND"] },
              providerTransactionId: optionalText,
              providerObjectId: optionalText,
            },
          },
        },
      },
    },
  })
  list() {
    return this.payments.issues();
  }
  @Post(":id/resume")
  @HttpCode(202)
  @ApiAcceptedResponse({ schema: itemEnvelope(refundSchema) })
  resume(
    @Param("id", new ParseUUIDPipe()) id: string,
    @Body() body: ResumeFundsDto,
    @Req() req: AuthRequest,
  ) {
    return this.payments.resumeFunds(
      id,
      req.user.id,
      body.requestId,
      body.note,
    );
  }
  @Post(":id/resolve")
  @HttpCode(200)
  @ApiOkResponse({ schema: itemEnvelope(refundSchema) })
  resolve(
    @Param("id", new ParseUUIDPipe()) id: string,
    @Body() body: ResolveDto,
    @Req() req: AuthRequest,
  ) {
    return this.payments.resolve(id, req.user.id, body.action, body.note);
  }
}
