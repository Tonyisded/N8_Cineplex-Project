import { Injectable } from "@nestjs/common";
import { chromium, type Page } from "playwright";

import {
  businessDate,
  dateWindow,
  nextBusinessDate,
  sourceMovieUrl,
  sourceSlot,
  normalizeFormat,
  clean,
  type CrawlSnapshot,
} from "./normalize.js";
import { parseMovie, type MovieDom } from "./parse-movie.js";

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
    return this.visit(async (_page, result) => result);
  }

  private async visit<T>(
    read: (page: Page, result: CrawlerProbeResult) => Promise<T>,
    date?: string,
  ): Promise<T> {
    const targetUrl = date
      ? `${CGV_LANDMARK_URL}?selecteddate=${date.replaceAll("-", "")}`
      : CGV_LANDMARK_URL;
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
      const response = await page.goto(targetUrl, {
        waitUntil: "domcontentloaded",
        timeout: 30_000,
      });
      if (!response || response.status() !== 200) {
        throw new Error(`CGV returned HTTP ${response?.status() ?? "unknown"}`);
      }
      if (page.url() !== targetUrl) {
        throw new Error("CGV navigated outside the fixed theater page");
      }
      const heading = page.locator(".theater-title h3");
      const location = page.locator(".theater-address");
      await Promise.all([
        heading.waitFor({ state: "visible" }),
        location.waitFor({ state: "visible" }),
      ]);
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
      return await read(page, {
        sourceUrl: CGV_LANDMARK_URL,
        finalUrl: page.url(),
        status: response.status(),
        title: await page.title(),
        theaterName,
        address,
        checkedAt: new Date().toISOString(),
      });
    } finally {
      await browser.close();
    }
  }

  async collect(date = businessDate()): Promise<CrawlSnapshot> {
    dateWindow(date);
    return this.visit(
      async (page) =>
        this.collectPage(page, date, await page.context().newPage(), new Map()),
      date,
    );
  }

  async collectUpcoming(
    today: string,
    consume: (
      date: string,
      collect: () => Promise<CrawlSnapshot>,
    ) => Promise<CrawlSnapshot>,
  ): Promise<{
    stopReason: "EMPTY_DAY" | "UNPUBLISHED_DATE";
    stopDate: string;
  }> {
    dateWindow(today);
    return this.visit(async (page) => {
      const ids = await page
        .locator('[id^="cgv"]')
        .evaluateAll((nodes) =>
          nodes.map((node) => node.id).filter((id) => /^cgv\d{8}$/.test(id)),
        );
      const dates = new Set(
        ids.map((id) => {
          const dy = id.slice(3);
          const date = `${dy.slice(0, 4)}-${dy.slice(4, 6)}-${dy.slice(6)}`;
          dateWindow(date);
          return date;
        }),
      );
      const details = await page.context().newPage();
      const cache = new Map<string, MovieDom>();
      const read = (date: string) =>
        consume(date, () => this.collectPage(page, date, details, cache));
      // Today may have no remaining slots; only a future empty day ends the range.
      if (dates.has(today)) await read(today);
      for (
        let date = nextBusinessDate(today);
        ;
        date = nextBusinessDate(date)
      ) {
        if (!dates.has(date))
          return { stopReason: "UNPUBLISHED_DATE", stopDate: date };
        if ((await read(date)).empty)
          return { stopReason: "EMPTY_DAY", stopDate: date };
      }
    }, today);
  }

  private async collectPage(
    page: Page,
    date: string,
    details: Page,
    cache: Map<string, MovieDom>,
  ): Promise<CrawlSnapshot> {
    const dy = date.replaceAll("-", "");
    const tab = page.locator(`#cgv${dy}`);
    if ((await tab.count()) !== 1)
      throw new Error("Requested date is not published by CGV");
    const targetUrl = `${CGV_LANDMARK_URL}?selecteddate=${dy}`;
    if (page.url() !== targetUrl) {
      // CGV's public GET calendar returns the same schedules as selecteddate in its date-tab POST.
      const response = await page.goto(targetUrl, {
        waitUntil: "domcontentloaded",
        timeout: 30000,
      });
      if (response?.status() !== 200)
        throw new Error(`CGV schedule HTTP ${response?.status() ?? "unknown"}`);
      if (page.url() !== targetUrl)
        throw new Error(
          "CGV schedule navigated outside the requested calendar URL",
        );
    }
    const theater = clean(await page.locator(".theater-title h3").innerText());
    const address = clean(await page.locator(".theater-address").innerText());
    if (
      theater !== "CGV Vincom Center Landmark 81" ||
      !address.includes("772") ||
      !address.includes("Landmark 81")
    )
      throw new Error(
        "CGV schedule content is missing or incorrect; not an empty day",
      );
    await page.waitForFunction((dy) => {
      const links = [
        ...document.querySelectorAll<HTMLAnchorElement>(".film-showtimes a"),
      ];
      const films = document.querySelectorAll(".film-list");
      const container = document.querySelector(".tabs-cgv-showtimes");
      return (
        (films.length > 0 &&
          links.length > 0 &&
          links.every((a) => a.href.includes(`/dy/${dy}`))) ||
        (films.length === 0 &&
          links.length === 0 &&
          (document.body.innerText.includes("No schedules available") ||
            (container?.children.length === 0 &&
              container.textContent?.trim() === "")))
      );
    }, dy);
    const rows = await page.locator(".film-list").evaluateAll((films) =>
      films.map((film) => {
        const a = film.querySelector<HTMLAnchorElement>(".film-label h3 a")!;
        return {
          url: a?.href ?? "",
          title: a?.textContent ?? "",
          age: film.querySelector(".film-label span")?.className ?? "",
          slots: [...film.querySelectorAll(".film-screen")].flatMap((screen) =>
            [
              ...(screen.nextElementSibling?.querySelectorAll<HTMLAnchorElement>(
                "a",
              ) ?? []),
            ].map((link) => ({
              href: link.href,
              time: link.textContent?.trim() ?? "",
              format: screen.textContent?.trim() ?? "",
              screenClass: screen.className,
            })),
          ),
        };
      }),
    );
    const snapshot: CrawlSnapshot = {
      businessDate: date,
      capturedAt: new Date().toISOString(),
      sourceMovies: rows.length,
      empty: rows.length === 0,
      sourceShowtimes: rows.reduce((n, r) => n + r.slots.length, 0),
      movies: [],
      showtimes: [],
      skipped: [],
    };
    const keys = new Set<string>();
    for (const row of rows) {
      let url = row.url;
      try {
        url = sourceMovieUrl(url);
        let raw = cache.get(url);
        if (!raw) {
          const response = await details.goto(url, {
            waitUntil: "domcontentloaded",
            timeout: 30000,
          });
          if (response?.status() !== 200 || details.url() !== url)
            throw new Error("CGV movie navigation failed");
          await details
            .locator(".product-view h1")
            .first()
            .waitFor({ state: "visible" });
          raw = await details.evaluate(() => ({
            title:
              document.querySelector(".product-view h1")?.textContent ?? "",
            rows: [...document.querySelectorAll(".movie-info")].map(
              (el) => el.textContent ?? "",
            ),
            synopsis:
              document.querySelector(".product-collateral .std")?.textContent ??
              "",
            poster:
              document
                .querySelector<HTMLImageElement>(".product-img-box img")
                ?.getAttribute("src") ?? "",
            age: [
              ...document.querySelectorAll(
                ".movie-rating, .movie-rating-detail",
              ),
            ]
              .map((el) => `${el.className} ${el.textContent}`)
              .join(" "),
          }));
          cache.set(url, raw);
        }
        const movie = parseMovie(url, raw, row.age);
        snapshot.movies.push(movie);
        for (const slot of row.slots) {
          try {
            const identity = sourceSlot(slot.href, clean(slot.time), date);
            const normalized = {
              ...identity,
              movieUrl: url,
              format: normalizeFormat(slot.format, slot.screenClass),
            };
            if (keys.has(identity.sourceKey))
              throw new Error("Duplicate source slot");
            keys.add(identity.sourceKey);
            snapshot.showtimes.push(normalized);
          } catch (error) {
            snapshot.skipped.push({
              source: slot.href,
              reason:
                error instanceof Error ? error.message : "Invalid showtime",
            });
          }
        }
      } catch (error) {
        snapshot.skipped.push({
          source: url,
          reason: error instanceof Error ? error.message : "Invalid movie",
        });
      }
    }
    return snapshot;
  }
}
