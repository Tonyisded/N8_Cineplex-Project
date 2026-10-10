import { clean, sourceMovieUrl, type CrawledMovie } from "./normalize.js";

export type MovieDom = {
  title: string;
  rows: string[];
  synopsis: string;
  poster: string;
  age: string;
};
export function parseMovie(
  source: string,
  raw: MovieDom,
  listingAge = "",
): CrawledMovie {
  const field = (label: string) => {
    const row = raw.rows.map(clean).find((r) => r.startsWith(`${label}:`));
    return row ? clean(row.slice(label.length + 1)) || null : null;
  };
  const title = clean(raw.title);
  const duration = field("Thời lượng");
  const durationMinutes =
    duration && /^(\d+)\s*(phút|minutes|min)$/iu.exec(duration)?.[1];
  const ageSource = /(?:^|[^A-Z0-9])(T18|T16|T13|P|K)(?=$|[^A-Z0-9])/i.test(
    raw.age,
  )
    ? raw.age
    : listingAge;
  const ages = [
    ...ageSource
      .toUpperCase()
      .matchAll(/(?:^|[^A-Z0-9])(T18|T16|T13|P|K)(?=$|[^A-Z0-9])/g),
  ].map((m) => m[1]);
  const ageRating = [...new Set(ages)];
  if (
    !title ||
    !durationMinutes ||
    +durationMinutes < 1 ||
    +durationMinutes > 600 ||
    ageRating.length !== 1
  )
    throw new Error("Missing/invalid title, duration or age rating");
  const release = field("Khởi chiếu");
  let releaseDate: Date | null = null;
  if (release) {
    const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(release);
    if (m) {
      const iso = `${m[3]}-${m[2]}-${m[1]}`;
      const d = new Date(`${iso}T00:00:00Z`);
      if (Number.isFinite(d.getTime()) && d.toISOString().slice(0, 10) === iso)
        releaseDate = d;
    }
  }
  let posterUrl: string | null = null;
  if (raw.poster) {
    const p = new URL(raw.poster, source);
    if (
      p.protocol === "https:" &&
      !p.username &&
      !p.password &&
      ["www.cgv.vn", "static-cgv.vncdn.vn"].includes(p.hostname) &&
      (!p.port || p.port === "443")
    )
      posterUrl = p.href;
  }
  return {
    sourceUrl: sourceMovieUrl(source),
    title,
    posterUrl,
    durationMinutes: +durationMinutes,
    ageRating: ageRating[0],
    originalTitle: field("Tên gốc") ?? field("Original title"),
    releaseDate,
    director: field("Đạo diễn"),
    cast: field("Diễn viên"),
    genre: field("Thể loại"),
    language: field("Ngôn ngữ"),
    synopsis: clean(raw.synopsis) || null,
  };
}
