import "reflect-metadata";
import process from "node:process";
import { Logger, type INestApplication } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { NestFactory } from "@nestjs/core";
import { DocumentBuilder, SwaggerModule } from "@nestjs/swagger";
import { AppModule } from "./app.module.js";

let app: INestApplication | undefined;
try {
  app = await NestFactory.create(AppModule, { abortOnError: false });
  app.enableShutdownHooks();
  app.setGlobalPrefix("api/v1");
  const document = SwaggerModule.createDocument(
    app,
    new DocumentBuilder()
      .setTitle("N8 Cineplex API")
      .setVersion("0.1.0")
      .build(),
  );
  SwaggerModule.setup("api/docs", app, document, {
    jsonDocumentUrl: "api/docs-json",
  });
  const config = app.get(ConfigService);
  await app.listen(
    config.getOrThrow<number>("PORT"),
    config.getOrThrow<string>("HOST"),
  );
} catch (error) {
  if (app) await app.close();
  Logger.error(
    error instanceof Error ? error.message : "Application startup failed",
    undefined,
    "Bootstrap",
  );
  process.exitCode = 1;
}
