import type { Payout } from "@payos/node";
import type { Recipient } from "./payment-domain.js";
export const REFUND_RETRY_LIMIT = 4;
export const REFUND_RETRY_DELAY_MS = 180_000;
export type RefundRetry = {
  version: 1;
  cycles: number;
  key: string;
  baseIds: string[];
  dispatched: boolean;
  dispatches: number;
};
export function initialRefundRetry(id: string): RefundRetry {
  return {
    version: 1,
    cycles: 0,
    key: id,
    baseIds: [],
    dispatched: false,
    dispatches: 0,
  };
}
export function readRefundRetry(value: unknown): RefundRetry {
  const s = value as RefundRetry;
  if (
    !s ||
    s.version !== 1 ||
    !Number.isInteger(s.cycles) ||
    s.cycles < 0 ||
    s.cycles > REFUND_RETRY_LIMIT ||
    !Number.isInteger(s.dispatches) ||
    s.dispatches < 0 ||
    s.dispatches > REFUND_RETRY_LIMIT ||
    typeof s.key !== "string" ||
    !s.key ||
    !Array.isArray(s.baseIds) ||
    s.baseIds.length > REFUND_RETRY_LIMIT ||
    !s.baseIds.every((x) => typeof x === "string") ||
    typeof s.dispatched !== "boolean"
  )
    throw new Error("Invalid persisted refund retry state");
  return { ...s, baseIds: [...s.baseIds] };
}
export function refundRetryDecision(
  results: Payout[],
  state: RefundRetry,
  reference: string,
  amount: string,
  recipient: Recipient,
): {
  action: "SUCCESS" | "WAIT" | "SEND" | "REVIEW";
  result?: Payout;
  newAttempt?: boolean;
} {
  if (
    results.length > REFUND_RETRY_LIMIT ||
    new Set(results.map((p) => p.id)).size !== results.length
  )
    return { action: "REVIEW" };
  if (
    results.some(
      (p) =>
        typeof p.id !== "string" ||
        !p.id ||
        p.referenceId !== reference ||
        p.transactions.length !== 1 ||
        p.transactions.some(
          (t) =>
            t.referenceId !== reference ||
            String(t.amount) !== amount ||
            t.toBin !== recipient.bin ||
            t.toAccountNumber !== recipient.account,
        ),
    )
  )
    return { action: "REVIEW" };
  const success = results.filter(
    (p) => p.transactions[0].state === "SUCCEEDED",
  );
  if (success.length > 1) return { action: "REVIEW" };
  if (success.length === 1) return { action: "SUCCESS", result: success[0] };
  if (
    results.some(
      (p) =>
        !["FAILED", "CANCELLED", "REVERSED"].includes(p.transactions[0].state),
    )
  )
    return { action: "WAIT" };
  if (!state.dispatched)
    return results.length === 0 ? { action: "SEND" } : { action: "WAIT" };
  const last = results.filter((p) => !state.baseIds.includes(p.id));
  if (last.length > 1) return { action: "REVIEW" };
  // Only a signed, unfunded failure with no bank transfer permits a new payout key.
  if (last.length === 1) {
    const t = last[0].transactions[0];
    return t.state === "FAILED" &&
      t.errorCode === "PAYOUT_INSUFFICIENT_FUNDS" &&
      !t.reference &&
      !t.transactionDatetime
      ? { action: "SEND", newAttempt: true }
      : { action: "WAIT" };
  }
  // Crash/timeout before recording a response: replay only the exact persisted key/body.
  return { action: "SEND" };
}
export function refundRetryAt(now: Date, retryAfter?: string | null) {
  const seconds =
    retryAfter && /^\d+$/.test(retryAfter) ? Number(retryAfter) : NaN;
  const server = Number.isFinite(seconds)
    ? now.getTime() + seconds * 1000
    : Date.parse(retryAfter ?? "");
  return new Date(
    Math.max(
      now.getTime() + REFUND_RETRY_DELAY_MS,
      Number.isFinite(server) ? server : 0,
    ),
  );
}
