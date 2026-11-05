import test, { after } from "node:test";
import assert from "node:assert/strict";
import { randomBytes } from "node:crypto";
import { ConfigService } from "@nestjs/config";
import { PayOS } from "@payos/node";
import { PayosAdapter } from "../dist/payments/payos.adapter.js";
import { safeRefundError } from "../dist/payments/refund-retry.js";
const marker = "SECRET_RECIPIENT_TOKEN_MARKER",
  checksum = randomBytes(32).toString("hex");
const config = new ConfigService({
  PAYOS_PAYOUT_CLIENT_ID: randomBytes(16).toString("hex"),
  PAYOS_PAYOUT_API_KEY: randomBytes(16).toString("hex"),
  PAYOS_PAYOUT_CHECKSUM_KEY: checksum,
});
const sdk = new PayOS({
  clientId: "fixture",
  apiKey: "fixture",
  checksumKey: checksum,
  logLevel: "off",
});
const body = {
  referenceId: "fixture",
  amount: 179000,
  description: "fixture",
  toBin: "970436",
  toAccountNumber: "00000000",
};
const payout = {
  id: "fixture",
  referenceId: body.referenceId,
  transactions: [
    {
      referenceId: body.referenceId,
      amount: body.amount,
      toBin: body.toBin,
      toAccountNumber: body.toAccountNumber,
      state: "SUCCEEDED",
    },
  ],
};
const originalFetch = globalThis.fetch;
let network = 0;
globalThis.fetch = async () => {
  network++;
  throw Error("Network forbidden in fixture");
};
after(() => {
  globalThis.fetch = originalFetch;
  assert.equal(network, 0);
});
const event = (e) => safeRefundError(e, "send", new Date(0), 20, 1, 1);
function adapterFor(
  fetch,
  execute = async (_key, _operation, _interval, call) => call(),
) {
  const adapter = new PayosAdapter(config, { execute });
  adapter.payoutTransport = () => ({
    fetch,
    fetchOptions: { redirect: "error" },
  });
  return adapter;
}
function response(status, code, data, headers = {}) {
  return new Response(JSON.stringify({ code, desc: marker, data }), {
    status,
    headers: {
      "content-type": "application/json",
      "x-secret": marker,
      ...headers,
    },
  });
}
function assertPrivate(error) {
  assert.ok(!JSON.stringify(event(error)).includes(marker));
  assert.ok(!error.message.includes(marker));
  assert.ok(!JSON.stringify(error.error ?? null).includes(marker));
  assert.ok(
    !JSON.stringify([...(error.headers?.entries?.() ?? [])]).includes(marker),
  );
}
for (const [status, code, kind] of [
  [200, "99", "APIError"],
  [400, "400", "BadRequestError"],
  [429, "429", "TooManyRequestError"],
  [500, "500", "APIError"],
]) {
  test(`unsigned HTTP${status} business error preserves status/code and does not become connection error`, async () => {
    let calls = 0,
      admittedError;
    const adapter = adapterFor(
      async () => {
        calls++;
        return response(status, code, null, { "retry-after": "600" });
      },
      async (_key, op, interval, call) => {
        assert.equal(op, "PAYOUT_CREATE");
        assert.equal(interval, 5000);
        try {
          return await call();
        } catch (e) {
          admittedError = e;
          throw e;
        }
      },
    );
    try {
      await assert.rejects(adapter.createPayout(body, "same-key"), (e) => {
        assert.equal(e.constructor.name, kind);
        assert.equal(e.status, status);
        assert.equal(e.code, code);
        assert.equal(e.headers.get("retry-after"), "600");
        assert.equal(event(e).signaturePresent, false);
        assert.equal(admittedError, e);
        assertPrivate(e);
        return true;
      });
      assert.equal(calls, 1);
    } finally {
      await adapter.onModuleDestroy();
    }
  });
}
for (const [code, expectedKind] of [
  ["ENOTFOUND", "ConnectionError"],
  ["ECONNRESET", "ConnectionError"],
  ["ERR_TLS_CERT_ALTNAME_INVALID", "ConnectionError"],
  ["UND_ERR_CONNECT_TIMEOUT", "ConnectionTimeoutError"],
  [marker, "ConnectionError"],
]) {
  test(`transport cause ${code === marker ? "unknown" : code} remains safe and typed`, async () => {
    const adapter = adapterFor(async () => {
      throw new TypeError(marker, {
        cause: Object.assign(new Error(marker), {
          code,
          headers: { secret: marker },
        }),
      });
    });
    try {
      await assert.rejects(adapter.createPayout(body, "same-key"), (e) => {
        assert.equal(e.constructor.name, expectedKind);
        assert.equal(
          event(e).transportCode,
          code === marker ? undefined : code,
        );
        assert.equal(e.status, undefined);
        assert.equal(e.cause, undefined);
        assertPrivate(e);
        return true;
      });
    } finally {
      await adapter.onModuleDestroy();
    }
  });
}
for (const action of ["create", "get", "list", "balance"]) {
  for (const signed of [true, false])
    test(`${action} accepts only a signed successful payout response`, async () => {
      const data =
        action === "list"
          ? {
              payouts: [payout],
              pagination: {
                total: 1,
                limit: 10,
                offset: 0,
                count: 1,
                hasMore: false,
              },
            }
          : action === "balance"
            ? { balance: "200000" }
            : payout;
      const signature = await sdk.crypto.createSignature(checksum, data);
      let calls = 0;
      const adapter = adapterFor(async () => {
        calls++;
        return response(
          200,
          "00",
          data,
          signed ? { "x-signature": signature } : {},
        );
      });
      const call = () =>
        action === "create"
          ? adapter.createPayout(body, "same-key")
          : action === "get"
            ? adapter.getPayout("fixture")
            : action === "list"
              ? adapter.findPayout("fixture")
              : adapter.payoutBalance();
      try {
        if (signed) {
          const value = await call();
          assert.deepEqual(value, action === "list" ? [payout] : data);
        } else
          await assert.rejects(call(), (e) => {
            assert.equal(e.constructor.name, "InvalidSignatureError");
            assert.equal(e.status, 200);
            assert.equal(event(e).signaturePresent, false);
            assertPrivate(e);
            return true;
          });
        assert.equal(calls, 1);
      } finally {
        await adapter.onModuleDestroy();
      }
    });
}
test("invalid signature and invalid JSON cannot return usable payout data", async () => {
  for (const invalidJSON of [false, true]) {
    const adapter = adapterFor(async () =>
      invalidJSON
        ? new Response(marker, {
            status: 200,
            headers: {
              "content-type": "application/json",
              "x-signature": "invalid",
            },
          })
        : response(200, "00", payout, { "x-signature": "invalid" }),
    );
    try {
      await assert.rejects(adapter.createPayout(body, "same-key"), (e) => {
        assert.equal(e.status, 200);
        assert.equal(
          e.constructor.name,
          invalidJSON ? "APIError" : "InvalidSignatureError",
        );
        assertPrivate(e);
        return true;
      });
    } finally {
      await adapter.onModuleDestroy();
    }
  }
});
test("concurrent payout calls cannot borrow each other's successful signature", async () => {
  let release, entered;
  const gate = new Promise((r) => (release = r)),
    reading = new Promise((r) => (entered = r));
  const adapter = adapterFor(async (_url, init) => {
    const a = JSON.parse(init.body).referenceId === "unsigned";
    const data = { ...payout, referenceId: a ? "unsigned" : "signed" };
    const res = response(
      200,
      "00",
      data,
      a
        ? {}
        : { "x-signature": await sdk.crypto.createSignature(checksum, data) },
    );
    if (a)
      res.json = async () => {
        entered();
        await gate;
        return { code: "00", data };
      };
    return res;
  });
  try {
    const unsigned = adapter
      .createPayout({ ...body, referenceId: "unsigned" }, "a")
      .then(
        () => null,
        (e) => e,
      );
    await reading;
    await adapter.createPayout({ ...body, referenceId: "signed" }, "b");
    release();
    const e = await unsigned;
    assert.equal(e.constructor.name, "InvalidSignatureError");
    assert.equal(event(e).signaturePresent, false);
  } finally {
    release();
    await adapter.onModuleDestroy();
  }
});
test("safe error metadata ignores unknown provider/cause strings and raw SDK data", () => {
  const e = {
    status: 429,
    code: marker,
    signaturePresent: marker,
    transportCode: marker,
    message: marker,
    headers: { secret: marker },
    cause: { code: marker },
    stack: marker,
  };
  assert.ok(!JSON.stringify(event(e)).includes(marker));
  assert.equal(event(e).transportCode, undefined);
  assert.equal(event(e).providerCode, undefined);
  assert.equal(event(e).signaturePresent, undefined);
});

for (const [input, expected] of [
  ["Wed, 01 Jan 2031 00:00:00 GMT", "Wed, 01 Jan 2031 00:00:00 GMT"],
  ["SECRET_RECIPIENT_TOKEN_MARKER", null],
])
  test(
    "Retry-After retains valid date only: " + (expected ? "date" : "redacted"),
    async () => {
      const adapter = adapterFor(async () =>
        response(429, "429", null, { "retry-after": input }),
      );
      try {
        await assert.rejects(adapter.createPayout(body, "same-key"), (e) => {
          assert.equal(e.status, 429);
          assert.equal(e.headers.get("retry-after"), expected);
          assertPrivate(e);
          return true;
        });
      } finally {
        await adapter.onModuleDestroy();
      }
    },
  );
