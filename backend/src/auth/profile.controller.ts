import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  Patch,
  Post,
  Req,
  Res,
  UseGuards,
  UseInterceptors,
} from "@nestjs/common";
import { ApiBearerAuth, ApiBody, ApiConsumes, ApiTags } from "@nestjs/swagger";
import { FileInterceptor } from "@nestjs/platform-express";
import { UploadedFile } from "@nestjs/common";
import type { Response } from "express";
import { AuthService, profile } from "./auth.service.js";
import { AvatarService } from "./avatar.service.js";
import { PrismaService } from "../prisma.service.js";
import { ChangeEmailDto, ProfileDto } from "./dto.js";
import { JwtGuard, type AuthRequest } from "./guards.js";

@ApiTags("Profile")
@ApiBearerAuth()
@UseGuards(JwtGuard)
@Controller("me")
export class ProfileController {
  constructor(
    private readonly db: PrismaService,
    private readonly auth: AuthService,
    private readonly avatars: AvatarService,
  ) {}
  @Get() me(@Req() req: AuthRequest) {
    return profile(req.user);
  }
  @Patch() async edit(@Req() req: AuthRequest, @Body() body: ProfileDto) {
    return profile(
      await this.db.user.update({
        where: { id: req.user.id },
        data: { fullName: body.fullName },
      }),
    );
  }
  @Post("change-email") @HttpCode(200) email(
    @Req() req: AuthRequest,
    @Body() body: ChangeEmailDto,
  ) {
    return this.auth.changeEmail(req.user, body);
  }
  @Post("avatar")
  @ApiConsumes("multipart/form-data")
  @ApiBody({
    schema: {
      type: "object",
      properties: { file: { type: "string", format: "binary" } },
      required: ["file"],
    },
  })
  @UseInterceptors(
    FileInterceptor("file", {
      limits: { fileSize: 5 * 1024 * 1024, files: 1, fields: 0 },
    }),
  )
  avatar(@Req() req: AuthRequest, @UploadedFile() file: Express.Multer.File) {
    return this.avatars.upload(req.user, file);
  }
  @Delete("avatar") remove(@Req() req: AuthRequest) {
    return this.avatars.remove(req.user);
  }
}
@Controller("assets/avatars")
export class AvatarController {
  constructor(private readonly avatars: AvatarService) {}
  @Get(":userId/:name") async image(
    @Param("userId") userId: string,
    @Param("name") name: string,
    @Res() res: Response,
  ) {
    const bytes = await this.avatars.read(userId, name);
    res
      .set({
        "X-Content-Type-Options": "nosniff",
        "Cache-Control": "public, max-age=86400",
      })
      .type("image/webp")
      .send(bytes);
  }
}
