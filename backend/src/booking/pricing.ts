import { createHash, createHmac, timingSafeEqual } from "node:crypto";
import { BadRequestException } from "@nestjs/common";

export const fingerprint = (value: unknown) =>
  createHash("sha256").update(JSON.stringify(value)).digest("hex");
export function signQuote(value: unknown, secret: string) {
  const body = Buffer.from(JSON.stringify(value)).toString("base64url");
  return (
    body +
    "." +
    createHmac("sha256", secret)
      .update("checkout-quote:" + body)
      .digest("base64url")
  );
}
export function readQuote(token: string, secret: string): any {
  try {
    const [body, signature, extra] = token.split(".");
    const expected = createHmac("sha256", secret)
      .update("checkout-quote:" + body)
      .digest();
    const actual = Buffer.from(signature, "base64url");
    if (
      extra ||
      actual.length !== expected.length ||
      !timingSafeEqual(actual, expected)
    )
      throw Error();
    return JSON.parse(Buffer.from(body, "base64url").toString());
  } catch {
    throw new BadRequestException({
      code: "INVALID_QUOTE",
      message: "Báo giá không hợp lệ.",
    });
  }
}
