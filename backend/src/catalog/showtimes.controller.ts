import {
  Controller,
  Get,
  NotFoundException,
  Param,
  ParseUUIDPipe,
} from "@nestjs/common";
import {
  ApiBadRequestResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiTags,
} from "@nestjs/swagger";
import { PrismaService } from "../prisma.service.js";
import { slotJson, slotSelect } from "./showtime-json.js";

@ApiTags("Showtimes")
@ApiParam({ name: "id", format: "uuid" })
@ApiBadRequestResponse({ description: "Invalid showtime UUID" })
@ApiNotFoundResponse({ description: "SHOWTIME_NOT_FOUND" })
@Controller("showtimes")
export class ShowtimesController {
  constructor(private readonly prisma: PrismaService) {}

  private async find(id: string) {
    const item = await this.prisma.showtime.findUnique({
      where: { id },
      select: slotSelect,
    });
    if (!item)
      throw new NotFoundException({
        code: "SHOWTIME_NOT_FOUND",
        message: "Showtime not found",
      });
    return item;
  }

  @Get(":id")
  @ApiOperation({
    summary: "Read a showtime, including canceled or begun slots",
  })
  @ApiOkResponse({
    description:
      "{serverTime,item}; item uses the movie-showtimes structure, with UTC dates, VND string basePrice and AVAILABLE/total N8 inventory counts. Read-only; never reserves seats.",
  })
  async detail(@Param("id", new ParseUUIDPipe()) id: string) {
    const serverTime = new Date().toISOString();
    const item = await this.find(id);
    return { serverTime, item: (await slotJson(this.prisma, [item]))[0] };
  }

  @Get(":id/seats")
  @ApiOperation({
    summary: "Read N8 seat inventory without holder information",
  })
  @ApiOkResponse({
    description:
      "{serverTime,showtimeId,items}; items contain id, seatCode, rowName, seatNumber, seatType, price (VND string), status. Ordered by row and seat number; missing inventory is an empty list, not generated capacity.",
  })
  async seats(@Param("id", new ParseUUIDPipe()) id: string) {
    const serverTime = new Date().toISOString();
    await this.find(id);
    const items = await this.prisma.showtimeSeat.findMany({
      where: { showtimeId: id },
      select: {
        id: true,
        seatCode: true,
        rowName: true,
        seatNumber: true,
        seatType: true,
        price: true,
        status: true,
      },
      orderBy: [{ rowName: "asc" }, { seatNumber: "asc" }, { id: "asc" }],
    });
    return {
      serverTime,
      showtimeId: id,
      items: items.map((seat) => ({ ...seat, price: seat.price.toString() })),
    };
  }
}
