import "reflect-metadata";
import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { spawnSync } from "node:child_process";
import { Test } from "@nestjs/testing";
import { PrismaService } from "../dist/prisma.service.js";
import {
  CatalogController,
  catalogDate,
  movieJson,
} from "../dist/catalog/catalog.controller.js";
import { CrawlerImportService } from "../dist/crawler/crawler-import.service.js";
import {
  businessDate,
  dateWindow,
  nextBusinessDate,
  normalizeFormat,
  sourceSlot,
  sourceMovieUrl,
  seatTemplate,
  PRICES,
} from "../dist/crawler/normalize.js";
import { parseMovie } from "../dist/crawler/parse-movie.js";

const url = "https://www.cgv.vn/default/fixture.html";
const raw = {
  title: " Phim thử ",
  rows: [
    "Thời lượng: 95 phút",
    "Khởi chiếu: 09/10/2026",
    "Đạo diễn: Người thử",
    "Diễn viên: A, B",
    "Ngôn ngữ: Tiếng Việt",
    "Thể loại: Hài",
  ],
  poster: "https://static-cgv.vncdn.vn/poster.jpg",
  age: "movie-rating-detail t18 Rated: T18",
  synopsis: " Nội\n dung ",
};
test("Vietnam date boundary, leap dates and UTC windows", () => {
  assert.equal(businessDate(new Date("2026-10-09T17:00:00Z")), "2026-10-10");
  assert.equal(businessDate(new Date("2026-10-09T16:59:59Z")), "2026-10-09");
  assert.equal(
    dateWindow("2026-10-10").start.toISOString(),
    "2026-10-09T17:00:00.000Z",
  );
  assert.equal(
    dateWindow("2024-02-29").end.toISOString(),
    "2024-02-29T17:00:00.000Z",
  );
  for (const d of [
    "2026-02-29",
    "2026-13-01",
    "2026-10-1",
    "",
    "x",
    "2026-04-31",
  ])
    assert.throws(() => catalogDate(d));
});
test("next calendar day handles Vietnam midnight, month, leap year and year boundaries", () => {
  assert.equal(nextBusinessDate("2026-10-31"), "2026-11-01");
  assert.equal(nextBusinessDate("2026-12-31"), "2027-01-01");
  assert.equal(nextBusinessDate("2024-02-28"), "2024-02-29");
  assert.equal(nextBusinessDate("2024-02-29"), "2024-03-01");
});
test("movie metadata uses labels (duration and cast share source classes), nullable values and no slug inference", () => {
  const m = parseMovie(url, raw, "icon-T18");
  assert.equal(m.title, "Phim thử");
  assert.equal(m.durationMinutes, 95);
  assert.equal(m.cast, "A, B");
  assert.equal(m.synopsis, "Nội dung");
  assert.equal(m.originalTitle, null);
  assert.equal(m.releaseDate.toISOString(), "2026-10-09T00:00:00.000Z");
  for (const age of ["P", "K", "T13", "T16", "T18"])
    assert.equal(
      parseMovie(url, { ...raw, age: `Rated: ${age}` }).ageRating,
      age,
    );
  assert.equal(
    parseMovie(url, { ...raw, rows: [...raw.rows, "Tên gốc: Original"] })
      .originalTitle,
    "Original",
  );
  for (const replacement of [
    { title: "" },
    { rows: [] },
    { age: "Rated: C" },
    { age: "T16 T18" },
  ])
    assert.throws(() => parseMovie(url, { ...raw, ...replacement }));
  assert.equal(
    parseMovie(url, { ...raw, poster: "https://untrusted.invalid/x" })
      .posterUrl,
    null,
  );
  assert.equal(
    parseMovie(url, {
      ...raw,
      rows: raw.rows.map((r) =>
        r.startsWith("Khởi") ? "Khởi chiếu: 31/02/2026" : r,
      ),
    }).releaseDate,
    null,
  );
  assert.equal(
    movieJson({ ...m, id: randomUUID(), updatedAt: new Date() }).releaseDate,
    "2026-10-09",
  );
});
test("showtime identities refuse other hosts/sites/dates and malformed time", () => {
  const link =
    "https://www.cgv.vn/default/cinemas/booking/tickets/site/071/seq/123/dy/20261010";
  const s = sourceSlot(link, "23:30", "2026-10-10");
  assert.equal(s.sourceKey, "cgv:071:20261010:123");
  assert.equal(s.startAt.toISOString(), "2026-10-10T16:30:00.000Z");
  for (const bad of [
    link.replace("071", "001"),
    link.replace("www.cgv.vn", "evil.invalid"),
    link.replace("20261010", "20261011"),
    `${link}?x=1`,
  ])
    assert.throws(() => sourceSlot(bad, "23:30", "2026-10-10"));
  assert.throws(() => sourceSlot(link, "24:00", "2026-10-10"));
  for (const bad of [
    "http://www.cgv.vn/default/a.html",
    "https://u@www.cgv.vn/default/a.html",
    "https://evil.invalid/default/a.html",
    "https://www.cgv.vn/default/a.html?x=1",
  ])
    assert.throws(() => sourceMovieUrl(bad));
});
test("six formats and source-screen capability take precedence over subtitle label", () => {
  for (const [label, expected] of Object.entries({
    "2D": "STANDARD_2D",
    "3D": "STANDARD_3D",
    "IMAX 2D": "IMAX_2D",
    IMAX3D: "IMAX_3D",
    SCREENX: "SCREENX",
    "GOLD CLASS": "GOLD_CLASS",
  }))
    assert.equal(normalizeFormat(label), expected);
  assert.equal(
    normalizeFormat("2D Phụ Đề Việt | Rạp GOLD CLASS", "film-screen goldclass"),
    "GOLD_CLASS",
  );
  assert.equal(
    normalizeFormat(
      "2D Phụ Đề Anh & Việt | Rạp SCREENX",
      "film-screen screenx",
    ),
    "SCREENX",
  );
  assert.equal(
    normalizeFormat("IMAX2D Phụ Đề Việt | Rạp IMAX", "film-screen imax"),
    "IMAX_2D",
  );
  for (const [label, cap] of [
    ["4DX", "film-screen 4dx"],
    ["IMAX2D", "film-screen std"],
    ["3D", "film-screen goldclass"],
  ])
    assert.throws(() => normalizeFormat(label, cap));
  assert.equal(
    normalizeFormat(
      "SCREENX-2D Phụ Đề Việt | Rạp SCREENX",
      "film-screen screenx",
    ),
    "SCREENX",
  );
  assert.equal(PRICES.STANDARD_2D, 100000);
});
test("seat template exactly matches capacities including last partial row", () => {
  for (const [type, n] of [
    ["STANDARD", 145],
    ["IMAX", 496],
    ["SCREENX", 180],
    ["GOLD", 32],
  ]) {
    const seats = seatTemplate(type, n);
    assert.equal(seats.length, n);
    assert.equal(new Set(seats.map((s) => s.seatCode)).size, n);
    assert.equal(seats[0].seatCode, "A1");
    assert.equal(seats[0].seatType, type === "GOLD" ? "GOLD" : "NORMAL");
  }
  assert.throws(() => seatTemplate("STANDARD", 0));
});

