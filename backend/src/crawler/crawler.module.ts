import { Module } from "@nestjs/common";
import { CrawlerImportService } from "./crawler-import.service.js";
import { CrawlerJob } from "./crawler.job.js";
import { CrawlerService } from "./crawler.service.js";

@Module({ providers: [CrawlerService, CrawlerImportService, CrawlerJob] })
export class CrawlerModule {}
