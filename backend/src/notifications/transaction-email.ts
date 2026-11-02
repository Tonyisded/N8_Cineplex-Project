export type TicketSnapshot = {
  movieTitle: string;
  posterUrl?: string | null;
  startAt: string;
  format: string;
  auditorium: string;
  seatCodes: string[];
  seats: { seatCode: string; price: string }[];
  comboName?: string | null;
  comboQuantity: number;
  comboUnitPrice: string;
  subtotal: string;
  discount: string;
  totalAmount: string;
  voucherCode?: string | null;
  email: string;
  bookingCode: string;
  ticketId: string;
};
export type TransactionEmail = {
  subject: string;
  html: string;
  text: string;
  messageId: string;
  attachments?: {
    filename: string;
    content: Buffer;
    cid: string;
    contentType: string;
  }[];
};
export const escapeHtml = (s: unknown) =>
  String(s ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ]!,
  );
const money = (v: string) =>
  /^\d{1,14}$/.test(v) ? `${BigInt(v).toLocaleString("vi-VN")} đ` : "—";
const date = (s: string) =>
  new Date(s).toLocaleString("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    dateStyle: "full",
    timeStyle: "short",
  });
function poster(url?: string | null) {
  try {
    const u = new URL(url ?? "");
    return u.protocol === "https:" && !u.username && !u.password
      ? u.href
      : undefined;
  } catch {
    return undefined;
  }
}
const row = (label: string, value: string, strong = false) =>
  `<tr><td style="padding:10px 0;border-bottom:1px solid #E9E4E6;color:#625D65">${escapeHtml(label)}</td><td align="right" style="padding:10px 0;border-bottom:1px solid #E9E4E6;${strong ? "font-size:22px;font-weight:bold;color:#7B263D;white-space:nowrap" : "color:#25232A;overflow-wrap:anywhere"}">${escapeHtml(value)}</td></tr>`;
