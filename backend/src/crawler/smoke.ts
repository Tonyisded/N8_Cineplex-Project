import "reflect-metadata";
import process from "node:process";
import { CrawlerService } from "./crawler.service.js";

try {
  console.log(JSON.stringify(await new CrawlerService().probe()));
} catch (error) {
  console.error(
    error instanceof Error ? error.message : "CGV smoke test failed",
  );
  process.exitCode = 1;
}
