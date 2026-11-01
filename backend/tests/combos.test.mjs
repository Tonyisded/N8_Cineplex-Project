import "reflect-metadata";
import test from "node:test";
import assert from "node:assert/strict";
import { Test } from "@nestjs/testing";
import { SwaggerModule, DocumentBuilder } from "@nestjs/swagger";
import { PrismaService } from "../dist/prisma.service.js";
import { CombosController } from "../dist/catalog/combos.controller.js";
import { CatalogModule } from "../dist/catalog/catalog.module.js";

test("combo HTTP is public, read-only, ordered, complete and documented", async (t) => {
  const rows = ["AVAILABLE", "UNAVAILABLE"].map((status, i) => ({
    id: `81000000-0000-4000-8000-00000000000${i + 1}`,
    name: "Combo",
    description: "Complete product",
    price: { toString: () => "100000" },
    imageUrl: i ? "https://example.test/combo.png" : null,
    status,
    privateField: "must not leak",
  }));
  let reads = 0;
  const db = {
    combo: {
      findMany: async (q) => {
        reads++;
        assert.deepEqual(q, { orderBy: [{ name: "asc" }, { id: "asc" }] });
        return rows;
      },
    },
  };
  assert.ok(
    Reflect.getMetadata("controllers", CatalogModule).includes(
      CombosController,
    ),
  );
  const module = await Test.createTestingModule({
    controllers: [CombosController],
    providers: [{ provide: PrismaService, useValue: db }],
  }).compile();
  const app = module.createNestApplication({ logger: false });
  app.setGlobalPrefix("api/v1");
  await app.listen(0, "127.0.0.1");
  t.after(() => app.close());
  const before = JSON.stringify(rows);
  for (let i = 0; i < 2; i++) {
    const res = await fetch(`${await app.getUrl()}/api/v1/combos`);
    assert.equal(res.status, 200);
    const body = await res.json();
    assert.ok(Number.isFinite(Date.parse(body.serverTime)));
    assert.deepEqual(
      body.items.map((c) => c.status),
      ["AVAILABLE", "UNAVAILABLE"],
    );
    assert.equal(body.items[0].price, "100000");
    assert.equal(body.items[0].imageUrl, null);
    assert.equal(body.items[0].privateField, undefined);
    assert.equal(new Set(body.items.map((c) => c.id)).size, 2);
  }
  assert.equal(JSON.stringify(rows), before);
  assert.equal(reads, 2);
  rows.length = 0;
  assert.deepEqual(
    (await (await fetch(`${await app.getUrl()}/api/v1/combos`)).json()).items,
    [],
  );
  const spec = SwaggerModule.createDocument(app, new DocumentBuilder().build());
  const schema =
    spec.paths["/api/v1/combos"].get.responses["200"].content[
      "application/json"
    ].schema;
  assert.equal(schema.properties.items.items.properties.price.type, "string");
  assert.deepEqual(schema.properties.items.items.properties.status.enum, [
    "AVAILABLE",
    "UNAVAILABLE",
  ]);
});
