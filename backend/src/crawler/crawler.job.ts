import { Injectable, Logger } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { Cron } from "@nestjs/schedule";
import { CrawlerService } from "./crawler.service.js";

@Injectable()
export class CrawlerJob {
  private readonly logger = new Logger(CrawlerJob.name);

  constructor(
    private readonly config: ConfigService,
    private readonly crawler: CrawlerService,
  ) {}

  @Cron("0 0 */6 * * *", {
    name: "crawl-cgv",
    timeZone: "Asia/Ho_Chi_Minh",
    waitForCompletion: true,
  })
  async run() {
    if (!this.config.getOrThrow<boolean>("CRAWLER_ENABLED")) return;
    try {
      this.logger.log(JSON.stringify(await this.crawler.probe()));
    } catch (error) {
      this.logger.error(
        error instanceof Error ? error.message : "CGV crawler failed",
      );
    }
  }
}
