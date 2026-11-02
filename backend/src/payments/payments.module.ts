import { Module } from "@nestjs/common";
import { AuthModule } from "../auth/auth.module.js";
import { BookingModule } from "../booking/booking.module.js";
import { PayosAdapter } from "./payos.adapter.js";
import { PaymentsService } from "./payments.service.js";
import {
  OrderPaymentsController,
  TicketsController,
  PayosWebhookController,
  PaymentIssuesController,
} from "./payments.controller.js";
@Module({
  imports: [AuthModule, BookingModule],
  controllers: [
    OrderPaymentsController,
    TicketsController,
    PayosWebhookController,
    PaymentIssuesController,
  ],
  providers: [PayosAdapter, PaymentsService],
  exports: [PaymentsService],
})
export class PaymentsModule {}
