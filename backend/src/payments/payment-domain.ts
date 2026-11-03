import {
  createCipheriv,
  createDecipheriv,
  createHash,
  createHmac,
  randomBytes,
} from "node:crypto";
export function ticketQrPayload(id: string, secret: string) {
  if (secret.length < 32) throw new Error("TICKET_QR_SECRET is required");
  return `N8T1:${id}:${createHmac("sha256", secret).update(`N8T1:${id}`).digest("hex")}`;
}
export const tokenHash = (token: string) =>
  createHash("sha256").update(token).digest("hex");
export type Recipient = { bin: string; account: string; name: string };
export function payer(input: {
  counterAccountBankId?: string | null;
  counterAccountNumber?: string | null;
  counterAccountName?: string | null;
}): Recipient | null {
  // VSDC CITAD list + VietQR bank directory: Vietcombank head office -> NAPAS BIN.
  // shortcut: other 8-digit CITAD codes remain NEEDS_REVIEW until their mapping is verified.
  const bankId = input.counterAccountBankId ?? "";
  const bin = bankId === "01203001" ? "970436" : bankId,
    account = input.counterAccountNumber ?? "",
    name = input.counterAccountName?.trim() ?? "";
  return /^\d{6}$/.test(bin) && /^\d{4,34}$/.test(account) && name
    ? { bin, account, name }
    : null;
}
export function sealPaymentData(recipient: unknown, keyHex: string) {
  if (!/^[a-f0-9]{64}$/i.test(keyHex))
    throw new Error("PAYMENT_DATA_ENCRYPTION_KEY is required");
  const iv = randomBytes(12),
    c = createCipheriv("aes-256-gcm", Buffer.from(keyHex, "hex"), iv);
  return Buffer.concat([
    iv,
    c.update(JSON.stringify(recipient)),
    c.final(),
    c.getAuthTag(),
  ]).toString("base64");
}
export function openPaymentData(value: string, keyHex: string): unknown {
  const b = Buffer.from(value, "base64");
  const d = createDecipheriv(
    "aes-256-gcm",
    Buffer.from(keyHex, "hex"),
    b.subarray(0, 12),
  );
  d.setAuthTag(b.subarray(-16));
  return JSON.parse(
    Buffer.concat([d.update(b.subarray(12, -16)), d.final()]).toString(),
  );
}
export function canRefund(
  orderStatus: string,
  ticket: {
    status: string;
    checkedInAt: Date | null;
    comboRedeemedAt: Date | null;
  },
  startAt: Date,
  now: Date,
) {
  return (
    orderStatus === "PAID" &&
    ticket.status === "VALID" &&
    !ticket.checkedInAt &&
    !ticket.comboRedeemedAt &&
    startAt.getTime() - now.getTime() >= 45 * 60_000
  );
}
export const maskAccount = (r: Recipient | null) =>
  r ? `${r.bin} ••••${r.account.slice(-4)}` : null;
export function collectionProblem(
  expected: string,
  received: number,
  pending: boolean,
  expiresAt: Date,
  now: Date,
): "WRONG_AMOUNT" | "LATE_PAYMENT" | null {
  if (!Number.isSafeInteger(received) || BigInt(expected) !== BigInt(received))
    return "WRONG_AMOUNT";
  return !pending || expiresAt <= now ? "LATE_PAYMENT" : null;
}

// payOS MB issues a different virtual receiving account for each signed payment link.
export function collectionAccountMatches(
  account: string,
  physical: string,
  virtual: unknown,
) {
  return (
    account === physical ||
    (typeof virtual === "string" && virtual.length > 0 && account === virtual)
  );
}

export const sealRecipient = (r: Recipient, key: string) =>
  sealPaymentData(r, key);
export const openRecipient = (value: string, key: string): Recipient =>
  openPaymentData(value, key) as Recipient;
