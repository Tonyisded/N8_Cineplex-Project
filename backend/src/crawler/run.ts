import "reflect-metadata";
import { NestFactory } from "@nestjs/core";
import { AppModule } from "../app.module.js";
import { CrawlerImportService } from "./crawler-import.service.js";
import { businessDate, dateWindow } from "./normalize.js";

let app:
  Awaited<ReturnType<typeof NestFactory.createApplicationContext>> | undefined;
try {
  const date = process.argv[2];
  if (date !== undefined) {
    dateWindow(date);
    if (date < businessDate())
      throw new Error("Cannot crawl a past business date");
  }
  // CLI is one-shot, never a second enabled scheduler.
  process.env.CRAWLER_ENABLED = "false";
  app = await NestFactory.createApplicationContext(AppModule, {
    abortOnError: false,
  });
  const crawler = app.get(CrawlerImportService);
  const result =
    date === undefined ? await crawler.runUpcoming() : await crawler.run(date);
  process.stdout.write(`${JSON.stringify(result)}\n`);
  if (result.event.status !== "SUCCESS") process.exitCode = 2;
} catch (error) {
  process.stderr.write(
    `${error instanceof Error ? error.message : "Crawler failed"}\n`,
  );
  process.exitCode = 1;
} finally {
  await app?.close();
}
