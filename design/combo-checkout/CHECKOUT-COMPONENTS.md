# Cineplex — Combo, Checkout, Pricing và Số dư hoàn vé

## Mở prototype

- `index.html`: giao diện khách hàng, mặc định ở **Bắp nước & Combo**.
- `preview/index.html`: các nút tình huống nằm ngoài iframe khách hàng.
- Route mới: `#combos`, `#checkout`, `#account-entry`, `#refund-credit`, `#credit-transaction`, `#payment-processing`, `#payment-result`, `#ticket`.
- Route cũ vẫn giữ: `#dashboard`, `#movie-detail`, `#showtimes`, `#seat-handoff`; `#seats` là alias. Điểm `#checkout-handoff` cũ chuyển tới combo.

Mở file bằng trình duyệt, không cần backend hoặc cài package. Tất cả ảnh, font và mã QR được lưu cục bộ. Trailer của phần cũ vẫn cần Internet. JavaScript mô phỏng giao dịch trong phiên; tải lại khởi tạo lại dữ liệu.

Mở trực tiếp phần combo tạo context mẫu Couple J1–J2 đã được giữ 30 giây trước đó, còn 09:30. Khi đi từ chọn ghế, module dùng nguyên lựa chọn và deadline sẵn có. Không tạo thêm 10 phút khi chuyển màn hình. Ngày chiếu 16/10/2026 là dữ liệu mẫu kế thừa, không phải lịch hiện hành.

## Cấu trúc source

| Tệp | Vai trò |
|---|---|
| `checkout/checkout.js` | Catalog, giỏ, ưu đãi, pricing, Credit ledger, state machine, view và tương tác |
| `checkout/checkout.css` | Component mới, kế thừa token sẵn có |
| `checkout/assets/` | Ảnh đầu vào và nguồn ảnh |
| `checkout/vendor/qrcode.js` | qrcode-generator 1.4.4, tạo QR offline |
| `preview/commerce-preview.js` | Công cụ tình huống và telemetry riêng của preview |
| `preview/commerce-preview.css` | Bố cục công cụ preview |
| `app.js` | Hook routing, render, modal và khởi tạo module |
| `seats.js` | Hook chia sẻ snapshot, deadline, sự kiện và xác nhận hoàn tất phiên |

Các file/màn hình cũ được bảo toàn; entry/routing/session chỉ thêm kết nối cần thiết. Không sửa `app.css`, `seats.css`, ảnh phim, font, `COMPONENTS.md` hoặc ZIP tham chiếu. Tài liệu cũ mô tả giai đoạn thiết kế trước; tài liệu này mô tả phần mở rộng hiện tại.

## Hệ thiết kế và Android

Giữ nền `#F6F7F9`, surface trắng, chữ `#20222A`, chữ phụ `#5D6470`, đỏ rượu `#8C2945`, nhấn nhẹ `#F5E8EC`, viền `#D9DDE4`. Trạng thái thành công dùng `#246345` trên `#EAF3EE`; cảnh báo dùng màu sẵn có từ chọn ghế. Font Be Vietnam Pro 400/500/600/700; body 16px; nhãn phụ 14px; tiêu đề 18/20/24/28/32px. Spacing bội 4/8; bo góc 8/12/16/24px; icon outline stroke nhất quán.

Một shell Android tồn tại xuyên suốt. Layout header + vùng cuộn + footer theo `.od-screen`; countdown/banner sticky vẫn giữ vị trí trong flow, footer dành chỗ riêng và không che nội dung. Target từ 44px, có focus bàn phím, reduced motion, input cỡ 16px và cuộn vào vùng nhìn khi bàn phím mở. Sheet nằm trong màn hình điện thoại, giữ/trả focus và có đường đóng.

