import { Controller, Get } from "@nestjs/common";
import { ApiOkResponse, ApiOperation, ApiTags } from "@nestjs/swagger";
import { PrismaService } from "../prisma.service.js";
@ApiTags("Combos")
@Controller("combos")
export class CombosController {
  constructor(private readonly db: PrismaService) {}
  @Get()
  @ApiOperation({
    summary: "Complete combos; an order selects at most one type",
  })
  @ApiOkResponse({
    schema: {
      type: "object",
      required: ["serverTime", "items"],
      properties: {
        serverTime: { type: "string", format: "date-time" },
        items: {
          type: "array",
          items: {
            type: "object",
            required: [
              "id",
              "name",
              "description",
              "price",
              "imageUrl",
              "status",
            ],
            properties: {
              id: { type: "string", format: "uuid" },
              name: { type: "string" },
              description: { type: "string" },
              price: { type: "string", pattern: "^[0-9]+$", example: "100000" },
              imageUrl: { type: "string", nullable: true },
              status: { type: "string", enum: ["AVAILABLE", "UNAVAILABLE"] },
            },
          },
        },
      },
    },
  })
  async list() {
    const rows = await this.db.combo.findMany({
      orderBy: [{ name: "asc" }, { id: "asc" }],
    });
    return {
      serverTime: new Date().toISOString(),
      items: rows.map((c) => ({
        id: c.id,
        name: c.name,
        description: c.description,
        price: c.price.toString(),
        imageUrl: c.imageUrl,
        status: c.status,
      })),
    };
  }
}