function fakeDb() {
  const movies = new Map(),
    slots = new Map(),
    rooms = [
      {
        id: randomUUID(),
        name: "Cinema 3",
        type: "STANDARD",
        capacity: 145,
        isExtra: false,
        status: "ACTIVE",
      },
    ],
    seats = [];
  let locks = 0;
  const tx = {
    $queryRaw: async () => {
      locks++;
    },
    movie: {
      upsert: async ({ create }) => {
        let m = movies.get(create.sourceUrl);
        m = { ...create, id: m?.id ?? randomUUID() };
        movies.set(create.sourceUrl, m);
        return m;
      },
    },
    showtime: {
      findUnique: async ({ where }) => slots.get(where.sourceKey) ?? null,
      create: async ({ data }) => {
        const s = { ...data, id: randomUUID(), status: "ACTIVE" };
        slots.set(data.sourceKey, s);
        return s;
      },
    },
    auditorium: {
      findFirst: async ({ where }) =>
        rooms.find(
          (r) =>
            r.type === where.type &&
            !Array.from(slots.values()).some(
              (s) =>
                s.auditoriumId === r.id &&
                s.status === "ACTIVE" &&
                s.startAt < where.showtimes.none.startAt.lt &&
                s.endAt > where.showtimes.none.endAt.gt,
            ),
        ) ?? null,
      create: async ({ data }) => {
        const r = { ...data, id: randomUUID() };
        rooms.push(r);
        return r;
      },
    },
    showtimeSeat: {
      createMany: async ({ data }) => {
        seats.push(...data.map((s) => ({ ...s, status: "AVAILABLE" })));
        return { count: data.length };
      },
    },
  };
  return {
    movies,
    slots,
    rooms,
    seats,
    locks: () => locks,
    $transaction: async (fn) => fn(tx),
  };
}
test("import is idempotent, allocates extras only on overlap and preserves occupied/canceled inventory", async () => {
  const db = fakeDb(),
    importer = new CrawlerImportService(null, db),
    movie = parseMovie(url, raw);
  const slot = (seq, time) => ({
    ...sourceSlot(
      `https://www.cgv.vn/default/cinemas/booking/tickets/site/071/seq/${seq}/dy/20261010`,
      time,
      "2026-10-10",
    ),
    movieUrl: url,
    format: "STANDARD_2D",
  });
  const snap = {
    movies: [movie],
    showtimes: [slot(1, "10:00"), slot(2, "10:10"), slot(3, "11:35")],
    skipped: [],
  };
  const a = await importer.import(snap);
  assert.equal(a.created, 3);
  assert.equal(db.rooms.length, 2);
  assert.equal(a.seatsCreated, 145 + 160 + 145);
  const assigned = [...db.slots.values()];
  assert.equal(assigned[0].auditoriumId, assigned[2].auditoriumId);
  db.seats[0].status = "HELD";
  db.seats[1].status = "SOLD";
  db.seats[2].status = "BLOCKED";
  assigned[1].status = "CANCELED";
  const before = JSON.stringify(db.seats);
  const b = await importer.import(snap);
  assert.equal(b.created, 0);
  assert.equal(b.unchanged, 3);
  assert.equal(b.seatsCreated, 0);
  assert.equal(JSON.stringify(db.seats), before);
  assert.equal(assigned[1].status, "CANCELED");
  const c = await importer.import({
    ...snap,
    showtimes: [
      { ...snap.showtimes[0], startAt: new Date("2026-10-10T05:00Z") },
    ],
  });
  assert.equal(c.conflicts.length, 1);
  assert.equal(
    db.slots.get(snap.showtimes[0].sourceKey).startAt,
    snap.showtimes[0].startAt,
  );
  assert.equal(db.locks(), 3);
});
test("real catalog HTTP returns envelopes, validates UUID/date, 404 and decimal strings", async (t) => {
  const id = randomUUID(),
    m = { ...parseMovie(url, raw), id, updatedAt: new Date() };
  let filter;
  const prisma = {
    movie: {
      findMany: async (q) => {
        filter = q;
        return [m];
      },
      findUnique: async (q) => (q.where.id === id ? m : null),
    },
    showtime: {
      findMany: async () => [
        {
          id: randomUUID(),
          basePrice: { toString: () => "100000" },
          startAt: new Date(),
        },
      ],
    },
  };
  const module = await Test.createTestingModule({
    controllers: [CatalogController],
    providers: [{ provide: PrismaService, useValue: prisma }],
  }).compile();
  const app = module.createNestApplication();
  app.setGlobalPrefix("api/v1");
  await app.listen(0, "127.0.0.1");
  t.after(() => app.close());
  const base = await app.getUrl();
  const get = (p) => fetch(`${base}/api/v1/movies${p}`);
  let r = await get("?date=2026-10-10");
  assert.equal(r.status, 200);
  assert.equal((await r.json()).items[0].ageRating, "T18");
  assert.equal(
    filter.where.showtimes.some.startAt.gte.toISOString(),
    "2026-10-09T17:00:00.000Z",
  );
  for (const p of [
    "?date=2026-02-29",
    "?date=2026-10-10&date=2026-10-11",
    "/bad",
    `/${id}/showtimes?date=bad`,
  ])
    assert.equal((await get(p)).status, 400);
  assert.equal((await get(`/${randomUUID()}`)).status, 404);
  r = await get(`/${id}/showtimes?date=2026-10-10`);
  assert.equal((await r.json()).items[0].basePrice, "100000");
  prisma.movie.findMany = async () => [];
  assert.deepEqual((await (await get("?date=2026-10-11")).json()).items, []);
});

