import { BadRequestException } from "@nestjs/common";
export const VOUCHERS = [
  {
    code: "FIRST50",
    title: "Chào bạn mới · Giảm 50%",
    description:
      "Giảm 50% tổng vé và bắp nước, không giới hạn mức giảm. Một lần cho lần mua vé đầu tiên; không cộng dồn.",
    minimum: "0",
    illustration: "welcome",
  },
  {
    code: "CINE20",
    title: "Hẹn hò màn ảnh · Giảm 20.000đ",
    description: "Tổng bill từ 300.000đ. Không cộng dồn voucher khác.",
    minimum: "300000",
    illustration: "cinema",
  },
];
export function voucherPrice(
  seats: string,
  combo: string,
  quantity: number,
  code: string,
) {
  const subtotal = BigInt(seats) + BigInt(combo) * BigInt(quantity);
  if (
    subtotal < 0n ||
    subtotal > 99999999999999n ||
    (code && !VOUCHERS.some((v) => v.code === code))
  )
    throw new BadRequestException({
      code: "VOUCHER_INVALID",
      message: "Voucher hoặc số tiền không hợp lệ.",
    });
  if (code === "CINE20" && subtotal < 300000n)
    throw new BadRequestException({
      code: "VOUCHER_MINIMUM",
      message: "Voucher cần tổng bill từ 300.000đ.",
    });
  const discount =
    code === "FIRST50" ? subtotal / 2n : code === "CINE20" ? 20000n : 0n;
  return {
    subtotal: String(subtotal),
    discount: String(discount),
    totalAmount: String(subtotal - discount),
    bankAmount: String(subtotal - discount),
    creditUsed: "0",
    promotionCode: code,
  };
}