| `data-component` / thành phần | Compose đề xuất | Props / trạng thái |
|---|---|---|
| `ComboProduct` | `ComboProductRow` | product, ảnh, giá, stock, variant, quantity |
| `QuantityStepper` | `QuantityStepper` | 0–9; 0 xóa dòng; disabled khi chạm trần/hết hàng |
| Product options sheet | `ProductOptionsSheet` | vị, kích cỡ, lựa chọn hết hàng và phụ thu hiển thị trước |
| `BookingSummary` | `BookingSummary` | phim, T16, rạp/phòng/suất và ghế nguyên cặp |
| `PriceBreakdown` | `PriceBreakdown` | các dòng tiền từ một đối tượng pricing; mở rộng được |
| `PromoCodeField` | `PromoCodeField` | idle/checking/success/invalid/ineligible |
| `RefundCreditControl` | `RefundCreditControl` | balance, cap, switch, amount, lỗi giới hạn, zero/full/partial |
| `PaymentMethod` | `PaymentMethodGroup` | ví/thẻ; không yêu cầu phương thức ngoài khi số còn trả bằng 0 |
| `HoldCountdown` | `HoldCountdown` | cùng deadline từ ViewModel phiên ghế, không reset |
| `PaymentFooter` | `Scaffold.bottomBar` | số cần trả, disabled/loading/confirm zero |
| `CreditLedgerItem` | `CreditLedgerItem` | posted/pending/failed; amount, orderId, thời gian và lý do |
| `TransactionStatus` | `PaymentResultPanel` | processing/success/failed/unknown/cancelled |
| Ticket summary | `TicketScreen` | snapshot đơn, mã đặt vé, QR mẫu và chi tiết tiền |

Các view là function dùng chung trong `checkout.js`; catalog, bảng giá, giới hạn và dữ liệu ledger tập trung ở đầu module. State `C` là nguồn UI duy nhất của phần commerce; ghế và deadline vẫn thuộc `CineplexSeats`.

## Dữ liệu combo

| Sản phẩm | Thành phần mẫu | Giá chuẩn |
|---|---|---|
| Combo 1 người | 1 bắp vừa, 1 nước | 79.000đ |
| Combo 2 người | 1 bắp lớn, 2 nước | 100.000đ |
| Bắp riêng | Chọn vị và kích cỡ | 49.000đ |
| Nước riêng | Ly 500ml | 35.000đ |

Vị bơ/caramel có cùng giá chuẩn. Phô mai là tùy chọn hết hàng. Bắp riêng cỡ lớn phụ thu mẫu 10.000đ, hiển thị ngay trong sheet. Combo 2 người chuẩn lớn vẫn giá 100.000đ. Đây là cấu hình minh họa, không khẳng định catalog kinh doanh thực tế. Ảnh chỉ minh họa sản phẩm; thành phần/khối lượng theo phần chữ.

Chọn biến thể trước lần thêm đầu. Tăng/giảm sau đó dùng chính biến thể đã chọn. “Đổi tùy chọn” thay biến thể và tính lại giá. Bỏ qua khi đã chọn combo có xác nhận xóa combo. Quay về từ checkout giữ nguyên giỏ.

## Pricing và ví dụ xuyên suốt

Tất cả giá trị là số nguyên VND, format bằng locale `vi-VN` tại UI.

```
subtotal = seatTotal + comboTotal + fee
afterDiscount = max(0, subtotal - validDiscount)
creditUsed <= min(availableBalance, afterDiscount)
amountDue = afterDiscount - creditUsed
```

| Khoản | Giá trị |
|---|---:|
| Couple J1–J2, 1 cặp, 2 chỗ | 240.000đ |
| Combo 2 người × 1 | 100.000đ |
| Phí dịch vụ | 0đ |
| Tổng trước giảm | 340.000đ |
| Mã CINE20 | −20.000đ |
| Tổng sau giảm | 320.000đ |
| Credit khả dụng trước đặt | 150.000đ |
| Credit dùng | −100.000đ |
| Thanh toán ngoài | **220.000đ** |
| Credit còn lại sau thành công | **50.000đ** |

