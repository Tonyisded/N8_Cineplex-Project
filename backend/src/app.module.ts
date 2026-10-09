import { Module } from "@nestjs/common";
import { ConfigModule } from "@nestjs/config";
import { ScheduleModule } from "@nestjs/schedule";
import { validateEnvironment } from "./config.js";
import { HealthController } from "./health.controller.js";
import { PrismaService } from "./prisma.service.js";
import { CrawlerModule } from "./crawler/crawler.module.js";

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, validate: validateEnvironment }),
    ScheduleModule.forRoot(),
    CrawlerModule,
  ],
  controllers: [HealthController],
  providers: [PrismaService],
})
export class AppModule {}
