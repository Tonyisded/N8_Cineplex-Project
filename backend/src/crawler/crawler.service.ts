import { Injectable } from "@nestjs/common";
import { chromium } from "playwright";

export const CGV_LANDMARK_URL =
  "https://www.cgv.vn/default/cinox/site/cgv-vincom-landmark-81/";

export type CrawlerProbeResult = {
  sourceUrl: string;
  finalUrl: string;
  status: number;
  title: string;
  theaterName: string;
  address: string;
  checkedAt: string;
};

@Injectable()
export class CrawlerService {
  async probe(): Promise<CrawlerProbeResult> {
    const browser = await chromium.launch({ headless: true, timeout: 15_000 });
    try {
      const context = await browser.newContext({
        locale: "vi-VN",
        serviceWorkers: "block",
        acceptDownloads: false,
      });
      context.setDefaultTimeout(15_000);
      // Fixed target, same-host resources only; refuse redirects before following them.
      await context.route("**/*", async (route) => {
        const url = new URL(route.request().url());
        if (
          url.protocol !== "https:" ||
          url.hostname !== "www.cgv.vn" ||
          (url.port !== "" && url.port !== "443") ||
          url.username ||
          url.password ||
          ["image", "media", "font"].includes(route.request().resourceType())
        ) {
          await route.abort();
          return;
        }
        try {
          const response = await route.fetch({
            maxRedirects: 0,
            maxRetries: 0,
            timeout: 30_000,
          });
          if (response.status() >= 300 && response.status() < 400) {
            await route.abort();
          } else {
            await route.fulfill({ response });
          }
          await response.dispose();
        } catch {
          await route.abort();
        }
      });
      const page = await context.newPage();
      const response = await page.goto(CGV_LANDMARK_URL, {
        waitUntil: "domcontentloaded",
        timeout: 30_000,
      });
      if (!response || response.status() !== 200) {
        throw new Error(`CGV returned HTTP ${response?.status() ?? "unknown"}`);
      }
      if (page.url() !== CGV_LANDMARK_URL) {
        throw new Error("CGV navigated outside the fixed theater page");
      }
      const heading = page.locator(".theater-title h3");
      const location = page.locator(".theater-address");
      await Promise.all([
        heading.waitFor({ state: "visible" }),
        location.waitFor({ state: "visible" }),
      ]);
      const clean = (text: string) =>
        text.normalize("NFC").replace(/\s+/gu, " ").trim();
      const theaterName = clean(await heading.innerText());
      const address = clean(await location.innerText());
      if (
        theaterName !== "CGV Vincom Center Landmark 81" ||
        !address.includes("Landmark 81") ||
        !address.includes("772")
      ) {
        throw new Error(
          "CGV theater content is missing or does not match Landmark 81",
        );
      }
      return {
        sourceUrl: CGV_LANDMARK_URL,
        finalUrl: page.url(),
        status: response.status(),
        title: await page.title(),
        theaterName,
        address,
        checkedAt: new Date().toISOString(),
      };
    } finally {
      await browser.close();
    }
  }
}
