import "reflect-metadata";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { createServer } from "node:http";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";
import test from "node:test";
import { Logger, Module } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { NestFactory } from "@nestjs/core";
import { ScheduleModule, SchedulerRegistry } from "@nestjs/schedule";
import { chromium } from "playwright";
import { validateEnvironment } from "../dist/config.js";
import { CrawlerJob } from "../dist/crawler/crawler.job.js";
import {
  CGV_LANDMARK_URL,
  CrawlerService,
} from "../dist/crawler/crawler.service.js";

const validHtml = `<!doctype html><html><head><title>Site</title></head><body>
  <div class="theater-title"><h3>CGV Vincom Center Landmark 81</h3></div>
  <div class="theater-address">Tầng B1, Landmark 81, 772 Điện Biên Phủ</div>
</body></html>`;
const databaseUrl = "postgresql://ci:ci@127.0.0.1:5432/n8_cineplex";
const authConfig = {
  JWT_ACCESS_SECRET: "a".repeat(40),
  JWT_REFRESH_SECRET: "b".repeat(40),
  JWT_EMAIL_SECRET: "c".repeat(40),
  GMAIL_USER: "sender@example.test",
  GMAIL_APP_PASSWORD: "t".repeat(16),
  PUBLIC_BASE_URL: "https://18.143.100.43/api/v1",
  AVATAR_UPLOAD_DIR: tmpdir(),
};