function layout(title: string, description: string, body: string) {
  return `<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head><body style="margin:0;background:#F5F3F0;font-family:Arial,Helvetica,sans-serif;color:#25232A"><div style="display:none;max-height:0;overflow:hidden">${escapeHtml(description)}</div><table role="presentation" width="100%" cellspacing="0" cellpadding="0"><tr><td align="center" style="padding:24px 12px"><table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:600px;background:white;border-radius:16px"><tr><td style="background:#7B263D;color:white;padding:24px;border-radius:16px 16px 0 0;font-size:24px;font-weight:bold">N8 CINEPLEX</td></tr><tr><td style="padding:24px"><p style="margin:0 0 8px;color:#7B263D;font-size:12px;letter-spacing:1px">THÔNG TIN GIAO DỊCH</p><h1 style="font-size:26px;line-height:1.3;margin:0 0 12px">${escapeHtml(title)}</h1><p style="font-size:15px;line-height:1.7;color:#625D65;margin:0 0 24px">${escapeHtml(description)}</p>${body}<hr style="border:0;border-top:1px solid #E9E4E6;margin:24px 0"><p style="font-size:13px;line-height:1.7;color:#625D65;margin:0">Mở ứng dụng N8 Cineplex → Vé của tôi để xem trạng thái mới nhất và liên hệ hỗ trợ theo mã vé/đơn. Không chia sẻ QR với người khác. Đây là xác nhận giao dịch, không phải hóa đơn VAT.</p></td></tr></table><p style="font-size:12px;color:#625D65">N8 Cineplex · Hẹn gặp bạn tại rạp!</p></td></tr></table></body></html>`;
}
function movie(s: TicketSnapshot) {
  const image = poster(s.posterUrl);
  return `<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:20px"><tr>${image ? `<td width="96" valign="top" style="padding-right:16px"><img src="${escapeHtml(image)}" width="96" alt="Poster ${escapeHtml(s.movieTitle)}" style="display:block;width:96px;max-width:96px;border-radius:8px;border:0"></td>` : ""}<td valign="top"><h2 style="font-size:20px;line-height:1.4;margin:0 0 10px">${escapeHtml(s.movieTitle)}</h2><p style="font-size:14px;line-height:1.7;margin:0">${escapeHtml(date(s.startAt))}<br>${escapeHtml(s.format)} · ${escapeHtml(s.auditorium)}<br>Ghế: <strong>${escapeHtml(s.seatCodes.join(", "))}</strong><br>${s.seatCodes.length} ghế · 1 vé điện tử</p></td></tr></table>`;
}
export function purchaseEmail(
  s: TicketSnapshot,
  qrPng: Buffer,
  usable = true,
): TransactionEmail {
  const groups = new Map<string, number>();
  for (const seat of s.seats)
    groups.set(seat.price, (groups.get(seat.price) ?? 0) + 1);
  const lines = [...groups].map(([price, n]) =>
    row(`Vé ${money(price)} × ${n}`, money(String(BigInt(price) * BigInt(n)))),
  );
  if (s.comboQuantity > 0)
    lines.push(
      row(
        `${s.comboName ?? "Combo"} × ${s.comboQuantity} (${money(s.comboUnitPrice)}/phần)`,
        money(String(BigInt(s.comboUnitPrice) * BigInt(s.comboQuantity))),
      ),
    );
  lines.push(
    row("Tạm tính", money(s.subtotal)),
    row(
      `Giảm giá${s.voucherCode ? ` · ${s.voucherCode}` : ""}`,
      `−${money(s.discount)}`,
    ),
    row("Tổng đã thanh toán", money(s.totalAmount), true),
  );
  const title = "Đặt vé thành công";
  const description = usable
    ? "Vé của bạn đã sẵn sàng. Cảm ơn bạn đã chọn N8 Cineplex!"
    : "Đơn đã được mua thành công, nhưng vé hiện không còn hiệu lực sử dụng. Xem trạng thái và tiến trình hoàn tiền trong ứng dụng.";
  const content = `${movie(s)}<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="font-size:14px">${lines.join("")}</table><div style="margin-top:24px;padding:20px;background:#F8F4F5;border-radius:12px;text-align:center"><p style="font-size:13px;margin:0 0 6px">MÃ ĐẶT VÉ</p><p style="font-size:22px;font-weight:bold;letter-spacing:2px;color:#7B263D;margin:0 0 16px">${escapeHtml(s.bookingCode)}</p>${usable ? `<img src="cid:ticket-qr" width="240" height="240" alt="QR vé; mã đặt vé ${escapeHtml(s.bookingCode)}" style="display:block;margin:0 auto;max-width:100%;background:white">` : `<p style="font-size:18px;font-weight:bold;color:#7B263D">VÉ KHÔNG CÒN HIỆU LỰC</p>`}<p style="font-size:13px;line-height:1.7;color:#625D65;margin:16px 0 0">Xuất trình QR khi sử dụng vé. QR không còn hợp lệ nếu vé đã được hoàn, hủy hoặc hết hạn.</p></div><p style="font-size:13px;line-height:1.7;color:#625D65">Có thể yêu cầu hoàn toàn đơn khi chưa sử dụng vé/chưa nhận combo và còn ít nhất 45 phút trước giờ chiếu. Điều kiện và số tiền cuối cùng do hệ thống kiểm tra.</p>`;
  return {
    subject: `[N8 Cineplex] Đặt vé thành công · ${s.bookingCode}`,
    html: layout(title, description, content),
    text: `${title}\n${description}\n${s.movieTitle}\n${date(s.startAt)} · ${s.format} · ${s.auditorium}\nGhế: ${s.seatCodes.join(", ")} (${s.seatCodes.length} ghế / 1 QR)\n${s.seats.map((x) => `${x.seatCode}: ${money(x.price)}`).join("\n")}\nCombo: ${s.comboName ?? "Không"} × ${s.comboQuantity}\nTạm tính: ${money(s.subtotal)}\nVoucher: ${s.voucherCode ?? "Không"} · Giảm: ${money(s.discount)}\nTổng đã thanh toán: ${money(s.totalAmount)}\nMã vé: ${s.bookingCode}\nMở ứng dụng → Vé của tôi để xem QR và điều kiện hoàn.`,
    messageId: `<ticket-${s.ticketId}@n8-cineplex.local>`,
    attachments: usable
      ? [
          {
            filename: "N8-Cineplex-ticket.png",
            content: qrPng,
            cid: "ticket-qr",
            contentType: "image/png",
          },
        ]
      : [],
  };
}
export function refundEmail(
  s: TicketSnapshot,
  event: "requested" | "completed" | "review",
  refund: {
    id: string;
    amount: string;
    recipientLabel?: string;
    reference?: string;
    completedAt?: string;
  },
): TransactionEmail {
  const content = {
    requested: {
      title: "Đã tiếp nhận yêu cầu hoàn vé",
      description:
        "Vé đã mất hiệu lực sử dụng. Tiền hoàn đang được xử lý; đây chưa phải xác nhận tiền đã về tài khoản.",
    },
    completed: {
      title: "Hoàn tiền thành công",
      description:
        "Giao dịch hoàn tiền đã được xác nhận thành công. Vui lòng đối chiếu tài khoản nhận tiền; liên hệ hỗ trợ nếu cần tra soát.",
    },
    review: {
      title: "Yêu cầu hoàn cần được kiểm tra",
      description:
        "Vé đã mất hiệu lực, nhưng việc hoàn tiền chưa hoàn tất. Yêu cầu và nghĩa vụ hoàn vẫn được lưu để tiếp tục xử lý; không cần tạo yêu cầu mới.",
    },
  }[event];
  const label =
    event === "completed" ? "Số tiền đã hoàn" : "Số tiền yêu cầu hoàn";
  const details = [
    row("Mã vé", s.bookingCode),
    row(label, money(refund.amount), true),
    row(
      "Tài khoản nhận",
      refund.recipientLabel ?? "Nguồn thanh toán gốc · đang xác minh",
    ),
    row("Mã yêu cầu", refund.id),
  ];
  if (refund.reference) details.push(row("Tham chiếu", refund.reference));
  if (event === "completed" && refund.completedAt)
    details.push(row("Thời điểm hoàn", date(refund.completedAt)));
  return {
    subject: `[N8 Cineplex] ${content.title} · ${s.bookingCode}`,
    html: layout(
      content.title,
      content.description,
      `${movie(s)}<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="font-size:14px">${details.join("")}</table><p style="font-size:13px;line-height:1.7;color:#625D65">Voucher không được quy đổi thành tiền. QR cũ không còn giá trị sử dụng.</p>`,
    ),
    text: `${content.title}\n${content.description}\n${s.movieTitle} · ${s.bookingCode}\n${label}: ${money(refund.amount)}\nTài khoản nhận: ${refund.recipientLabel ?? "Nguồn thanh toán gốc · đang xác minh"}\nMã yêu cầu: ${refund.id}\nTham chiếu: ${refund.reference ?? "Đang cập nhật"}\nMở ứng dụng → Vé của tôi để xem trạng thái mới nhất.`,
    messageId: `<refund-${refund.id}-${event}@n8-cineplex.local>`,
  };
}
