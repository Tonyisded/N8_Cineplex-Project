import type {
  ShowtimeFormat,
  AuditoriumType,
} from "../generated/prisma/enums.js";

export const clean = (value: string) =>
  value.normalize("NFC").replace(/\s+/gu, " ").trim();
export function businessDate(now = new Date()): string {
  const parts = new Intl.DateTimeFormat("en", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(now);
  const part = (name: string) => parts.find((p) => p.type === name)!.value;
  return `${part("year")}-${part("month")}-${part("day")}`;
}
export function dateWindow(value = businessDate()) {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value))
    throw new Error("Invalid business date (YYYY-MM-DD)");
  const calendar = new Date(`${value}T00:00:00.000Z`);
  if (
    !Number.isFinite(calendar.getTime()) ||
    calendar.toISOString().slice(0, 10) !== value
  )
    throw new Error("Invalid calendar date");
  const start = new Date(`${value}T00:00:00+07:00`);
  return {
    businessDate: value,
    start,
    end: new Date(start.getTime() + 86400000),
  };
}
export function nextBusinessDate(value: string) {
  return businessDate(dateWindow(value).end);
}
export function sourceMovieUrl(value: string) {
  const u = new URL(value, "https://www.cgv.vn");
  if (
    u.origin !== "https://www.cgv.vn" ||
    u.username ||
    u.password ||
    !/^\/default\/[a-z0-9-]+\.html$/.test(u.pathname) ||
    u.search ||
    u.hash
  )
    throw new Error("Invalid CGV movie URL");
  return u.href;
}
export function sourceSlot(href: string, time: string, date: string) {
  dateWindow(date);
  const u = new URL(href, "https://www.cgv.vn");
  const m =
    /^\/default\/cinemas\/booking\/tickets\/site\/071\/seq\/(\d+)\/dy\/(\d{8})\/?$/.exec(
      u.pathname,
    );
  if (
    u.origin !== "https://www.cgv.vn" ||
    u.username ||
    u.password ||
    u.search ||
    u.hash ||
    !m ||
    m[2] !== date.replaceAll("-", "")
  )
    throw new Error("Invalid CGV showtime identity/date");
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(time))
    throw new Error("Invalid showtime time");
  return {
    sourceKey: `cgv:071:${m[2]}:${m[1]}`,
    startAt: new Date(`${date}T${time}:00+07:00`),
  };
}
export function normalizeFormat(
  label: string,
  screenClass?: string,
): ShowtimeFormat {
  const text = clean(label).toUpperCase();
  const key = text
    .replace(
      /\s+(PHỤ ĐỀ|LỒNG TIẾNG|(?:ENG|VIET)(?:&(?:ENG|VIET))? SUB).*$/u,
      "",
    )
    .replace(/[^A-Z0-9]/g, "");
  const formats: Record<string, ShowtimeFormat> = {
    "2D": "STANDARD_2D",
    "3D": "STANDARD_3D",
    IMAX2D: "IMAX_2D",
    IMAX3D: "IMAX_3D",
    SCREENX: "SCREENX",
    SCREENX2D: "SCREENX",
    GOLDCLASS: "GOLD_CLASS",
  };
  if (screenClass) {
    const type = screenClass
      .split(/\s+/)
      .filter((c) => c !== "film-screen")
      .join(" ");
    if (type === "goldclass" && key === "2D") return "GOLD_CLASS";
    if (type === "screenx" && ["2D", "SCREENX2D"].includes(key))
      return "SCREENX";
    if (type === "imax" && ["IMAX2D", "IMAX3D"].includes(key))
      return formats[key];
    if (type === "std" && ["2D", "3D"].includes(key)) return formats[key];
    throw new Error(`Unsupported format/capability: ${clean(label)} (${type})`);
  }
  if (!formats[key]) throw new Error(`Unsupported format: ${clean(label)}`);
  return formats[key];
}
export const PRICES: Record<ShowtimeFormat, number> = {
  STANDARD_2D: 100000,
  STANDARD_3D: 120000,
  IMAX_2D: 150000,
  IMAX_3D: 180000,
  SCREENX: 160000,
  GOLD_CLASS: 250000,
};
export const roomType = (format: ShowtimeFormat): AuditoriumType =>
  format.startsWith("IMAX")
    ? "IMAX"
    : format === "SCREENX"
      ? "SCREENX"
      : format === "GOLD_CLASS"
        ? "GOLD"
        : "STANDARD";
export const CAPACITIES: Record<AuditoriumType, number> = {
  STANDARD: 160,
  IMAX: 496,
  SCREENX: 180,
  GOLD: 32,
};
export function seatTemplate(type: AuditoriumType, capacity: number) {
  const columns = { STANDARD: 16, IMAX: 31, SCREENX: 12, GOLD: 8 }[type];
  if (!Number.isInteger(capacity) || capacity < 1 || capacity > columns * 26)
    throw new Error("Invalid auditorium capacity");
  // shortcut: no VIP/couple zoning in crawler import; add it with the booking seat-map task.
  return Array.from({ length: capacity }, (_, i) => {
    const rowName = String.fromCharCode(65 + Math.floor(i / columns));
    const seatNumber = (i % columns) + 1;
    return {
      seatCode: `${rowName}${seatNumber}`,
      rowName,
      seatNumber,
      seatType: type === "GOLD" ? ("GOLD" as const) : ("NORMAL" as const),
    };
  });
}
export type CrawledMovie = {
  sourceUrl: string;
  title: string;
  posterUrl: string | null;
  durationMinutes: number;
  ageRating: string;
  synopsis: string | null;
  genre: string | null;
  language: string | null;
  originalTitle: string | null;
  releaseDate: Date | null;
  director: string | null;
  cast: string | null;
};
export type CrawledSlot = {
  movieUrl: string;
  sourceKey: string;
  startAt: Date;
  format: ShowtimeFormat;
};
export type CrawlSnapshot = {
  businessDate: string;
  capturedAt: string;
  sourceMovies: number;
  sourceShowtimes: number;
  empty: boolean;
  movies: CrawledMovie[];
  showtimes: CrawledSlot[];
  skipped: { source: string; reason: string }[];
};
