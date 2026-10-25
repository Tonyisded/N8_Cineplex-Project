package com.nhom8.cineplex.data

import com.nhom8.cineplex.R
import com.nhom8.cineplex.model.Movie

data class CastCredit(val name: String, val role: String, val portrait: Int)

object DetailSchedule {
    data class SampleFilmMeta(val releaseDate: String?, val rating: String?, val reviewCount: String?, val trailerYoutubeId: String?)
    fun filmMeta(movie: Movie) = SampleFilmMeta(movie.releaseDate, null, null, movie.trailerYoutubeId)
    val duneCast = listOf(
        CastCredit("Timothée Chalamet", "Paul Atreides", R.drawable.detail_timothee),
        CastCredit("Zendaya", "Chani", R.drawable.detail_zendaya),
        CastCredit("Rebecca Ferguson", "Jessica", R.drawable.detail_rebecca)
    )
    fun isDune(movie: Movie) = movie.id == "dune" || movie.original.equals("Dune: Part Two", ignoreCase = true) ||
        (movie.name.contains("Dune", ignoreCase = true) && (movie.name.contains("Phần hai", ignoreCase = true) || movie.name.contains("Part Two", ignoreCase = true)))
    fun ageLimit(movie: Movie): Int? = Regex("^T(\\d+)$").matchEntire(movie.age)?.groupValues?.get(1)?.toIntOrNull()
    fun upcomingReleaseNotice(movie: Movie, today: String): String? {
        val release = movie.releaseDate?.takeIf { it > today } ?: return null
        val shown = runCatching { displayCalendarDate(release) }.getOrNull() ?: return null
        return "Phim khởi chiếu $shown — lịch chiếu sẽ được cập nhật."
    }
}
