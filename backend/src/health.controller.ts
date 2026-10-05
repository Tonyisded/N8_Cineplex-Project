import { Controller, Get, ServiceUnavailableException } from "@nestjs/common";
import {
  ApiOkResponse,
  ApiOperation,
  ApiServiceUnavailableResponse,
  ApiTags,
} from "@nestjs/swagger";
import { PrismaService } from "./prisma.service.js";

@ApiTags("health")
@Controller("health")
export class HealthController {
  constructor(private readonly prisma: PrismaService) {}

  @Get()
  @ApiOperation({ summary: "Check API and PostgreSQL connectivity" })
  @ApiOkResponse({
    schema: {
      type: "object",
      required: ["status", "database"],
      properties: {
        status: { type: "string", enum: ["ok"] },
        database: { type: "string", enum: ["up"] },
      },
    },
  })
  @ApiServiceUnavailableResponse({
    schema: {
      type: "object",
      required: ["status", "database"],
      properties: {
        status: { type: "string", enum: ["error"] },
        database: { type: "string", enum: ["down"] },
      },
    },
  })
  async health() {
    try {
      await this.prisma.$queryRaw`SELECT 1`;
      return { status: "ok", database: "up" };
    } catch {
      throw new ServiceUnavailableException({
        status: "error",
        database: "down",
      });
    }
  }
}