Ghế thường 90.000đ/chỗ, VIP 120.000đ/chỗ, Couple 240.000đ/cặp gồm hai mã. Giá Couple không nhân hai lần. Tất cả khoản phí xuất hiện trong pricing, không thêm phí tại lúc xử lý.

Mã mẫu `CINE20` giảm 20.000đ cho subtotal từ 300.000đ. Điều kiện là cấu hình minh họa để thể hiện đủ/không đủ điều kiện. Mã khác hiển thị không hợp lệ; kiểm tra có độ trễ 700ms. Đổi giỏ/ghế kiểm tra lại điều kiện và cap Credit.

## Credit và ledger

“Số dư hoàn vé” là Credit nội bộ, không hoàn trực tiếp về ngân hàng và không có nút rút tiền. Bật switch mặc định áp dụng tối đa `min(balance, afterDiscount)`; ví dụ chính chỉnh xuống 100.000đ. Giá trị âm, không nguyên hoặc vượt cap giữ ở input, hiện lỗi gần trường và khóa thanh toán. Balance 0 khóa switch. Khi giỏ làm cap giảm, UI điều chỉnh mức áp dụng và thông báo.

Balance được tính từ các bút toán đã posted, không lấy số hiển thị riêng:

- Khoản hoàn đã cộng: +200.000đ.
- Khoản dùng cũ: −50.000đ.
- Balance mở đầu: 150.000đ.
- Khoản dùng của ví dụ chính sau thành công: −100.000đ.
- Balance còn lại: 50.000đ.

Khoản hoàn đang xử lý 80.000đ và thất bại 50.000đ chỉ minh họa trạng thái, không cộng vào balance. Chi tiết khoản đã cộng có đề nghị 210.000đ, khấu trừ mẫu 10.000đ và thực nhận 200.000đ. UI ghi rõ điều kiện/khấu trừ là dữ liệu mẫu có thể cấu hình; không đặt chính sách hoàn vé thực tế.

Không tạm trừ Credit trước thành công trong mô phỏng này. Failure/cancel giữ nguyên ledger. Unknown chờ kiểm tra, không tự ghi giảm hoặc tự ghi hoàn. Commit theo `orderId`, dùng `Set` chống ghi trùng; mỗi đơn chỉ tạo tối đa một dòng sử dụng Credit. Kiểm tra lại không tạo lần thanh toán mới.

## State machine thanh toán

| State | UI / hành động |
|---|---|
| idle | Chỉnh thông tin, combo, ưu đãi, Credit, phương thức; kiểm tra phiên trước khi trả |
| processing | Snapshot bất biến; khóa bấm lặp; progress và thông tin đơn; có hủy mô phỏng |
| success | Ghi đơn và Credit một lần; đóng phiên giữ ghế, QR/vé dùng snapshot |
| failed | Lý do, Credit giữ nguyên; thử lại chỉ khi còn phiên hợp lệ |
| unknown | “Đang kiểm tra giao dịch”; không tạo thanh toán mới; “Kiểm tra lại” dùng cùng định danh |
| cancelled | Về checkout khi còn hạn; không trừ Credit; hết hạn thì chọn lại ghế |

Snapshot gồm booking, từng entity ghế và giá, dòng combo/variant/quantity, pricing, thông tin nhận vé, phương thức, balance trước thanh toán, `orderId` và `attemptId`. Kết quả/vé không tính lại theo giỏ đang thay đổi.

Hết hạn **trước** xử lý: xóa quyền tiếp tục, hiện thông báo và về chọn ghế. Hết hạn **trong** xử lý/unknown: giữ snapshot và chuyển unknown; callback cũ bị vô hiệu hóa. Kiểm tra giao dịch sau deadline vẫn có thể trả thành công, vì đồng hồ cục bộ không quyết định kết quả giao dịch. Nếu kết quả xác nhận thất bại sau khi hết hạn thì cần chọn lại ghế.

