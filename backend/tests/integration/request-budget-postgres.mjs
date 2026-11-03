import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { ConfigService } from "@nestjs/config";
import { PrismaService } from "../../dist/prisma.service.js";
import {
  ProviderGovernor,
  ProviderDeferred,
} from "../../dist/payments/provider-governor.js";
import { PaymentsService } from "../../dist/payments/payments.service.js";
import { initialRefundRetry } from "../../dist/payments/refund-retry.js";
if (!process.env.DATABASE_URL?.includes("w9_test"))
  throw new Error("Disposable database required");
const db = new PrismaService(new ConfigService(process.env));
const governor = new ProviderGovernor(db);
const reset = async (key) =>
  db.$executeRaw`UPDATE provider_api_governor SET blocked_until=clock_timestamp(),next_allowed_at=clock_timestamp(),lease_until=clock_timestamp() WHERE channel_key=${key}`;
test("PostgreSQL governor/inbox/resume contract", async (t) => {
  try {
    await t.test(
      "six competing callers produce one HTTP; lease and interval survive restart",
      async () => {
        const key = randomUUID();
        let calls = 0;
        const results = await Promise.allSettled(
          Array.from({ length: 6 }, () =>
            new ProviderGovernor(db).execute(key, "CREATE", 5000, async () => {
              calls++;
              await new Promise((r) => setTimeout(r, 50));
              return true;
            }),
          ),
        );
        assert.equal(calls, 1);
        assert.equal(results.filter((x) => x.status === "fulfilled").length, 1);
        await assert.rejects(
          new ProviderGovernor(db).execute(key, "GET", 5000, async () => {
            calls++;
          }),
          ProviderDeferred,
        );
        assert.equal(calls, 1);
      },
    );
    await t.test(
      "POST429 blocks all callers, successful GET does not reset POST streak",
      async () => {
        const key = randomUUID();
        const fail = () => {
          throw { status: 429, headers: new Headers({ "retry-after": "1" }) };
        };
        await assert.rejects(governor.execute(key, "CREATE", 5000, fail));
        await assert.rejects(
          governor.execute(key, "GET", 5000, async () => true),
          ProviderDeferred,
        );
        await reset(key);
        await governor.execute(key, "GET", 5000, async () => true);
        await reset(key);
        await assert.rejects(governor.execute(key, "CREATE", 5000, fail));
        const [row] =
          await db.$queryRaw`SELECT streaks,extract(epoch from blocked_until-clock_timestamp()) AS wait FROM provider_api_governor WHERE channel_key=${key}`;
        assert.equal(row.streaks.CREATE, 2);
        assert.ok(Number(row.wait) > 350);
      },
    );
    await t.test(
      "Retry-After600 dominates default180 and other channels remain available",
      async () => {
        const key = randomUUID();
        await assert.rejects(
          governor.execute(key, "CREATE", 5000, async () => {
            throw {
              status: 429,
              headers: new Headers({ "retry-after": "600" }),
            };
          }),
        );
        const [row] =
          await db.$queryRaw`SELECT extract(epoch from blocked_until-clock_timestamp()) AS wait FROM provider_api_governor WHERE channel_key=${key}`;
        assert.ok(Number(row.wait) > 590);
        assert.equal(
          await governor.execute(randomUUID(), "GET", 1000, async () => 42),
          42,
        );
      },
    );
    await t.test(
      "ten identical verified webhooks persist one inbox event; two consumers perform one provider query",
      async () => {
        const ref = randomUUID(),
          orderCode = 8000000 + Math.floor(Math.random() * 1000000),
          linkId = randomUUID();
        let calls = 0;
        const provider = {
          verify: async () => ({
            code: "00",
            currency: "VND",
            reference: ref,
            orderCode,
            amount: 100000,
            accountNumber: "123456789",
            paymentLinkId: linkId,
          }),
          getLink: async () => {
            calls++;
            await new Promise((r) => setTimeout(r, 50));
            return {
              id: linkId,
              orderCode,
              transactions: [{ reference: ref, amount: 100000 }],
            };
          },
        };
        const service = () =>
          new PaymentsService(db, new ConfigService({}), provider, {});
        await Promise.all(
          Array.from({ length: 10 }, () =>
            service().webhook({ verifiedFixture: true }),
          ),
        );
        const [row] =
          await db.$queryRaw`SELECT count(*)::int AS count FROM provider_webhook_inbox WHERE event_key=${"payos:" + ref}`;
        assert.equal(row.count, 1);
        await Promise.all([service().inboxTick(), service().inboxTick()]);
        assert.equal(calls, 1);
        await service().webhook({ verifiedFixture: true });
        await service().inboxTick();
        assert.equal(calls, 1);
      },
    );
    await t.test(
      "admin resume is one-shot, no HTTP, preserves lifetime send budget and rejects ambiguous refund",
      async () => {
        const id = randomUUID(),
          requestId = randomUUID(),
          actor = randomUUID();
        const state = {
          ...initialRefundRetry(id),
          phase: "WAITING_FUNDS",
          sendAttempts: 1,
          dispatched: true,
          dispatches: 1,
        };
        await db.payment.create({
          data: {
            id,
            kind: "REFUND",
            paymentCode: "fixture-" + id,
            expectedAmount: 100000,
            expiresAt: new Date(),
            operationState: "NEEDS_REVIEW",
            status: "NEEDS_ADMIN",
            providerObjectId: randomUUID(),
            snapshot: { refundRetry: state },
          },
        });
        const service = new PaymentsService(db, new ConfigService({}), {}, {});
        await Promise.all([
          service.resumeFunds(id, actor, requestId, "Funds added fixture"),
          service.resumeFunds(id, actor, requestId, "Funds added fixture"),
        ]);
        const row = await db.payment.findUniqueOrThrow({ where: { id } });
        assert.equal(row.operationState, "REQUESTED");
        assert.equal(row.snapshot.refundRetry.sendAttempts, 1);
        assert.equal(row.snapshot.refundRetry.key, id + ":retry:2");
        await assert.rejects(
          service.resumeFunds(
            id,
            actor,
            randomUUID(),
            "Must not requeue twice",
          ),
        );
      },
    );
  } finally {
    await db.$disconnect();
  }
});
