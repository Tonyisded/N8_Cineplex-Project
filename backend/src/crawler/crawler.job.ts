import { Injectable, Logger } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { Cron } from "@nestjs/schedule";
import { CrawlerImportService } from "./crawler-import.service.js";

@Injectable()
export class CrawlerJob {
  private readonly logger = new Logger(CrawlerJob.name);

  constructor(
    private readonly config: ConfigService,
    private readonly crawler: CrawlerImportService,
  ) {}

  @Cron("0 0 */6 * * *", {
    name: "crawl-cgv",
    timeZone: "Asia/Ho_Chi_Minh",
    waitForCompletion: true,
  })
  async run() {
    if (!this.config.getOrThrow<boolean>("CRAWLER_ENABLED")) return;
    // Same tick for both CGV sources; one source failing must not block the other.
    try {
      await this.crawler.runUpcoming();
    } catch (error) {
      this.logger.error(
        error instanceof Error ? error.message : "CGV crawler failed",
      );
    }
    try {
      await this.crawler.runComingSoon();
    } catch (error) {
      this.logger.error(
        `CGV coming-soon crawler failed: ${error instanceof Error ? error.message : "unknown error"}`,
      );
    }
  }
}
