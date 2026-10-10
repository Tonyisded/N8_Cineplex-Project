import { Module } from "@nestjs/common";
import { ConfigModule } from "@nestjs/config";
import { ScheduleModule } from "@nestjs/schedule";
import { validateEnvironment } from "./config.js";
import { HealthController } from "./health.controller.js";
import { PrismaModule } from "./prisma.module.js";
import { AuthModule } from "./auth/auth.module.js";
import { ThrottlerGuard, ThrottlerModule } from "@nestjs/throttler";
import { APP_GUARD } from "@nestjs/core";
import { CrawlerModule } from "./crawler/crawler.module.js";
import { CatalogModule } from "./catalog/catalog.module.js";

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, validate: validateEnvironment }),
    ScheduleModule.forRoot(),
    CrawlerModule,
    CatalogModule,
    PrismaModule,
    AuthModule,
    ThrottlerModule.forRoot([{ name: "default", ttl: 60000, limit: 120 }]),
  ],
  controllers: [HealthController],
  providers: [{ provide: APP_GUARD, useClass: ThrottlerGuard }],
})
export class AppModule {}
