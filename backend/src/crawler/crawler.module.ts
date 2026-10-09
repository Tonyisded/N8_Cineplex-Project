import { Module } from "@nestjs/common";
import { CrawlerJob } from "./crawler.job.js";
import { CrawlerService } from "./crawler.service.js";

@Module({ providers: [CrawlerService, CrawlerJob] })
export class CrawlerModule {}
