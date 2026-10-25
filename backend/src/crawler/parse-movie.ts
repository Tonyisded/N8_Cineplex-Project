import {
  clean,
  sourceMovieUrl,
  trailerYoutubeId,
  type CrawledMovie,
} from "./normalize.js";

export type MovieDom = {
  title: string;
  rows: string[];
  synopsis: string;
  poster: string;
  age: string;
  trailer?: string;
};

export type ComingSoonListing = {
  title?: string;
  age?: string;
  duration?: string;
  release?: string;
  poster?: string;
};

function ageRatings(value: string): string[] {
  const matches = value
    .toUpperCase()
    .matchAll(/(?:^|[^A-Z0-9])(T18|T16|T13|P|K)(?=$|[^A-Z0-9])/g);
  return [...new Set([...matches].map((match) => match[1]))];
}

function field(rows: string[], label: string): string | null {
  const row = rows.map(clean).find((value) => value.startsWith(`${label}:`));
  return row ? clean(row.slice(label.length + 1)) || null : null;
}

function durationMinutes(value: string | null | undefined): number | null {
  const match = value
    ? /^(\d+)\s*(phút|minutes|min)$/iu.exec(clean(value))
    : null;
  const minutes = match ? +match[1] : NaN;
  return Number.isInteger(minutes) && minutes >= 1 && minutes <= 600
    ? minutes
    : null;
}

function releaseDate(
  value: string | null | undefined,
  separator: "/" | "-",
): Date | null {
  if (!value) return null;
  const match = new RegExp(
    `^(\\d{2})${separator}(\\d{2})${separator}(\\d{4})$`,
  ).exec(clean(value));
  if (!match) return null;
  const iso = `${match[3]}-${match[2]}-${match[1]}`;
  const date = new Date(`${iso}T00:00:00Z`);
  return Number.isFinite(date.getTime()) &&
    date.toISOString().slice(0, 10) === iso
    ? date
    : null;
}

function posterUrl(value: string, source: string): string | null {
  if (!value) return null;
  const url = new URL(value, source);
  if (
    url.protocol === "https:" &&
    !url.username &&
    !url.password &&
    ["www.cgv.vn", "static-cgv.vncdn.vn"].includes(url.hostname) &&
    (!url.port || url.port === "443")
  )
    return url.href;
  return null;
}

function baseMovie(source: string, raw: MovieDom) {
  return {
    sourceUrl: sourceMovieUrl(source),
    title: clean(raw.title),
    posterUrl: posterUrl(raw.poster, source),
    synopsis: clean(raw.synopsis) || null,
    originalTitle:
      field(raw.rows, "Tên gốc") ?? field(raw.rows, "Original title"),
    director: field(raw.rows, "Đạo diễn"),
    cast: field(raw.rows, "Diễn viên"),
    genre: field(raw.rows, "Thể loại"),
    language: field(raw.rows, "Ngôn ngữ"),
    trailerYoutubeId: trailerYoutubeId(raw.trailer ?? ""),
  };
}

export function parseMovie(
  source: string,
  raw: MovieDom,
  listingAge = "",
): CrawledMovie {
  const movie = baseMovie(source, raw);
  const detailAges = ageRatings(raw.age);
  const ages = detailAges.length ? detailAges : ageRatings(listingAge);
  const duration = durationMinutes(field(raw.rows, "Thời lượng"));
  if (!movie.title || !duration || ages.length !== 1)
    throw new Error("Missing/invalid title, duration or age rating");
  return {
    ...movie,
    durationMinutes: duration,
    ageRating: ages[0],
    releaseDate: releaseDate(field(raw.rows, "Khởi chiếu"), "/"),
  };
}

// Coming-soon entries are not bookable yet: duration and age may be unpublished.
// `0` means unknown duration and "NO" means unclassified; both are source facts,
// not invented values. A missing release date is returned as null so the caller
// can log it as omitted instead of failing the whole listing.
export function parseComingSoonMovie(
  source: string,
  raw: MovieDom,
  listing: ComingSoonListing = {},
): CrawledMovie | null {
  const movie = baseMovie(source, raw);
  const title = movie.title || clean(listing.title ?? "");
  if (!title) throw new Error("Missing/invalid title");
  const detailAges = ageRatings(raw.age);
  const ages = detailAges.length ? detailAges : ageRatings(listing.age ?? "");
  if (ages.length > 1) throw new Error("Conflicting age ratings");
  const release =
    releaseDate(field(raw.rows, "Khởi chiếu"), "/") ??
    releaseDate(listing.release, "-");
  if (!release) return null;
  return {
    ...movie,
    title,
    posterUrl: movie.posterUrl ?? posterUrl(listing.poster ?? "", source),
    durationMinutes:
      durationMinutes(field(raw.rows, "Thời lượng")) ??
      durationMinutes(listing.duration) ??
      0,
    ageRating: ages[0] ?? "NO",
    releaseDate: release,
  };
}
