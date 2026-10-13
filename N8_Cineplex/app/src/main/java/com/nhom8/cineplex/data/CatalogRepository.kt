package com.nhom8.cineplex.data

import com.nhom8.cineplex.BuildConfig
import com.nhom8.cineplex.model.Movie
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

fun vietnamDate(now: Date = Date()): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh") }.format(now)
fun calendarDateMillis(value: String): Long {
    require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(value))
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
    val parsed = requireNotNull(format.parse(value))
    require(format.format(parsed) == value)
    return parsed.time
}
fun calendarDate(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))
fun nextCalendarDate(value: String): String = calendarDate(calendarDateMillis(value) + 86_400_000L)
fun displayCalendarDate(value: String): String = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(calendarDateMillis(value)))
fun calendarWeekday(value: String): String = SimpleDateFormat("EEE", Locale.forLanguageTag("vi-VN")).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(calendarDateMillis(value)))
fun vietnamTime(iso: String): String {
    return SimpleDateFormat("HH:mm", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh") }.format(Date(isoMillis(iso)))
}
fun isoMillis(iso: String): Long {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
    val parsed = requireNotNull(parser.parse(iso))
    require(parser.format(parsed) == iso)
    return parsed.time
}
val timeWindows = listOf("Tất cả", "00:00–09:00", "09:00–12:00", "12:00–15:00", "15:00–18:00", "18:00–21:00", "21:00–24:00")
fun matchesTimeWindow(slot: ShowtimeDto, window: Int): Boolean {
    require(window in timeWindows.indices)
    if (window == 0) return true
    val time = vietnamTime(slot.startAt).split(':').let { it[0].toInt() * 60 + it[1].toInt() }
    val starts = listOf(0, 9 * 60, 12 * 60, 15 * 60, 18 * 60, 21 * 60)
    return time >= starts[window - 1] && time < (starts.getOrNull(window) ?: 24 * 60)
}
fun upcomingSlots(items: List<ShowtimeDto>, now: Long, window: Int = 0) = items.filter { it.status == "ACTIVE" && isoMillis(it.startAt) > now && matchesTimeWindow(it, window) }.sortedWith(compareBy({ it.startAt }, { it.id }))
fun seatSummary(slot: ShowtimeDto) = if (slot.totalSeats > 0) "Còn ${slot.availableSeats}/${slot.totalSeats}" else "Chưa có dữ liệu ghế"
fun showtimeRange(slot: ShowtimeDto) = "${vietnamTime(slot.startAt)}–${vietnamTime(slot.endAt)}"
fun searchMovies(items: List<Movie>, query: String): List<Movie> {
    fun normalize(s: String) = Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace("đ", "d")
    val q = normalize(query.trim())
    return items.filter { normalize("${it.name} ${it.original}").contains(q) }
}

@Serializable data class MovieDto(val id: String, val title: String, val durationMinutes: Int, val ageRating: String, val posterUrl: String? = null, val originalTitle: String? = null, val releaseDate: String? = null, val synopsis: String? = null, val genre: String? = null, val language: String? = null, val director: String? = null, val cast: String? = null, val sourceUrl: String, val trailerYoutubeId: String? = null, val showtimes: List<ShowtimeDto> = emptyList()) {
    fun movie() = Movie(id, title, originalTitle.orEmpty(), releaseDate?.take(4)?.toIntOrNull() ?: 0, genre.orEmpty(), durationMinutes, ageRating, false, 0, 2f / 3f, director.orEmpty(), cast.orEmpty(), synopsis.orEmpty(), posterUrl, releaseDate, language, sourceUrl, trailerYoutubeId)
}
@Serializable data class CatalogDatesResponse(val businessDate: String, val dates: List<String>)
@Serializable data class MoviesResponse(val businessDate: String, val items: List<MovieDto>, val serverTime: String)
@Serializable data class AuditoriumDto(val id: String, val name: String, val type: String, val capacity: Int, val isExtra: Boolean)
@Serializable data class ShowtimeDto(val id: String, val movieId: String, val sourceKey: String, val startAt: String, val endAt: String, val format: String, val basePrice: String, val status: String, val auditorium: AuditoriumDto, val presentationLabel: String? = null, val availableSeats: Int = 0, val totalSeats: Int = 0)
@Serializable data class ShowtimesResponse(val businessDate: String, val items: List<ShowtimeDto>, val serverTime: String)
data class CatalogDay(val movies: List<Movie>, val showtimes: List<ShowtimeDto>, val serverTime: String)
interface CatalogRepository {
    suspend fun dates(): List<String>
    suspend fun movies(date: String): CatalogDay
    suspend fun movie(id: String): Movie
    suspend fun showtimes(id: String, date: String): ShowtimesResponse
}
interface CatalogApi {
    @GET("movies/dates") suspend fun dates(): CatalogDatesResponse
    @GET("movies") suspend fun movies(@Query("date") date: String): MoviesResponse
    @GET("movies/{id}") suspend fun movie(@Path("id") id: String): MovieDto
    @GET("movies/{id}/showtimes") suspend fun showtimes(@Path("id") id: String, @Query("date") date: String): ShowtimesResponse
}
class ApiCatalogRepository : CatalogRepository {
    private val api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build())
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())).build().create(CatalogApi::class.java)
    override suspend fun dates(): List<String> {
        val r = api.dates()
        calendarDateMillis(r.businessDate)
        check(r.dates == r.dates.distinct().sorted())
        r.dates.forEach { calendarDateMillis(it); check(it >= r.businessDate) }
        return r.dates
    }
    override suspend fun movies(date: String): CatalogDay {
        val r = api.movies(date); check(r.businessDate == date); isoMillis(r.serverTime)
        r.items.forEach { movie -> validateSlots(movie.showtimes, movie.id, date) }
        return CatalogDay(r.items.map { it.movie() }, r.items.flatMap { it.showtimes }, r.serverTime)
    }
    override suspend fun movie(id: String) = api.movie(id).movie().also { check(it.id == id) }
    override suspend fun showtimes(id: String, date: String): ShowtimesResponse { val r = api.showtimes(id,date); check(r.businessDate == date); isoMillis(r.serverTime); validateSlots(r.items,id,date); return r }
    private fun validateSlots(items: List<ShowtimeDto>, id: String, date: String) {
        check(items.map { it.id }.distinct().size == items.size)
        items.forEach { check(it.movieId == id && vietnamDate(Date(isoMillis(it.startAt))) == date && isoMillis(it.endAt) > isoMillis(it.startAt) && it.status == "ACTIVE" && it.availableSeats >= 0 && it.availableSeats <= it.totalSeats) }
    }
}

// Explicit fixture for the existing UI tests; never constructed by MainActivity.
object MockCatalogRepository : CatalogRepository {
    override suspend fun dates() = listOf(vietnamDate())
    override suspend fun movies(date: String) = CatalogDay(MockMovies.all, MockMovies.all.flatMap { showtimes(it.id,date).items }, "${vietnamDate()}T00:00:00.000Z")
    override suspend fun movie(id: String) = MockMovies.all.first { it.id == id }
    override suspend fun showtimes(id: String, date: String) = ShowtimesResponse(date, listOf(ShowtimeDto("fixture-$id-$date",id,"fixture","${date}T16:30:00.000Z","${nextCalendarDate(date)}T00:00:00.000Z","STANDARD_2D","100000","ACTIVE",AuditoriumDto("fixture-room","Cinema 3","STANDARD",160,false),"Phụ đề Việt",160,160)), "${vietnamDate()}T00:00:00.000Z")
}
