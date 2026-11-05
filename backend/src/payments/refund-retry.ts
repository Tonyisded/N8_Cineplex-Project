import type { Payout } from "@payos/node";
import type { Recipient } from "./payment-domain.js";
export const REFUND_RETRY_LIMIT = 4;
export const REFUND_RETRY_DELAY_MS = 180_000;
export type RefundRetry = {
  version: 1 | 2 | 3;
  phase?:
    "READY" | "DISPATCHING" | "AWAITING_RESULT" | "WAITING_FUNDS" | "DONE";
  requestVersion?: number;
  requestHash?: string;
  requestCipher?: string;
  pollStep?: number;
  resumeRequestId?: string;
  resumeRequests?: { id: string; hash: string }[];
  sendAttempts?: number;
  reconcileOnly?: boolean;
  firstError?: RefundErrorEvent;
  lastError?: RefundErrorEvent;
  errors?: RefundErrorEvent[];
  cycles: number;
  key: string;
  baseIds: string[];
  dispatched: boolean;
  dispatches: number;
};
export function initialRefundRetry(id: string): RefundRetry {
  return {
    version: 3,
    phase: "READY",
    requestVersion: 1,
    pollStep: 0,
    sendAttempts: 0,
    reconcileOnly: false,
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
    ![1, 2, 3].includes(s.version) ||
    (s.version === 3 &&
      (!s.phase ||
        ![
          "READY",
          "DISPATCHING",
          "AWAITING_RESULT",
          "WAITING_FUNDS",
          "DONE",
        ].includes(s.phase) ||
        s.requestVersion !== 1 ||
        !Number.isInteger(s.pollStep) ||
        s.pollStep! < 0 ||
        s.pollStep! > 3)) ||
    !Number.isSafeInteger(s.cycles) ||
    s.cycles < 0 ||
    (s.sendAttempts !== undefined &&
      (!Number.isInteger(s.sendAttempts) ||
        s.sendAttempts < 0 ||
        s.sendAttempts > REFUND_RETRY_LIMIT)) ||
    (s.reconcileOnly !== undefined && typeof s.reconcileOnly !== "boolean") ||
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
  return {
    ...s,
    version: s.version,
    sendAttempts:
      s.sendAttempts ??
      (s.dispatched ? Math.min(s.cycles, REFUND_RETRY_LIMIT) : 0),
    reconcileOnly: s.reconcileOnly ?? false,
    baseIds: [...s.baseIds],
  };
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

export function safeProviderCode(value: unknown): string | undefined {
  const code =
    typeof value === "string" || typeof value === "number" ? String(value) : "";
  return /^\d{1,6}$/.test(code) ||
    [
      "PAYOUT_INSUFFICIENT_FUNDS",
      "PAYOUT_BANK_SYSTEM_ERROR",
      "PAYOUT_VALIDATION",
    ].includes(code)
    ? code
    : undefined;
}
export function safeTransportCode(error: unknown): string | undefined {
  let current = error as {
    transportCode?: unknown;
    code?: unknown;
    cause?: unknown;
  } | null;
  for (let i = 0; current && i < 4; i++) {
    for (const code of [current.transportCode, current.code]) {
      if (
        typeof code === "string" &&
        [
          "ENOTFOUND",
          "EAI_AGAIN",
          "ECONNREFUSED",
          "ECONNRESET",
          "ETIMEDOUT",
          "EHOSTUNREACH",
          "ENETUNREACH",
          "EPIPE",
          "UND_ERR_CONNECT_TIMEOUT",
          "UND_ERR_HEADERS_TIMEOUT",
          "UND_ERR_BODY_TIMEOUT",
          "UND_ERR_SOCKET",
          "UND_ERR_ABORTED",
          "ABORT_ERR",
          "CERT_HAS_EXPIRED",
          "DEPTH_ZERO_SELF_SIGNED_CERT",
          "UNABLE_TO_VERIFY_LEAF_SIGNATURE",
          "ERR_TLS_CERT_ALTNAME_INVALID",
        ].includes(code)
      )
        return code;
    }
    current = current.cause as typeof current;
  }
  return undefined;
}

export type RefundErrorEvent = {
  at: string;
  phase: string;
  elapsedMs: number;
  cycle: number;
  sendAttempts: number;
  kind: string;
  httpStatus?: number;
  providerCode?: string;
  transportCode?: string;
  signaturePresent?: boolean;
};
export function safeRefundError(
  error: unknown,
  phase: string,
  now: Date,
  elapsedMs: number,
  cycle: number,
  sendAttempts: number,
): RefundErrorEvent {
  const e = error as {
    status?: unknown;
    code?: unknown;
    signaturePresent?: unknown;
    constructor?: { name?: string };
  } | null;
  const kind = e?.constructor?.name ?? "Error";
  const code = safeProviderCode(e?.code);
  const transportCode = safeTransportCode(error);
  return {
    at: now.toISOString(),
    phase: ["query", "send", "provider", "validation", "finalize"].includes(
      phase,
    )
      ? phase
      : "query",
    elapsedMs: Math.min(120000, Math.max(0, Math.round(elapsedMs))),
    cycle,
    sendAttempts,
    kind: [
      "APIError",
      "BadRequestError",
      "NotFoundError",
      "UserAbortError",
      "ConnectionError",
      "TimeoutError",
      "ConnectionTimeoutError",
      "TooManyRequestError",
      "InvalidSignatureError",
      "UnauthorizedError",
      "ForbiddenError",
      "SignatureError",
      "Error",
      "TypeError",
    ].includes(kind)
      ? kind
      : "Error",
    ...(Number.isInteger(e?.status) &&
    Number(e?.status) >= 100 &&
    Number(e?.status) <= 599
      ? { httpStatus: Number(e?.status) }
      : {}),
    ...(code ? { providerCode: code } : {}),
    ...(transportCode ? { transportCode } : {}),
    ...(typeof e?.signaturePresent === "boolean"
      ? { signaturePresent: e.signaturePresent }
      : {}),
  };
}

export function sparseRefundAt(
  now: Date,
  step: number,
  retryAfter?: string | null,
) {
  const base = [180, 600, 1800, 3600][Math.min(Math.max(step, 0), 3)];
  return new Date(
    Math.max(
      refundRetryAt(now, retryAfter).getTime(),
      now.getTime() +
        Math.min(3600, base + Math.floor(Math.random() * 11)) * 1000,
    ),
  );
}
