package com.nhom8.cineplex.data

import com.nhom8.cineplex.R
import com.nhom8.cineplex.model.Movie
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** UI-only cinema schedule until the API exposes area/cinema selection and booking. */
data class SampleCinema(val id: String, val name: String, val address: String, val area: String)
data class SampleSlot(val id: String, val movieId: String, val date: String, val cinema: SampleCinema, val format: String, val time: String, val soldOut: Boolean)
data class CastCredit(val name: String, val role: String, val portrait: Int)

object DetailSchedule {
    data class SampleFilmMeta(val releaseDate: String?, val rating: String?, val reviewCount: String?, val trailerYoutubeId: String?)
    fun filmMeta(movie: Movie) = if (isDune(movie)) SampleFilmMeta(movie.releaseDate ?: "2025-02-25", "8,6 / 10", "1.280 lượt đánh giá", movie.trailerYoutubeId ?: "ABfQGtUEEzA")
        else SampleFilmMeta(movie.releaseDate, null, null, movie.trailerYoutubeId)
    val areas = linkedMapOf("hcm" to "TP. Hồ Chí Minh", "hn" to "Hà Nội")
    val cinemas = listOf(
        SampleCinema("dongkhoi", "Cineplex Đồng Khởi", "72 Lê Thánh Tôn · Quận 1", "hcm"),
        SampleCinema("thuduc", "Cineplex Thủ Đức", "216 Võ Văn Ngân · TP. Thủ Đức", "hcm"),
        SampleCinema("badinh", "Cineplex Ba Đình", "54 Liễu Giai · Ba Đình", "hn")
    )
    val duneCast = listOf(
        CastCredit("Timothée Chalamet", "Paul Atreides", R.drawable.detail_timothee),
        CastCredit("Zendaya", "Chani", R.drawable.detail_zendaya),
        CastCredit("Rebecca Ferguson", "Jessica", R.drawable.detail_rebecca)
    )
    fun isDune(movie: Movie) = movie.id == "dune" || movie.original.equals("Dune: Part Two", ignoreCase = true) ||
        (movie.name.contains("Dune", ignoreCase = true) && (movie.name.contains("Phần hai", ignoreCase = true) || movie.name.contains("Part Two", ignoreCase = true)))
    fun ageLimit(movie: Movie): Int? = Regex("^T(\\d+)$").matchEntire(movie.age)?.groupValues?.get(1)?.toIntOrNull()
    fun dates(today: String): List<String> = buildList {
        var date = today
        repeat(7) { add(date); date = nextCalendarDate(date) }
    }
    fun slots(movieId: String, date: String, today: String, nowMillis: Long): List<SampleSlot> {
        val index = dates(today).indexOf(date)
        if (index < 0 || index == 3) return emptyList()
        val times = listOf("10:00", "13:30", "16:45", "19:30", "22:00")
        return cinemas.flatMap { cinema ->
            val formats = if (cinema.id == "badinh") listOf("2D · Phụ đề") else listOf("2D · Phụ đề", "2D · Lồng tiếng")
            formats.flatMap { format ->
                times.mapIndexedNotNull { order, time ->
                    val hour = time.take(2).toInt()
                    val minute = time.takeLast(2).toInt()
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")).apply {
                        this.time = Date(calendarDateMillis(date)); set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
                    }
                    if (calendar.timeInMillis <= nowMillis) null else SampleSlot("$movieId-$date-${cinema.id}-$format-$time", movieId, date, cinema, format, time, order == 0)
                }
            }
        }
    }
}
