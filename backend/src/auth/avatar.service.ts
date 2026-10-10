import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
} from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { randomUUID } from "node:crypto";
import { mkdir, readFile, unlink, writeFile } from "node:fs/promises";
import path from "node:path";
import sharp from "sharp";
import { PrismaService } from "../prisma.service.js";
import type { User } from "../generated/prisma/client.js";
import { profile } from "./auth.service.js";

export async function normalizeAvatar(bytes: Buffer) {
  if (!bytes.length || bytes.length > 5 * 1024 * 1024)
    throw new BadRequestException({
      code: "INVALID_AVATAR",
      message: "Ảnh phải nhỏ hơn hoặc bằng 5 MB.",
    });
  try {
    const image = sharp(bytes, {
      limitInputPixels: 16_000_000,
      animated: false,
      failOn: "warning",
    });
    const meta = await image.metadata();
    if (
      !["jpeg", "png", "webp"].includes(meta.format ?? "") ||
      (meta.pages ?? 1) > 1
    )
      throw new Error();
    return await image
      .rotate()
      .resize(512, 512, { fit: "inside", withoutEnlargement: true })
      .webp({ quality: 85 })
      .toBuffer();
  } catch {
    throw new BadRequestException({
      code: "INVALID_AVATAR",
      message: "Chỉ nhận ảnh JPEG/PNG/WebP hợp lệ, không quá 16 megapixel.",
    });
  }
}
@Injectable()
export class AvatarService {
  constructor(
    private readonly db: PrismaService,
    private readonly config: ConfigService,
  ) {}
  private file(userId: string, name: string) {
    if (
      !/^[a-f0-9-]{36}$/i.test(userId) ||
      !/^[a-f0-9-]{36}\.webp$/i.test(name)
    )
      throw new NotFoundException();
    return path.join(
      this.config.getOrThrow<string>("AVATAR_UPLOAD_DIR"),
      userId,
      name,
    );
  }
  async read(userId: string, name: string) {
    try {
      return await readFile(this.file(userId, name));
    } catch {
      throw new NotFoundException();
    }
  }
  private async deleteManaged(userId: string, url: string | null) {
    const prefix = `${this.config.getOrThrow<string>("PUBLIC_BASE_URL")}/assets/avatars/${userId}/`;
    if (!url?.startsWith(prefix)) return;
    const name = url.slice(prefix.length);
    if (
      !/^[a-f0-9-]{36}\.webp$/i.test(name) ||
      (await this.db.user.count({ where: { avatarUrl: url } }))
    )
      return;
    await unlink(this.file(userId, name)).catch((e: NodeJS.ErrnoException) => {
      if (e.code !== "ENOENT") throw e;
    });
  }
  async upload(u: User, file?: Express.Multer.File) {
    if (
      !file ||
      !["image/jpeg", "image/png", "image/webp"].includes(file.mimetype)
    )
      throw new BadRequestException({
        code: "INVALID_AVATAR",
        message: "Hãy chọn ảnh JPEG/PNG/WebP.",
      });
    const bytes = await normalizeAvatar(file.buffer),
      name = `${randomUUID()}.webp`,
      location = this.file(u.id, name);
    await mkdir(path.dirname(location), { recursive: true });
    await writeFile(location, bytes, { flag: "wx" });
    const avatarUrl = `${this.config.getOrThrow<string>("PUBLIC_BASE_URL")}/assets/avatars/${u.id}/${name}`;
    try {
      const updated = await this.db.user.updateMany({
        where: { id: u.id, isActive: true, avatarUrl: u.avatarUrl },
        data: { avatarUrl },
      });
      if (!updated.count)
        throw new ConflictException({
          code: "AVATAR_CHANGED",
          message: "Ảnh đã thay đổi. Hãy tải lại profile.",
        });
    } catch (e) {
      await unlink(location);
      throw e;
    }
    await this.deleteManaged(u.id, u.avatarUrl);
    return profile(
      await this.db.user.findUniqueOrThrow({ where: { id: u.id } }),
    );
  }
  async remove(u: User) {
    const r = await this.db.user.updateMany({
      where: { id: u.id, isActive: true, avatarUrl: u.avatarUrl },
      data: { avatarUrl: null },
    });
    if (!r.count)
      throw new ConflictException({
        code: "AVATAR_CHANGED",
        message: "Ảnh đã thay đổi. Hãy tải lại profile.",
      });
    await this.deleteManaged(u.id, u.avatarUrl);
    return profile(
      await this.db.user.findUniqueOrThrow({ where: { id: u.id } }),
    );
  }
}
