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
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
    return SimpleDateFormat("HH:mm", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh") }.format(parser.parse(iso) ?: error("Invalid showtime"))
}
fun searchMovies(items: List<Movie>, query: String): List<Movie> {
    fun normalize(s: String) = Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace("đ", "d")
    val q = normalize(query.trim())
    return items.filter { normalize("${it.name} ${it.original}").contains(q) }
}

@Serializable data class MovieDto(val id: String, val title: String, val durationMinutes: Int, val ageRating: String, val posterUrl: String? = null, val originalTitle: String? = null, val releaseDate: String? = null, val synopsis: String? = null, val genre: String? = null, val language: String? = null, val director: String? = null, val cast: String? = null, val sourceUrl: String) {
    fun movie() = Movie(id, title, originalTitle.orEmpty(), releaseDate?.take(4)?.toIntOrNull() ?: 0, genre.orEmpty(), durationMinutes, ageRating, false, 0, 2f / 3f, director.orEmpty(), cast.orEmpty(), synopsis.orEmpty(), posterUrl, releaseDate, language, sourceUrl)
}
@Serializable data class CatalogDatesResponse(val businessDate: String, val dates: List<String>)
@Serializable data class MoviesResponse(val businessDate: String, val items: List<MovieDto>)
@Serializable data class AuditoriumDto(val id: String, val name: String, val type: String, val capacity: Int, val isExtra: Boolean)
@Serializable data class ShowtimeDto(val id: String, val movieId: String, val sourceKey: String, val startAt: String, val endAt: String, val format: String, val basePrice: String, val status: String, val auditorium: AuditoriumDto)
@Serializable data class ShowtimesResponse(val businessDate: String, val items: List<ShowtimeDto>)
interface CatalogRepository {
    suspend fun dates(): List<String>
    suspend fun movies(date: String): List<Movie>
    suspend fun movie(id: String): Movie
    suspend fun showtimes(id: String, date: String): List<ShowtimeDto>
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
    override suspend fun movies(date: String): List<Movie> { val r = api.movies(date); check(r.businessDate == date); return r.items.map { it.movie() } }
    override suspend fun movie(id: String) = api.movie(id).movie().also { check(it.id == id) }
    override suspend fun showtimes(id: String, date: String): List<ShowtimeDto> { val r = api.showtimes(id,date); check(r.businessDate == date && r.items.all { it.movieId == id }); return r.items }
}

// Explicit fixture for the existing UI tests; never constructed by MainActivity.
object MockCatalogRepository : CatalogRepository {
    override suspend fun dates() = listOf(vietnamDate())
    override suspend fun movies(date: String) = MockMovies.all
    override suspend fun movie(id: String) = MockMovies.all.first { it.id == id }
    override suspend fun showtimes(id: String, date: String) = emptyList<ShowtimeDto>()
}