test("multi-day importer logs per date and batch, preserves completed days on failure and rejects past dates", async (t) => {
  t.mock.timers.enable({
    apis: ["Date"],
    now: new Date("2026-10-10T12:00:00Z"),
  });
  const logs = [],
    errors = [];
  const { Logger } = await import("@nestjs/common");
  t.mock.method(Logger.prototype, "log", (line) => logs.push(JSON.parse(line)));
  t.mock.method(Logger.prototype, "error", (line) =>
    errors.push(JSON.parse(line)),
  );
  const db = fakeDb();
  let fail = false;
  const snapshots = ["2026-10-10", "2026-10-11", "2026-10-12"].map(
    (date, i) => ({
      businessDate: date,
      capturedAt: new Date().toISOString(),
      sourceMovies: i === 2 ? 0 : 1,
      sourceShowtimes: i === 2 ? 0 : 1,
      empty: i === 2,
      movies: i === 2 ? [] : [parseMovie(url, raw)],
      showtimes:
        i === 2
          ? []
          : [
              {
                ...sourceSlot(
                  `https://www.cgv.vn/default/cinemas/booking/tickets/site/071/seq/123/dy/${date.replaceAll("-", "")}`,
                  "10:00",
                  date,
                ),
                movieUrl: url,
                format: "STANDARD_2D",
              },
            ],
      skipped: [],
    }),
  );
  const crawler = {
    collectUpcoming: async (_today, consume) => {
      for (const snapshot of snapshots)
        await consume(snapshot.businessDate, async () => {
          if (fail && snapshot.businessDate === "2026-10-11")
            throw Error("Network failed");
          return snapshot;
        });
      return { stopReason: "EMPTY_DAY", stopDate: "2026-10-12" };
    },
  };
  const importer = new CrawlerImportService(crawler, db);
  const result = await importer.runUpcoming();
  assert.equal(result.event.status, "SUCCESS");
  assert.equal(result.runs.length, 3);
  assert.equal(new Set(result.runs.map((r) => r.event.runId)).size, 3);
  assert.ok(
    result.runs.every((r) => r.event.batchRunId === result.event.runId),
  );
  assert.equal(db.slots.size, 2);
  assert.equal(db.seats.length, 290);
  const repeated = await importer.runUpcoming();
  assert.ok(
    repeated.runs.every(
      (r) => r.event.created === 0 && r.event.seatsCreated === 0,
    ),
  );
  fail = true;
  await assert.rejects(importer.runUpcoming(), /Network failed/);
  assert.equal(db.slots.size, 2);
  assert.equal(errors[0].businessDate, "2026-10-11");
  assert.deepEqual(errors[1].completedDates, ["2026-10-10"]);
  await assert.rejects(importer.run("2026-10-09"), /past business date/);
  await assert.rejects(
    importer.runUpcoming("2026-10-09"),
    /past business date/,
  );
});

