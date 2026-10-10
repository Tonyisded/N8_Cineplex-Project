import { Injectable } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import nodemailer from "nodemailer";

@Injectable()
export class MailService {
  private readonly transport;
  constructor(private readonly config: ConfigService) {
    this.transport = nodemailer.createTransport({
      service: "gmail",
      auth: {
        user: config.getOrThrow<string>("GMAIL_USER"),
        pass: config
          .getOrThrow<string>("GMAIL_APP_PASSWORD")
          .replace(/\s/g, ""),
      },
      logger: false,
      debug: false,
      transactionLog: false,
      connectionTimeout: 10000,
      greetingTimeout: 10000,
      socketTimeout: 15000,
      disableFileAccess: true,
      disableUrlAccess: true,
    });
  }
  async verify() {
    await this.transport.verify();
  }
  async send(
    to: string,
    purpose: "verify" | "reset" | "change-email",
    token: string,
  ) {
    const route = purpose === "reset" ? "reset-password" : "verify-email";
    const link = `${this.config.getOrThrow<string>("PUBLIC_BASE_URL")}/auth/${route}#${encodeURIComponent(token)}`;
    const content = {
      verify: {
        title: "Chào mừng bạn đến với N8 Cineplex",
        subject: "Xác minh email của bạn · N8 Cineplex",
        description:
          "Chỉ còn một bước để hoàn tất đăng ký. Hãy xác minh email để sẵn sàng đặt vé và tận hưởng những bộ phim yêu thích.",
        action: "Xác minh email",
        minutes: 30,
      },
      reset: {
        title: "Đặt lại mật khẩu của bạn",
        subject: "Đặt lại mật khẩu · N8 Cineplex",
        description:
          "Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu tài khoản N8 Cineplex. Nhấn nút bên dưới để chọn mật khẩu mới.",
        action: "Đặt lại mật khẩu",
        minutes: 15,
      },
      "change-email": {
        title: "Xác nhận địa chỉ email mới",
        subject: "Xác nhận email mới · N8 Cineplex",
        description:
          "Bạn vừa yêu cầu thay đổi email tài khoản N8 Cineplex. Hãy xác nhận địa chỉ mới bằng nút bên dưới.",
        action: "Xác nhận email mới",
        minutes: 30,
      },
    }[purpose];
    const safeLink = link.replace(/&/g, "&amp;").replace(/"/g, "&quot;");
    const html = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"></head>
<body style="margin:0;padding:0;background-color:#F5F3F0;font-family:Arial,Helvetica,sans-serif;color:#25232A">
<div style="display:none;max-height:0;overflow:hidden">${content.title}. Liên kết có hiệu lực trong ${content.minutes} phút.</div>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#F5F3F0"><tr><td align="center" style="padding:32px 16px">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:560px;background-color:#ffffff;border-radius:16px">
<tr><td style="padding:28px 32px;background-color:#7B263D;border-radius:16px 16px 0 0;color:#ffffff;font-size:24px;font-weight:bold">N8 <span style="color:#ffffff">CINEPLEX</span></td></tr>
<tr><td style="padding:32px"><h1 style="margin:0 0 20px;font-family:Georgia,'Times New Roman',serif;font-size:28px;line-height:1.35;color:#25232A">${content.title}</h1>
<p style="margin:0 0 24px;font-size:16px;line-height:1.7">${content.description}</p>
<table role="presentation" cellspacing="0" cellpadding="0"><tr><td style="border-radius:12px;background-color:#7B263D"><a href="${safeLink}" style="display:inline-block;padding:16px 24px;color:#ffffff;text-decoration:none;font-size:16px;font-weight:bold;border-radius:12px">${content.action}</a></td></tr></table>
<p style="margin:24px 0 0;font-size:14px;line-height:1.7;color:#625D65">Liên kết có hiệu lực trong <strong>${content.minutes} phút</strong> và chỉ dùng một lần. Trang sẽ yêu cầu bạn xác nhận trước khi thực hiện thao tác.</p>
<p style="margin:16px 0 0;font-size:14px;line-height:1.7;color:#625D65">Nếu nút không mở được, hãy <a href="${safeLink}" style="color:#7B263D">mở trang xác nhận tại đây</a>.</p>
<hr style="border:0;border-top:1px solid #D9D3D6;margin:24px 0">
<p style="margin:0;font-size:13px;line-height:1.7;color:#625D65">Nếu bạn không yêu cầu thao tác này, hãy bỏ qua thư. Không chia sẻ liên kết với người khác. N8 Cineplex không yêu cầu bạn gửi mật khẩu qua email.</p></td></tr></table>
<p style="margin:20px 0 0;font-size:12px;color:#625D65">Email tự động từ N8 Cineplex · Hẹn gặp bạn tại rạp!</p>
</td></tr></table></body></html>`;
    try {
      await this.transport.sendMail({
        from: {
          name: "N8 Cineplex",
          address: this.config.getOrThrow<string>("GMAIL_USER"),
        },
        to,
        subject: content.subject,
        html,
        text: `${content.title}\n\n${content.description}\n\n${content.action}: ${link}\n\nLiên kết có hiệu lực trong ${content.minutes} phút và chỉ dùng một lần.\nNếu không yêu cầu thao tác này, hãy bỏ qua thư. Không chia sẻ liên kết với người khác.`,
      });
      return true;
    } catch {
      return false;
    }
  }
}
