import {
  BadRequestException,
  Controller,
  Get,
  NotFoundException,
  Param,
  ParseUUIDPipe,
  Query,
} from "@nestjs/common";
import { ApiOperation, ApiParam, ApiQuery, ApiTags } from "@nestjs/swagger";
import { PrismaService } from "../prisma.service.js";
import { businessDate, dateWindow } from "../crawler/normalize.js";
import type { Movie } from "../generated/prisma/client.js";

export function catalogDate(value?: string) {
  try {
    return dateWindow(value);
  } catch {
    throw new BadRequestException({
      code: "INVALID_DATE",
      message: "date must be a valid YYYY-MM-DD",
    });
  }
}
export function movieJson(movie: Movie) {
  return {
    ...movie,
    releaseDate: movie.releaseDate?.toISOString().slice(0, 10) ?? null,
  };
}

@ApiTags("Movies")
@Controller("movies")
export class CatalogController {
  constructor(private readonly prisma: PrismaService) {}

  @Get()
  @ApiOperation({
    summary:
      "Movies with active Landmark 81 showtimes on a Vietnam calendar date",
  })
  @ApiQuery({ name: "date", required: false, example: "2026-10-10" })
  async list(@Query("date") date?: string) {
    const window = catalogDate(date);
    const items = await this.prisma.movie.findMany({
      where: {
        showtimes: {
          some: {
            status: "ACTIVE",
            startAt: { gte: window.start, lt: window.end },
          },
        },
      },
      orderBy: [{ title: "asc" }, { id: "asc" }],
    });
    return { businessDate: window.businessDate, items: items.map(movieJson) };
  }

  @Get("dates")
  @ApiOperation({
    summary: "Vietnam dates with active showtimes, starting today",
  })
  async dates() {
    const window = catalogDate();
    const slots = await this.prisma.showtime.findMany({
      where: { status: "ACTIVE", startAt: { gte: window.start } },
      select: { startAt: true },
      orderBy: { startAt: "asc" },
    });
    return {
      businessDate: window.businessDate,
      dates: [
        ...new Set(slots.map((slot) => businessDate(slot.startAt))),
      ].sort(),
    };
  }

  @Get(":id")
  @ApiOperation({ summary: "Movie metadata" })
  @ApiParam({ name: "id", format: "uuid" })
  async detail(@Param("id", new ParseUUIDPipe()) id: string) {
    const movie = await this.prisma.movie.findUnique({ where: { id } });
    if (!movie)
      throw new NotFoundException({
        code: "MOVIE_NOT_FOUND",
        message: "Movie not found",
      });
    return movieJson(movie);
  }

  @Get(":id/showtimes")
  @ApiOperation({
    summary: "Active showtimes, logical N8 rooms and demo prices",
  })
  @ApiParam({ name: "id", format: "uuid" })
  @ApiQuery({ name: "date", required: false, example: "2026-10-10" })
  async showtimes(
    @Param("id", new ParseUUIDPipe()) id: string,
    @Query("date") date?: string,
  ) {
    const window = catalogDate(date);
    await this.detail(id);
    const items = await this.prisma.showtime.findMany({
      where: {
        movieId: id,
        status: "ACTIVE",
        startAt: { gte: window.start, lt: window.end },
      },
      orderBy: [{ startAt: "asc" }, { id: "asc" }],
      select: {
        id: true,
        movieId: true,
        sourceKey: true,
        startAt: true,
        endAt: true,
        format: true,
        basePrice: true,
        status: true,
        auditorium: {
          select: {
            id: true,
            name: true,
            type: true,
            capacity: true,
            isExtra: true,
          },
        },
      },
    });
    return {
      businessDate: window.businessDate,
      items: items.map((s) => ({ ...s, basePrice: s.basePrice.toString() })),
    };
  }
}
