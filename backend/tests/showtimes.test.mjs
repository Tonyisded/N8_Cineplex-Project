import "reflect-metadata";
import test from "node:test";
import assert from "node:assert/strict";
import { Test } from "@nestjs/testing";
import { SwaggerModule, DocumentBuilder } from "@nestjs/swagger";
import { PrismaService } from "../dist/prisma.service.js";
import { ShowtimesController } from "../dist/catalog/showtimes.controller.js";
import { CatalogModule } from "../dist/catalog/catalog.module.js";

test("showtime HTTP is read-only, handles stale selections, protects holders and documents both routes", async (t) => {
  t.mock.timers.enable({
    apis: ["Date"],
    now: new Date("2026-10-17T00:00:00Z"),
  });
  const ids = [1, 2, 3, 4].map(
    (n) => `10000000-0000-4000-8000-${String(n).padStart(12, "0")}`,
  );
  const slots = ids.map((id, i) => ({
    id,
    movieId: "movie",
    sourceKey: id,
    startAt: new Date(i === 2 ? "2026-10-16T03:00Z" : "2026-10-18T03:00Z"),
    endAt: new Date(i === 2 ? "2026-10-16T05:00Z" : "2026-10-18T05:00Z"),
    format: "IMAX_3D",
    presentationLabel: null,
    status: i === 1 ? "CANCELED" : "ACTIVE",
    basePrice: { toString: () => "180000" },
    auditorium: {
      id: "room",
      name: "Cinema 1",
      type: "IMAX",
      capacity: 496,
      isExtra: false,
    },
  }));
  const seats = [10, 2, 1, 3].map((n, i) => ({
    id: `seat-${n}`,
    showtimeId: ids[0],
    rowName: "A",
    seatNumber: n,
    seatCode: `A${n}`,
    seatType: "NORMAL",
    price: { toString: () => "180000" },
    status: ["SOLD", "HELD", "AVAILABLE", "BLOCKED"][i],
    heldByUserId: "private-user",
    holdExpiresAt: new Date(),
  }));
  let readQuery;
  const project = (item, select) =>
    Object.fromEntries(Object.keys(select).map((key) => [key, item[key]]));
  const prisma = {
    showtime: {
      findUnique: async ({ where }) =>
        slots.find((s) => s.id === where.id) ?? null,
    },
    showtimeSeat: {
      groupBy: async ({ where }) =>
        seats
          .filter((s) => where.showtimeId.in.includes(s.showtimeId))
          .map((s) => ({
            showtimeId: s.showtimeId,
            status: s.status,
            _count: { _all: 1 },
          })),
      findMany: async (q) => {
        readQuery = q;
        return seats
          .filter((s) => s.showtimeId === q.where.showtimeId)
          .sort(
            (a, b) =>
              a.rowName.localeCompare(b.rowName) || a.seatNumber - b.seatNumber,
          )
          .map((s) => project(s, q.select));
      },
    },
  };
  const before = JSON.stringify({ slots, seats });
  assert.ok(
    Reflect.getMetadata("controllers", CatalogModule).includes(
      ShowtimesController,
    ),
  );
  const module = await Test.createTestingModule({
    controllers: [ShowtimesController],
    providers: [{ provide: PrismaService, useValue: prisma }],
  }).compile();
  const app = module.createNestApplication({ logger: false });
  app.setGlobalPrefix("api/v1");
  await app.listen(0, "127.0.0.1");
  t.after(() => app.close());
  const base = await app.getUrl();
  const get = (p) => fetch(`${base}/api/v1/showtimes/${p}`);
  const detail = await (await get(ids[0])).json();
  assert.equal(detail.serverTime, "2026-10-17T00:00:00.000Z");
  assert.equal(detail.item.format, "IMAX_3D");
  assert.equal(detail.item.basePrice, "180000");
  assert.equal(detail.item.availableSeats, 1);
  assert.equal(detail.item.totalSeats, 4);
  assert.equal((await (await get(ids[1])).json()).item.status, "CANCELED");
  assert.equal((await get(ids[2])).status, 200);
  for (const suffix of ["", "/seats"]) {
    assert.equal((await get(`bad${suffix}`)).status, 400);
    const absent = await get(`10000000-0000-4000-8000-000000000099${suffix}`);
    assert.equal(absent.status, 404);
    assert.equal((await absent.json()).code, "SHOWTIME_NOT_FOUND");
  }
  const inventory = await (await get(`${ids[0]}/seats`)).json();
  assert.deepEqual(
    inventory.items.map((s) => s.seatCode),
    ["A1", "A2", "A3", "A10"],
  );
  assert.deepEqual(
    inventory.items.map((s) => s.status),
    ["AVAILABLE", "HELD", "BLOCKED", "SOLD"],
  );
  assert.ok(inventory.items.every((s) => s.price === "180000"));
  assert.ok(
    inventory.items.every(
      (s) =>
        !Object.hasOwn(s, "heldByUserId") &&
        !Object.hasOwn(s, "holdExpiresAt") &&
        !Object.hasOwn(s, "heldByUser"),
    ),
  );
  assert.deepEqual(readQuery.orderBy, [
    { rowName: "asc" },
    { seatNumber: "asc" },
    { id: "asc" },
  ]);
  assert.equal(Object.hasOwn(readQuery.select, "heldByUserId"), false);
  assert.deepEqual((await (await get(`${ids[3]}/seats`)).json()).items, []);
  const empty = (await (await get(ids[3])).json()).item;
  assert.equal(empty.totalSeats, 0);
  assert.equal(empty.availableSeats, 0);
  assert.equal(JSON.stringify({ slots, seats }), before);
  const doc = SwaggerModule.createDocument(
    app,
    new DocumentBuilder().setTitle("Fixture").setVersion("1").build(),
  );
  for (const p of ["/api/v1/showtimes/{id}", "/api/v1/showtimes/{id}/seats"]) {
    assert.ok(doc.paths[p].get.responses[200]);
    assert.ok(doc.paths[p].get.responses[400]);
    assert.ok(doc.paths[p].get.responses[404]);
  }
});