test("date listing is a static HTTP route, sorted and unique using Vietnam boundaries", async (t) => {
  t.mock.timers.enable({
    apis: ["Date"],
    now: new Date("2026-10-09T17:00:00Z"),
  });
  let query;
  const prisma = {
    showtime: {
      findMany: async (q) => {
        query = q;
        return [
          { startAt: new Date("2026-10-10T17:00:00Z") },
          { startAt: new Date("2026-10-09T17:00:00Z") },
          { startAt: new Date("2026-10-10T03:00:00Z") },
        ];
      },
    },
  };
  const module = await Test.createTestingModule({
    controllers: [CatalogController],
    providers: [{ provide: PrismaService, useValue: prisma }],
  }).compile();
  const app = module.createNestApplication();
  app.setGlobalPrefix("api/v1");
  await app.listen(0, "127.0.0.1");
  t.after(() => app.close());
  const response = await fetch(`${await app.getUrl()}/api/v1/movies/dates`);
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), {
    businessDate: "2026-10-10",
    dates: ["2026-10-10", "2026-10-11"],
  });
  assert.equal(query.where.status, "ACTIVE");
  assert.equal(
    query.where.startAt.gte.toISOString(),
    "2026-10-09T17:00:00.000Z",
  );
  prisma.showtime.findMany = async () => [];
  assert.deepEqual(
    (await (await fetch(`${await app.getUrl()}/api/v1/movies/dates`)).json())
      .dates,
    [],
  );
});

test("one-shot CLI rejects past date before opening Nest or the shared database", () => {
  const { status, stderr, stdout } = spawnSync(
    process.execPath,
    ["dist/crawler/run.js", "2000-01-01"],
    { encoding: "utf8", timeout: 15000 },
  );
  assert.equal(status, 1);
  assert.match(stderr, /Cannot crawl a past business date/);
  assert.doesNotMatch(stdout, /NestFactory|Prisma/);
});

test("English subtitles do not change format or source screen capability", () => {
  assert.equal(
    normalizeFormat(
      "SCREENX-2D Eng&Viet Sub | Rạp SCREENX",
      "film-screen screenx",
    ),
    "SCREENX",
  );
  assert.equal(
    normalizeFormat("IMAX2D Eng Sub | Rạp IMAX", "film-screen imax"),
    "IMAX_2D",
  );
  assert.equal(
    normalizeFormat("2D Viet Sub", "film-screen std"),
    "STANDARD_2D",
  );
  assert.throws(() =>
    normalizeFormat("SCREENX-2D Eng&Viet Sub", "film-screen std"),
  );
});
test("movie detail age is canonical when CGV schedule badge is stale; fallback only when detail has none", () => {
  assert.equal(
    parseMovie(
      url,
      { ...raw, age: "movie-rating-detail t13 Rated: T13" },
      "icon-P",
    ).ageRating,
    "T13",
  );
  assert.equal(
    parseMovie(url, { ...raw, age: "" }, "icon-T16").ageRating,
    "T16",
  );
  assert.throws(() => parseMovie(url, { ...raw, age: "T16 T18" }, "icon-P"));
  assert.throws(() => parseMovie(url, { ...raw, age: "" }, ""));
});
