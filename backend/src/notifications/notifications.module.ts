import { Module } from "@nestjs/common";
import { AuthModule } from "../auth/auth.module.js";
import { PrismaModule } from "../prisma.module.js";
import { NotificationsService } from "./notifications.service.js";
import { FcmService } from "./fcm.service.js";
import { FcmController } from "./fcm.controller.js";
@Module({
  imports: [AuthModule, PrismaModule],
  providers: [NotificationsService, FcmService],
  controllers: [FcmController],
})
export class NotificationsModule {}
