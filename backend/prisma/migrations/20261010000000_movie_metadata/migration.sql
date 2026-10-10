-- Stop on duplicate source URLs; do not merge or remove existing domain data.
ALTER TABLE "movies"
  ADD COLUMN "original_title" TEXT,
  ADD COLUMN "release_date" DATE,
  ADD COLUMN "director" TEXT,
  ADD COLUMN "cast" TEXT;
CREATE UNIQUE INDEX "movies_source_url_key" ON "movies"("source_url");
