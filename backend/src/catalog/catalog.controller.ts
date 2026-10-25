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
import type { Movie } from "../generated/prisma/client.js";
import { slotSelect, slotJson } from "./showtime-json.js";

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
    const slots = await slotJson(
      this.prisma,
      items.flatMap((m) => m.showtimes),
    );
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

  @Get("coming-soon")
  @ApiOkResponse({
    description:
      "businessDate/items and serverTime; CGV coming-soon movies with releaseDate after today (Vietnam) that are not already screening before release, no showtimes. durationMinutes 0 means unpublished; ageRating NO means unclassified.",
  })
  @ApiOperation({
    summary: "Coming-soon movies whose Vietnamese release date is after today",
  })
  async comingSoon() {
    const now = new Date();
    const today = businessDate(now);
    const movies = await this.prisma.movie.findMany({
      where: { releaseDate: { gt: new Date(`${today}T00:00:00.000Z`) } },
      orderBy: [{ releaseDate: "asc" }, { title: "asc" }, { id: "asc" }],
      include: {
        showtimes: {
          where: { status: "ACTIVE" },
          orderBy: [{ startAt: "asc" }],
          take: 1,
          select: { startAt: true },
        },
      },
    });
    return {
      businessDate: today,
      serverTime: now.toISOString(),
      // A presale movie stays here; a movie already screening before its
      // release date is now-showing, not coming soon.
      items: movies
        .filter(
          (movie) =>
            !movie.showtimes.length ||
            movie.showtimes[0].startAt >= movie.releaseDate!,
        )
        .map(({ showtimes: _showtimes, ...movie }) => movieJson(movie)),
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
      items: await slotJson(this.prisma, items),
    };
  }
}
