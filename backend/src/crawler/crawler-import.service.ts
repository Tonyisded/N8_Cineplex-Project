import { Injectable, Logger } from "@nestjs/common";
import { randomUUID } from "node:crypto";
import { PrismaService } from "../prisma.service.js";
import { CrawlerService } from "./crawler.service.js";
import {
  businessDate,
  dateWindow,
  CAPACITIES,
  PRICES,
  roomType,
  seatTemplate,
  type CrawlSnapshot,
} from "./normalize.js";

@Injectable()
export class CrawlerImportService {
  private readonly logger = new Logger(CrawlerImportService.name);
  constructor(
    private readonly crawler: CrawlerService,
    private readonly prisma: PrismaService,
  ) {}

  async run(date = businessDate()) {
    dateWindow(date);
    if (date < businessDate())
      throw new Error("Cannot crawl a past business date");
    return this.execute(date, () => this.crawler.collect(date));
  }

  async runUpcoming(today = businessDate()) {
    dateWindow(today);
    if (today < businessDate())
      throw new Error("Cannot crawl a past business date");
    const batchRunId = randomUUID();
    const startedAt = new Date().toISOString();
    const runs: Awaited<ReturnType<CrawlerImportService["execute"]>>[] = [];
    let attemptedDate = today;
    try {
      const range = await this.crawler.collectUpcoming(
        today,
        async (date, collect) => {
          attemptedDate = date;
          const result = await this.execute(date, collect, batchRunId);
          runs.push(result);
          return result.snapshot;
        },
      );
      const event = {
        job: "crawl-cgv-batch",
        runId: batchRunId,
        businessDate: today,
        startedAt,
        finishedAt: new Date().toISOString(),
        status: runs.some((run) => run.event.status === "PARTIAL")
          ? "PARTIAL"
          : "SUCCESS",
        days: runs.map((run) => ({
          businessDate: run.event.businessDate,
          runId: run.event.runId,
          status: run.event.status,
          sourceMovies: run.event.sourceMovies,
          sourceShowtimes: run.event.sourceShowtimes,
        })),
        ...range,
      };
      this.logger.log(JSON.stringify(event));
      return { event, runs };
    } catch (error) {
      this.logger.error(
        JSON.stringify({
          job: "crawl-cgv-batch",
          runId: batchRunId,
          businessDate: today,
          attemptedDate,
          startedAt,
          finishedAt: new Date().toISOString(),
          status: "FAILED",
          completedDates: runs.map((run) => run.event.businessDate),
          reason: error instanceof Error ? error.message : "Crawler failed",
        }),
      );
      throw error;
    }
  }

  private async execute(
    date: string,
    collect: () => Promise<CrawlSnapshot>,
    batchRunId?: string,
  ) {
    const runId = randomUUID();
    const startedAt = new Date().toISOString();
    try {
      const snapshot = await collect();
      const result = await this.import(snapshot);
      const event = {
        job: "crawl-cgv",
        runId,
        ...(batchRunId ? { batchRunId } : {}),
        businessDate: date,
        startedAt,
        finishedAt: new Date().toISOString(),
        status:
          snapshot.skipped.length || result.conflicts.length
            ? "PARTIAL"
            : "SUCCESS",
        capturedAt: snapshot.capturedAt,
        sourceMovies: snapshot.sourceMovies,
        sourceShowtimes: snapshot.sourceShowtimes,
        parsedMovies: snapshot.movies.length,
        parsedShowtimes: snapshot.showtimes.length,
        skipped: snapshot.skipped,
        ...result,
      };
      this.logger.log(JSON.stringify(event));
      return { event, snapshot };
    } catch (error) {
      this.logger.error(
        JSON.stringify({
          job: "crawl-cgv",
          runId,
          ...(batchRunId ? { batchRunId } : {}),
          businessDate: date,
          startedAt,
          finishedAt: new Date().toISOString(),
          status: "FAILED",
          reason: error instanceof Error ? error.message : "Crawler failed",
        }),
      );
      throw error;
    }
  }

  async import(snapshot: CrawlSnapshot) {
    return this.prisma.$transaction(
      async (tx) => {
        // Serialize room allocation across CLI/local/VPS; all network work is already complete.
        await tx.$queryRaw`SELECT 1 AS locked FROM pg_advisory_xact_lock(810710)`;
        const movies = new Map<
          string,
          { id: string; durationMinutes: number }
        >();
        for (const data of snapshot.movies) {
          const movie = await tx.movie.upsert({
            where: { sourceUrl: data.sourceUrl },
            create: data,
            update: data,
          });
          movies.set(data.sourceUrl, movie);
        }
        let created = 0,
          unchanged = 0,
          seatsCreated = 0;
        const conflicts: { source: string; reason: string }[] = [];
        for (const slot of [...snapshot.showtimes].sort(
          (a, b) =>
            a.startAt.getTime() - b.startAt.getTime() ||
            a.sourceKey.localeCompare(b.sourceKey),
        )) {
          const movie = movies.get(slot.movieUrl);
          if (!movie) {
            conflicts.push({
              source: slot.sourceKey,
              reason: "Movie unavailable",
            });
            continue;
          }
          const endAt = new Date(
            slot.startAt.getTime() + movie.durationMinutes * 60000,
          );
          const existing = await tx.showtime.findUnique({
            where: { sourceKey: slot.sourceKey },
          });
          if (existing) {
            if (
              existing.movieId !== movie.id ||
              existing.startAt.getTime() !== slot.startAt.getTime() ||
              existing.endAt.getTime() !== endAt.getTime() ||
              existing.format !== slot.format
            )
              conflicts.push({
                source: slot.sourceKey,
                reason: "Existing slot structure changed; inventory preserved",
              });
            else unchanged++;
            continue;
          }
          const type = roomType(slot.format);
          let auditorium = await tx.auditorium.findFirst({
            where: {
              type,
              status: "ACTIVE",
              showtimes: {
                none: {
                  status: "ACTIVE",
                  startAt: { lt: endAt },
                  endAt: { gt: slot.startAt },
                },
              },
            },
            orderBy: [{ isExtra: "asc" }, { name: "asc" }, { id: "asc" }],
          });
          if (!auditorium)
            auditorium = await tx.auditorium.create({
              data: {
                name: `${type}_EXTRA_${randomUUID().slice(0, 8)}`,
                type,
                capacity: CAPACITIES[type],
                isExtra: true,
              },
            });
          const showtime = await tx.showtime.create({
            data: {
              movieId: movie.id,
              auditoriumId: auditorium.id,
              startAt: slot.startAt,
              endAt,
              format: slot.format,
              basePrice: PRICES[slot.format],
              sourceKey: slot.sourceKey,
            },
          });
          const seats = seatTemplate(type, auditorium.capacity).map((seat) => ({
            ...seat,
            showtimeId: showtime.id,
            price: PRICES[slot.format],
          }));
          seatsCreated += (await tx.showtimeSeat.createMany({ data: seats }))
            .count;
          created++;
        }
        return {
          moviesUpserted: movies.size,
          created,
          unchanged,
          seatsCreated,
          conflicts,
        };
      },
      { maxWait: 10000, timeout: 60000 },
    );
  }
}