Mất mạng khóa tiếp tục; đang trả tiền mà chưa nhận kết quả chuyển unknown. Kết nối lại không tự gửi thêm thanh toán. Đổi ghế có giải thích tính lại giá. Rời toàn luồng khi giữ ghế có xác nhận; giao dịch unknown yêu cầu kiểm tra trước khi bắt đầu đơn khác.

Thông tin nhận vé là dữ liệu mẫu chỉnh sửa được; lỗi validate on blur giữ input, có summary liên kết trường nếu nhiều lỗi. Không thu thập thông tin thẻ thật. “Thông tin vé gửi tới…” trên kết quả là trạng thái mô phỏng, không có email được gửi thật.

## Preview và cách xem các tình huống

Trong `preview/index.html`, phần **Combo · Checkout · Credit** nằm ngoài điện thoại. App thường không nhận lệnh tình huống; iframe cần `?preview=1`, đúng parent, cùng origin và kênh được định danh. Tên action theo allowlist.

1. Chọn **Nạp ví dụ còn trả 220.000đ** để vào checkout với các số liệu trong bảng.
2. Bấm **Thanh toán 220.000đ** trong điện thoại. Mặc định giao dịch mô phỏng thành công sau 1,6 giây. Balance còn 50.000đ và ledger có đúng một dòng cho đơn.
3. Trước khi trả tiền, chọn **Thất bại**, **Chưa xác định kết quả** hoặc **Người dùng hủy** để đặt kết quả cho lần tiếp theo. Nếu đang có giao dịch chờ, các nút kết quả giải quyết giao dịch đó.
4. Unknown: dùng **Kiểm tra lại** trên điện thoại hoặc nút preview đối soát thành công/thất bại. Không bấm trả lần nữa.
5. **Hết giờ khi đang xử lý** tự nạp ví dụ, bắt đầu giao dịch và đưa deadline về gần hết; quan sát trạng thái kiểm tra giao dịch, sau đó đối soát.
6. **Credit đủ toàn bộ** nạp ghế thường A3 90.000đ, không combo, dùng 90.000đ từ balance 150.000đ: CTA là **Xác nhận đặt vé**, còn trả 0đ.
7. **Số dư 0đ và lịch sử trống** minh họa empty/disabled. **Nạp ví dụ** khởi tạo lại ledger mẫu.
8. Dùng công cụ ghế đã có để mô phỏng mất mạng/kết nối lại, ghế thay đổi và hết hạn trước trả tiền.

Nạp ví dụ là reset dữ liệu thử, chỉ có trong preview; có thể thay balance/ledger của phiên mẫu. Không phải nghiệp vụ cộng Credit của khách hàng.

## Tài sản và giới hạn bàn giao

Ảnh đầu vào: combo 800×534, bắp 800×1067, nước 600×900; hiển thị toàn khung theo tỷ lệ nội tại, không ép tỷ lệ thumbnail. Nguồn ở `checkout/assets/sources.json`. Poster/font kế thừa tài sản cũ. QR tạo từ chuỗi `CINEPLEX-DEMO:<orderId>` và ghi rõ không có giá trị soát vé. Thư viện qrcode-generator 1.4.4 được lưu cục bộ từ `https://cdn.jsdelivr.net/npm/qrcode-generator@1.4.4/qrcode.js`, giữ nguyên header nguồn.

CLI đọc hướng dẫn media trả nội dung rỗng; tài sản được thu nhận theo quy tắc nguồn ảnh thật và lưu cục bộ của kế hoạch. Không sử dụng hướng dẫn chưa đọc được.

Không backend, giữ ghế, thanh toán, đối soát, gửi email hoặc hoàn tiền thật. Tải lại có thể tạo lại phiên và ledger mẫu. Theo quy trình bàn giao, không mở preview, render hoặc chạy kiểm thử sau khi ghi deliverable.
