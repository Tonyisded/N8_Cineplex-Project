import { Controller, Get, Res, NotFoundException } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { ApiOperation, ApiTags } from "@nestjs/swagger";
import type { Request, Response, NextFunction } from "express";
import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
export const updateMessage =
  "Phiên bản ứng dụng đã cũ, vui lòng cập nhật lên phiên bản mới.";
export function appRelease(config: ConfigService) {
  return {
    minimumVersionCode: Number(config.get("ANDROID_MIN_VERSION_CODE") ?? 0),
    latestVersionCode: Number(config.get("ANDROID_LATEST_VERSION_CODE") ?? 7),
    downloadUrl: `${config.getOrThrow<string>("PUBLIC_BASE_URL")}/app/android/download`,
    sha256: String(config.get("ANDROID_APK_SHA256") ?? ""),
    message: updateMessage,
  };
}
export function appVersionMiddleware(config: ConfigService) {
  const publicPaths = new Set([
    "/health",
    "/app/version",
    "/app/android/download",
    "/webhooks/payos",
    "/payments/return",
    "/payments/cancel",
    "/auth/verify-email",
    "/auth/reset-password",
  ]);
  return (req: Request, res: Response, next: NextFunction) => {
    const release = appRelease(config);
    const publicAvatar =
      ["GET", "HEAD"].includes(req.method) &&
      /^\/assets\/avatars\/[^/]+\/[^/]+$/.test(req.path);
    if (
      !release.minimumVersionCode ||
      publicAvatar ||
      publicPaths.has(req.path.replace(/\/+$/, ""))
    )
      return next();
    const raw = req.get("X-App-Version-Code") ?? "";
    if (/^[1-9]\d{0,8}$/.test(raw) && Number(raw) >= release.minimumVersionCode)
      return next();
    res.setHeader("Cache-Control", "no-store");
    return res.status(426).json({ code: "APP_UPDATE_REQUIRED", ...release });
  };
}
@ApiTags("App compatibility")
@Controller("app")
export class AppReleaseController {
  constructor(private readonly config: ConfigService) {}
  onModuleInit() {
    const file = this.config.get<string>("ANDROID_APK_PATH");
    if (file) {
      const bytes = readFileSync(file);
      if (bytes[0] !== 0x50 || bytes[1] !== 0x4b)
        throw new Error("Android release must be an APK archive");
      const hash = createHash("sha256").update(bytes).digest("hex");
      if (hash !== this.config.get("ANDROID_APK_SHA256"))
        throw new Error("Android release APK hash mismatch");
    }
  }
  @Get("version")
  @ApiOperation({
    summary: "Minimum supported Android version and verified update APK",
  })
  version(@Res({ passthrough: true }) res: Response) {
    res.setHeader("Cache-Control", "no-store");
    return appRelease(this.config);
  }
  @Get("android/download")
  @ApiOperation({
    summary:
      "Download the published Android APK; user must approve installation",
  })
  download(@Res() res: Response) {
    const path = this.config.get<string>("ANDROID_APK_PATH");
    if (!path) throw new NotFoundException("No APK published");
    res.setHeader(
      "X-APK-SHA256",
      this.config.getOrThrow<string>("ANDROID_APK_SHA256"),
    );
    res.setHeader("Cache-Control", "no-store");
    res.download(
      path,
      `N8-Cineplex-${appRelease(this.config).latestVersionCode}.apk`,
    );
  }
}
