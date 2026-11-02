import { VouchersController } from "./vouchers.controller.js";
import { Module } from "@nestjs/common";
import { AuthModule } from "../auth/auth.module.js";
import { BookingController } from "./booking.controller.js";
import { BookingService } from "./booking.service.js";
import { BookingGateway } from "./booking.gateway.js";

@Module({
  imports: [AuthModule],
  controllers: [BookingController, VouchersController],
  providers: [BookingService, BookingGateway],
  exports: [BookingGateway],
})
export class BookingModule {}
