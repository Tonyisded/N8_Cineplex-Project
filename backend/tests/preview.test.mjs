import "reflect-metadata";
import test from "node:test";
import assert from "node:assert/strict";
import { CatalogController } from "../dist/catalog/catalog.controller.js";
import {
  presentationLabel,
  trailerYoutubeId,
} from "../dist/crawler/normalize.js";
import { parseMovie } from "../dist/crawler/parse-movie.js";

test("trailer parser accepts source embed IDs and refuses foreign hosts, credentials, schemes and channel links", () => {
  for (const source of [
    "//www.youtube.com/embed/xGiimAKN0TM?rel=0",
    "https://youtu.be/xGiimAKN0TM",
    "https://www.youtube-nocookie.com/embed/xGiimAKN0TM",
    "https://www.youtube.com/watch?v=xGiimAKN0TM",
  ])
    assert.equal(trailerYoutubeId(source), "xGiimAKN0TM");
  for (const source of [
    "javascript:alert(1)",
    "http://www.youtube.com/embed/xGiimAKN0TM",
    "https://www.youtube.com.evil.invalid/embed/xGiimAKN0TM",
    "https://u@www.youtube.com/embed/xGiimAKN0TM",
    "https://www.youtube.com:8443/embed/xGiimAKN0TM",
    "https://www.youtube.com/cgvvietnam",
    "https://youtu.be/bad",
    "",
    "https://www.youtube.com/embed/xGiimAKN0TM/extra",
  ])
    assert.equal(trailerYoutubeId(source), null);
  const movie = parseMovie("https://www.cgv.vn/default/fixture.html", {
    title: "Phim",
    rows: ["Thời lượng: 90 phút"],
    synopsis: "",
    poster: "",
    age: "T13",
    trailer: "//www.youtube.com/embed/xGiimAKN0TM",
  });
  assert.equal(movie.trailerYoutubeId, "xGiimAKN0TM");
});

test("presentation labels come from each screen, not movie language or format assumptions", () => {
  assert.equal(
    presentationLabel("IMAX2D Phụ Đề Việt | Rạp IMAX"),
    "Phụ Đề Việt",
  );
  assert.equal(presentationLabel("2D Lồng Tiếng"), "Lồng Tiếng");
  assert.equal(
    presentationLabel("SCREENX-2D Eng&Viet Sub | Rạp SCREENX"),
    "Eng&Viet Sub",
  );
  assert.equal(presentationLabel("2D"), null);
});

function fixtureDb() {
  const slots = [
    {
      id: "past",
      movieId: "m1",
      status: "ACTIVE",
      startAt: new Date("2026-10-13T07:10Z"),
    },
    {
      id: "future",
      movieId: "m1",
      status: "ACTIVE",
      startAt: new Date("2026-10-13T13:50Z"),
    },
    {
      id: "only-past",
      movieId: "m2",
      status: "ACTIVE",
      startAt: new Date("2026-10-13T06:00Z"),
    },
    {
      id: "canceled",
      movieId: "m2",
      status: "CANCELED",
      startAt: new Date("2026-10-13T14:00Z"),
    },
    {
      id: "tomorrow",
      movieId: "m2",
      status: "ACTIVE",
      startAt: new Date("2026-10-14T03:00Z"),
    },
  ].map((s) => ({
    ...s,
    endAt: new Date(s.startAt.getTime() + 90 * 60_000),
    basePrice: { toString: () => "100000" },
    format: "STANDARD_2D",
    presentationLabel: "Phụ đề Việt",
    auditorium: { capacity: 10 },
  }));
  const movies = ["m1", "m2"].map((id) => ({
    id,
    title: id,
    releaseDate: null,
  }));
  let countCalls = 0;
  const matching = (q) =>
    slots.filter(
      (s) =>
        (!q.movieId || s.movieId === q.movieId) &&
        s.status === q.status &&
        s.startAt > q.startAt.gt &&
        (!q.startAt.gte || s.startAt >= q.startAt.gte) &&
        (!q.startAt.lt || s.startAt < q.startAt.lt),
    );
  return {
    movie: {
      findMany: async (q) =>
        movies
          .map((m) => ({
            ...m,
            showtimes: matching({
              ...q.include.showtimes.where,
              movieId: m.id,
            }),
          }))
          .filter((m) => m.showtimes.length),
      findUnique: async (q) => movies.find((m) => m.id === q.where.id),
    },
    showtime: { findMany: async (q) => matching(q.where) },
    showtimeSeat: {
      groupBy: async (q) => {
        countCalls++;
        return ["AVAILABLE", "HELD", "SOLD", "BLOCKED"].map((status, i) => ({
          showtimeId: q.where.showtimeId.in[0],
          status,
          _count: { _all: i === 0 ? 7 : 1 },
        }));
      },
    },
    countCalls: () => countCalls,
  };
}

test("catalog removes begun slots and exhausted movies, batch-counts only AVAILABLE and keeps future dates", async (t) => {
  t.mock.timers.enable({
    apis: ["Date"],
    now: new Date("2026-10-13T07:09:59Z"),
  });
  const db = fixtureDb(),
    controller = new CatalogController(db);
  const before = await controller.list("2026-10-13");
  assert.equal(before.items.length, 1);
  assert.deepEqual(
    before.items[0].showtimes.map((s) => s.id),
    ["past", "future"],
  );
  assert.equal(db.countCalls(), 1);
  t.mock.timers.tick(1000);
  const atStart = await controller.list("2026-10-13");
  assert.equal(atStart.serverTime, "2026-10-13T07:10:00.000Z");
  assert.deepEqual(
    atStart.items[0].showtimes.map((s) => s.id),
    ["future"],
  );
  assert.equal(atStart.items[0].showtimes[0].availableSeats, 7);
  assert.equal(atStart.items[0].showtimes[0].totalSeats, 10);
  assert.equal(atStart.items[0].showtimes[0].basePrice, "100000");
  assert.deepEqual((await controller.showtimes("m2", "2026-10-13")).items, []);
  t.mock.timers.setTime(new Date("2026-10-13T13:50Z").getTime());
  assert.deepEqual((await controller.list("2026-10-13")).items, []);
  assert.equal((await controller.list("2026-10-14")).items[0].id, "m2");
  assert.deepEqual((await controller.list("2026-10-12")).items, []);
});

test("missing seat inventory returns zero counters, not an invented auditorium capacity", async (t) => {
  t.mock.timers.enable({ apis: ["Date"], now: new Date("2026-10-13T07:10Z") });
  const db = fixtureDb();
  db.showtimeSeat.groupBy = async () => [];
  const result = await new CatalogController(db).showtimes("m1", "2026-10-13");
  assert.equal(result.items[0].totalSeats, 0);
  assert.equal(result.items[0].availableSeats, 0);
});
