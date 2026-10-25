-- Coming-soon movies may be published by CGV before a duration is announced.
-- Client contracts use 0 as the documented "duration not published" sentinel.
ALTER TABLE "movies" DROP CONSTRAINT "movies_duration_positive";
ALTER TABLE "movies" ADD CONSTRAINT "movies_duration_nonnegative" CHECK ("duration_minutes" >= 0);
