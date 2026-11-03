import test from "node:test";
import assert from "node:assert/strict";
import http from "node:http";
import { randomBytes } from "node:crypto";
import { ConfigService } from "@nestjs/config";
import { PayosAdapter } from "../dist/payments/payos.adapter.js";
test("payout SDK uses fixed authenticated CONNECT proxy, never falls back direct; collection is unaffected", async (t) => {
  const connections = [];
  const proxy = http.createServer();
  proxy.on("connect", (req, socket) => {
    connections.push({ url: req.url, headers: req.headers });
    socket.end(
      "HTTP/1.1 407 Proxy Authentication Required\r\nContent-Length: 0\r\nConnection: close\r\n\r\n",
    );
  });
  await new Promise((resolve) => proxy.listen(0, "127.0.0.1", resolve));
  const secret = randomBytes(16).toString("hex"),
    config = new ConfigService({
      PAYOS_PAYOUT_PROXY_ENABLED: true,
      PAYOS_PAYOUT_PROXY_SERVER: "http://127.0.0.1:" + proxy.address().port,
      PAYOS_PAYOUT_PROXY_USERNAME: "fixture",
      PAYOS_PAYOUT_PROXY_PASSWORD: secret,
      ...Object.fromEntries(
        ["PAYOUT", "COLLECT"].flatMap((k) =>
          ["CLIENT_ID", "API_KEY", "CHECKSUM_KEY"].map((s) => [
            "PAYOS_" + k + "_" + s,
            randomBytes(32).toString("hex"),
          ]),
        ),
      ),
    });
  let direct = 0;
  t.mock.method(globalThis, "fetch", async () => {
    direct++;
    throw new Error("DIRECT_FIXTURE");
  });
  const adapter = new PayosAdapter(config, {
    execute: async (_key, _op, _interval, call) => call(),
  });
  try {
    await assert.rejects(adapter.findPayout("fixture"));
    assert.ok(connections.length >= 1);
    assert.equal(connections[0].url, "api-merchant.payos.vn:443");
    assert.equal(
      connections[0].headers["proxy-authorization"],
      "Basic " + Buffer.from("fixture:" + secret).toString("base64"),
    );
    assert.equal(connections[0].headers["x-api-key"], undefined);
    assert.equal(direct, 0);
    await assert.rejects(adapter.getLink(1));
    assert.equal(direct, 1);
  } finally {
    await adapter.onModuleDestroy();
    await new Promise((resolve) => proxy.close(resolve));
  }
});
