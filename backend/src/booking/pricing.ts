import { createHash, createHmac, timingSafeEqual } from "node:crypto";
import { BadRequestException } from "@nestjs/common";

export function calculatePrice(
  seats: string,
  combo: string,
  quantity: number,
  promotion: string,
  balance: string,
) {
  const subtotal = BigInt(seats) + BigInt(combo) * BigInt(quantity);
  const code = promotion.trim().toUpperCase();
  if (code && code !== "CINE20")
    throw new BadRequestException({
      code: "PROMOTION_INVALID",
      message: "Mã ưu đãi không hợp lệ.",
    });
  const discount = code === "CINE20" && subtotal >= 300000n ? 20000n : 0n;
  const total = subtotal - discount;
  const credit = BigInt(balance) < total ? BigInt(balance) : total;
  if (subtotal < 0n || subtotal > 99999999999999n || credit < 0n)
    throw new BadRequestException({
      code: "AMOUNT_INVALID",
      message: "Số tiền vượt giới hạn.",
    });
  return {
    subtotal: String(subtotal),
    discount: String(discount),
    totalAmount: String(total),
    creditUsed: String(credit),
    bankAmount: String(total - credit),
    promotionCode: discount ? code : "",
  };
}
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
