import {
  BadRequestException,
  Controller,
  Get,
  NotFoundException,
  Param,
  ParseUUIDPipe,
  Query,
} from "@nestjs/common";
import {
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiQuery,
  ApiTags,
} from "@nestjs/swagger";
import { PrismaService } from "../prisma.service.js";
import { businessDate, dateWindow } from "../crawler/normalize.js";
import type { Movie, Prisma } from "../generated/prisma/client.js";

const slotSelect = {
  id: true,
  movieId: true,
  sourceKey: true,
  startAt: true,
  endAt: true,
  format: true,
  presentationLabel: true,
  basePrice: true,
  status: true,
  auditorium: {
    select: { id: true, name: true, type: true, capacity: true, isExtra: true },
  },
} as const;
type CatalogSlot = Prisma.ShowtimeGetPayload<{ select: typeof slotSelect }>;
export function upcomingWhere(
  window: ReturnType<typeof catalogDate>,
  now: Date,
) {
  return {
    status: "ACTIVE" as const,
    startAt: { gte: window.start, lt: window.end, gt: now },
  };
}

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

  private async slotJson(items: CatalogSlot[]) {
    if (!items.length) return [];
    const grouped = await this.prisma.showtimeSeat.groupBy({
      by: ["showtimeId", "status"],
      where: { showtimeId: { in: items.map((s) => s.id) } },
      _count: { _all: true },
    });
    const counts = new Map<
      string,
      { availableSeats: number; totalSeats: number }
    >();
    for (const row of grouped) {
      const count = counts.get(row.showtimeId) ?? {
        availableSeats: 0,
        totalSeats: 0,
      };
      count.totalSeats += row._count._all;
      if (row.status === "AVAILABLE") count.availableSeats += row._count._all;
      counts.set(row.showtimeId, count);
    }
    return items.map((s) => ({
      ...s,
      basePrice: s.basePrice.toString(),
      ...(counts.get(s.id) ?? { availableSeats: 0, totalSeats: 0 }),
    }));
  }

  @Get()
  @ApiOkResponse({
    description:
      "businessDate/items envelope with serverTime (UTC ISO); each movie includes trailerYoutubeId and upcoming showtimes with presentationLabel, availableSeats and totalSeats from N8 inventory.",
  })
  @ApiOperation({
    summary:
      "Movies with upcoming showtimes and N8 seat inventory on a Vietnam calendar date",
  })
  @ApiQuery({ name: "date", required: false, example: "2026-10-10" })
  async list(@Query("date") date?: string) {
    const now = new Date();
    const window = catalogDate(date ?? businessDate(now));
    const where = upcomingWhere(window, now);
    const items = await this.prisma.movie.findMany({
      where: {
        showtimes: {
          some: where,
        },
      },
      orderBy: [{ title: "asc" }, { id: "asc" }],
      include: {
        showtimes: {
          where,
          orderBy: [{ startAt: "asc" }, { id: "asc" }],
          select: slotSelect,
        },
      },
    });
    const slots = await this.slotJson(items.flatMap((m) => m.showtimes));
    const byMovie = new Map<string, typeof slots>();
    for (const slot of slots) {
      const group = byMovie.get(slot.movieId) ?? [];
      group.push(slot);
      byMovie.set(slot.movieId, group);
    }
    return {
      businessDate: window.businessDate,
      serverTime: now.toISOString(),
      items: items.map((m) => ({
        ...movieJson(m),
        showtimes: byMovie.get(m.id) ?? [],
      })),
    };
  }

  @Get("dates")
  @ApiOperation({
    summary: "Vietnam dates with active showtimes, starting today",
  })
  async dates() {
    const now = new Date();
    const window = catalogDate(businessDate(now));
    const slots = await this.prisma.showtime.findMany({
      where: { status: "ACTIVE", startAt: { gt: now } },
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
  @ApiOkResponse({
    description:
      "Movie metadata; trailerYoutubeId is a nullable 11-character YouTube video ID.",
  })
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
  @ApiOkResponse({
    description:
      "businessDate/items and serverTime; each upcoming slot includes presentationLabel, availableSeats (AVAILABLE only), totalSeats (all inventory), N8 auditorium and demo basePrice.",
  })
  @ApiOperation({
    summary:
      "Upcoming showtimes, logical N8 rooms, seat counts and demo prices",
  })
  @ApiParam({ name: "id", format: "uuid" })
  @ApiQuery({ name: "date", required: false, example: "2026-10-10" })
  async showtimes(
    @Param("id", new ParseUUIDPipe()) id: string,
    @Query("date") date?: string,
  ) {
    const now = new Date();
    const window = catalogDate(date ?? businessDate(now));
    await this.detail(id);
    const items = await this.prisma.showtime.findMany({
      where: {
        movieId: id,
        ...upcomingWhere(window, now),
      },
      orderBy: [{ startAt: "asc" }, { id: "asc" }],
      select: slotSelect,
    });
    return {
      businessDate: window.businessDate,
      serverTime: now.toISOString(),
      items: await this.slotJson(items),
    };
  }
}
