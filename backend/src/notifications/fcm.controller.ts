import { Body, Controller, Delete, Put, Req, UseGuards } from "@nestjs/common";
import {
  ApiBearerAuth,
  ApiOperation,
  ApiProperty,
  ApiTags,
} from "@nestjs/swagger";
import { IsString, MaxLength, MinLength } from "class-validator";
import {
  JwtGuard,
  RoleGuard,
  Roles,
  type AuthRequest,
} from "../auth/guards.js";
import { PrismaService } from "../prisma.service.js";
import { Prisma } from "../generated/prisma/client.js";
class FcmTokenDto {
  @ApiProperty({
    description: "Current installation FCM token; never used as authentication",
  })
  @IsString()
  @MinLength(1)
  @MaxLength(4096)
  token!: string;
}
@ApiTags("Notifications")
@ApiBearerAuth()
@UseGuards(JwtGuard, RoleGuard)
@Roles("CUSTOMER")
@Controller("me/fcm-token")
export class FcmController {
  constructor(private readonly prisma: PrismaService) {}
  @Put()
  @ApiOperation({
    summary:
      "Register this installation token, removing its previous account binding",
  })
  async put(@Req() req: AuthRequest, @Body() body: FcmTokenDto) {
    await this.prisma.$transaction(async (tx) => {
      await tx.$executeRaw`SELECT pg_advisory_xact_lock(hashtextextended(${body.token}, 0))`;
      const owners = await tx.user.findMany({
        where: { OR: [{ id: req.user.id }, { fcmToken: body.token }] },
        select: { id: true },
      });
      await tx.$queryRaw(
        Prisma.sql`SELECT id FROM users WHERE id IN (${Prisma.join(owners.map((o) => Prisma.sql`${o.id}::uuid`))}) ORDER BY id FOR UPDATE`,
      );
      await tx.user.updateMany({
        where: { fcmToken: body.token, id: { not: req.user.id } },
        data: { fcmToken: null },
      });
      await tx.user.update({
        where: { id: req.user.id },
        data: { fcmToken: body.token },
      });
    });
    return { registered: true };
  }
  @Delete()
  @ApiOperation({ summary: "Remove this account current notification token" })
  async remove(@Req() req: AuthRequest) {
    await this.prisma.user.update({
      where: { id: req.user.id },
      data: { fcmToken: null },
    });
    return { registered: false };
  }
}