// Real Chromium renders fixture HTML; only route.fetch is redirected to a local server.
async function fixture(t, html = validHtml, status = 200, headers = {}) {
  let requests = 0;
  const server = createServer((_req, res) => {
    requests++;
    res.writeHead(status, {
      "Content-Type": "text/html; charset=utf-8",
      ...headers,
    });
    res.end(html);
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => new Promise((resolve) => server.close(resolve)));
  const localUrl = `http://127.0.0.1:${server.address().port}/`;
  const browser = await chromium.launch({ headless: true });
  t.after(async () => {
    if (browser.isConnected()) await browser.close();
  });
  const context = await browser.newContext();
  const setTimeout = context.setDefaultTimeout.bind(context);
  t.mock.method(context, "setDefaultTimeout", (timeout) => {
    assert.equal(timeout, 15_000);
    setTimeout(2_000);
  });
  const route = context.route.bind(context);
  t.mock.method(context, "route", (pattern, handler) =>
    route(pattern, async (requestRoute) => {
      t.mock.method(requestRoute, "fetch", (options) => {
        assert.equal(options.maxRedirects, 0);
        assert.equal(options.maxRetries, 0);
        return context.request.get(localUrl, options);
      });
      await handler(requestRoute);
    }),
  );
  t.mock.method(browser, "newContext", async () => context);
  t.mock.method(chromium, "launch", async (options) => {
    assert.equal(options.timeout, 15_000);
    return browser;
  });
  return { browser, context, requests: () => requests };
}

test("environment defaults preserve localhost and disable the crawler", () => {
  const config = validateEnvironment({
    ...authConfig,
    DATABASE_URL: databaseUrl,
  });
  assert.equal(config.HOST, "127.0.0.1");
  assert.equal(config.PORT, 5000);
  assert.equal(config.CRAWLER_ENABLED, false);
  for (const value of ["true", "false"]) {
    assert.equal(
      validateEnvironment({
        ...authConfig,
        DATABASE_URL: databaseUrl,
        CRAWLER_ENABLED: value,
      }).CRAWLER_ENABLED,
      value === "true",
    );
  }
  for (const value of ["1", "FALSE", "", "yes", " true "]) {
    assert.throws(
      () =>
        validateEnvironment({
          ...authConfig,
          DATABASE_URL: databaseUrl,
          CRAWLER_ENABLED: value,
        }),
      /CRAWLER_ENABLED must be true or false/,
    );
  }
  assert.throws(() => validateEnvironment({}), /DATABASE_URL is required/);
});

test("Chromium reads real DOM fields, not just HTTP 200 or page title", async (t) => {
  const f = await fixture(t);
  const result = await new CrawlerService().probe();
  assert.equal(result.sourceUrl, CGV_LANDMARK_URL);
  assert.equal(result.finalUrl, CGV_LANDMARK_URL);
  assert.equal(result.status, 200);
  assert.equal(result.title, "Site");
  assert.equal(result.theaterName, "CGV Vincom Center Landmark 81");
  assert.match(result.address, /772 Điện Biên Phủ/);
  assert.ok(Number.isFinite(Date.parse(result.checkedAt)));
  assert.equal(f.requests(), 1);
  assert.equal(f.browser.isConnected(), false);
});

test("Vietnamese Unicode and whitespace are normalized for assertions", async (t) => {
  await fixture(
    t,
    validHtml.normalize("NFD").replace("Landmark 81,", "Landmark\u00a0\n81,"),
  );
  const result = await new CrawlerService().probe();
  assert.equal(result.address, "Tầng B1, Landmark 81, 772 Điện Biên Phủ");
});

test("HTTP errors are rejected and the browser is closed", async (t) => {
  const f = await fixture(t, "Service unavailable", 503);
  await assert.rejects(new CrawlerService().probe(), /HTTP 503/);
  assert.equal(f.browser.isConnected(), false);
});

test("HTTP 200 without theater selectors is rejected", async (t) => {
  const f = await fixture(t, "<html><body>Verify you are human</body></html>");
  await assert.rejects(new CrawlerService().probe(), /Timeout/);
  assert.equal(f.browser.isConnected(), false);
});

test("empty or incorrect theater content is rejected", async (t) => {
  const f = await fixture(
    t,
    validHtml.replace("Landmark 81</h3>", "Other Cinema</h3>"),
  );
  await assert.rejects(
    new CrawlerService().probe(),
    /does not match Landmark 81/,
  );
  assert.equal(f.browser.isConnected(), false);
});

test("redirects are refused before following an untrusted destination", async (t) => {
  const f = await fixture(t, "", 302, {
    Location: "https://untrusted.invalid/",
  });
  await assert.rejects(new CrawlerService().probe(), /ERR_FAILED/);
  assert.equal(f.requests(), 1);
  assert.equal(f.browser.isConnected(), false);
});

test("external subresources are aborted without reaching fixture transport", async (t) => {
  const f = await fixture(
    t,
    validHtml.replace(
      "</body>",
      '<script src="https://untrusted.invalid/asset.js"></script></body>',
    ),
  );
  assert.equal((await new CrawlerService().probe()).status, 200);
  assert.equal(f.requests(), 1);
});

test("navigation timeout closes the browser", async (t) => {
  const f = await fixture(t);
  const page = await f.context.newPage();
  t.mock.method(f.context, "newPage", async () => page);
  t.mock.method(page, "goto", async () => {
    throw new Error("navigation timeout");
  });
  await assert.rejects(new CrawlerService().probe(), /navigation timeout/);
  assert.equal(f.browser.isConnected(), false);
});

test("launch errors propagate without hanging", async (t) => {
  t.mock.method(chromium, "launch", async () => {
    throw new Error("Chromium unavailable");
  });
  await assert.rejects(new CrawlerService().probe(), /Chromium unavailable/);
});

test("registered six-hour cron is disabled by config and prevents overlap", async (t) => {
  const config = new ConfigService({ CRAWLER_ENABLED: false });
  let calls = 0;
  let release;
  const crawler = {
    probe: async () => {
      calls++;
      await new Promise((resolve) => {
        release = resolve;
      });
      return { status: 200 };
    },
  };
  class TestModule {}
  Module({
    imports: [ScheduleModule.forRoot()],
    providers: [
      CrawlerJob,
      { provide: ConfigService, useValue: config },
      { provide: CrawlerService, useValue: crawler },
    ],
  })(TestModule);
  const app = await NestFactory.createApplicationContext(TestModule, {
    logger: false,
    abortOnError: false,
  });
  t.after(() => app.close());
  const job = app.get(SchedulerRegistry).getCronJob("crawl-cgv");
  await job.stop();
  assert.equal(job.waitForCompletion, true);
  assert.equal(job.cronTime.timeZone, "Asia/Ho_Chi_Minh");
  assert.equal(job.cronTime.source, "0 0 */6 * * *");
  await job.fireOnTick();
  assert.equal(calls, 0);
  config.set("CRAWLER_ENABLED", true);
  const first = job.fireOnTick();
  await job.fireOnTick();
  assert.equal(calls, 1);
  release();
  await first;
});

test("cron errors are logged, not propagated to the backend", async (t) => {
  const errors = [];
  t.mock.method(Logger.prototype, "error", (message) => errors.push(message));
  const job = new CrawlerJob(new ConfigService({ CRAWLER_ENABLED: true }), {
    probe: async () => {
      throw new Error("CGV unavailable");
    },
  });
  await job.run();
  assert.deepEqual(errors, ["CGV unavailable"]);
});

test("CLI needs no database and reports missing Chromium with exit 1", () => {
  const env = {
    ...process.env,
    PLAYWRIGHT_BROWSERS_PATH: join(
      tmpdir(),
      `n8-missing-browser-${randomUUID()}`,
    ),
  };
  delete env.DATABASE_URL;
  const result = spawnSync(process.execPath, ["dist/crawler/smoke.js"], {
    env,
    encoding: "utf8",
    timeout: 15_000,
    windowsHide: true,
  });
  assert.equal(result.status, 1);
  assert.match(result.stderr, /Executable doesn't exist/);
  assert.doesNotMatch(result.stderr, /DATABASE_URL|PostgreSQL/);
});
