import { Injectable } from "@nestjs/common";
import { randomUUID } from "node:crypto";
import { PrismaService } from "../prisma.service.js";

export class ProviderDeferred extends Error {
  constructor(public readonly until: Date) {
    super("Provider call deferred");
  }
}
const waits = [180, 360, 720, 1800, 3600];
export function providerBackoff(
  streak: number,
  now: Date,
  retryAfter?: string | null,
) {
  const parsed =
    retryAfter && /^\d+$/.test(retryAfter)
      ? now.getTime() + Number(retryAfter) * 1000
      : Date.parse(retryAfter ?? "");
  return new Date(
    Math.max(
      now.getTime() + waits[Math.min(Math.max(streak - 1, 0), 4)] * 1000,
      Number.isFinite(parsed) ? parsed : 0,
    ),
  );
}
@Injectable()
export class ProviderGovernor {
  constructor(private readonly db: PrismaService) {}
  async execute<T>(
    key: string,
    operation: string,
    interval: number,
    call: () => Promise<T>,
  ): Promise<T> {
    const token = randomUUID();
    const claim = await this.db.$transaction(async (tx) => {
      await tx.$executeRaw`INSERT INTO provider_api_governor(channel_key) VALUES (${key}) ON CONFLICT DO NOTHING`;
      const [row] = await tx.$queryRaw<{ ready: boolean; until: Date }[]>`
        SELECT (next_allowed_at<=clock_timestamp() AND blocked_until<=clock_timestamp()
          AND lease_until<=clock_timestamp()) AS ready,
          GREATEST(next_allowed_at,blocked_until,lease_until) AS until
        FROM provider_api_governor WHERE channel_key=${key} FOR UPDATE`;
      if (!row.ready) return row;
      await tx.$executeRaw`UPDATE provider_api_governor SET lease_token=${token}::uuid,
        lease_until=clock_timestamp()+interval '60 seconds' WHERE channel_key=${key}`;
      return row;
    });
    if (!claim.ready) throw new ProviderDeferred(claim.until);
    let httpStatus: number | undefined;
    let limited = false,
      retryAfter: string | null = null,
      succeeded = false;
    const started = Date.now();
    try {
      const result = await call();
      succeeded = true;
      return result;
    } catch (error) {
      const e = error as { status?: number; headers?: Headers };
      httpStatus = e?.status;
      limited = e?.status === 429;
      retryAfter = e?.headers?.get?.("retry-after") ?? null;
      throw error;
    } finally {
      // Persist cooldown before releasing the shared lease; GET must not clear POST's streak.
      await this.db.$transaction(async (tx) => {
        const rows = await tx.$queryRaw<
          { streaks: Record<string, number>; now: Date }[]
        >`
          SELECT streaks,clock_timestamp() AS now FROM provider_api_governor
          WHERE channel_key=${key} AND lease_token=${token}::uuid FOR UPDATE`;
        if (!rows.length) throw new Error("Provider lease lost");
        const { streaks, now } = rows[0];
        if (limited) streaks[operation] = (streaks[operation] ?? 0) + 1;
        else if (succeeded) streaks[operation] = 0;
        const block = limited
          ? providerBackoff(streaks[operation], now, retryAfter)
          : now;
        await tx.$executeRaw`UPDATE provider_api_governor SET streaks=${JSON.stringify(streaks)}::jsonb,
          blocked_until=GREATEST(blocked_until,${block}),
          next_allowed_at=clock_timestamp()+${interval}*interval '1 millisecond',
          lease_until=clock_timestamp(),lease_token=NULL
          WHERE channel_key=${key} AND lease_token=${token}::uuid`;
        console.info(
          "provider_call",
          JSON.stringify({
            operation,
            httpStatus,
            limited,
            succeeded,
            elapsedMs: Date.now() - started,
            blockedUntil: limited ? block.toISOString() : undefined,
          }),
        );
      });
    }
  }
}
