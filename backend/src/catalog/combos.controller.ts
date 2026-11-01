import { Controller, Get } from "@nestjs/common";
import { ApiOperation, ApiTags } from "@nestjs/swagger";
import { PrismaService } from "../prisma.service.js";
@ApiTags("Combos")
@Controller("combos")
export class CombosController {
  constructor(private readonly db: PrismaService) {}
  @Get()
  @ApiOperation({
    summary: "Complete combos; an order selects at most one type",
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
