package com.nhom8.cineplex.model

enum class Role { CUSTOMER, STAFF, ADMIN }
enum class Screen { LOGIN, SIGNUP, STAFF, HOME, DETAIL, ADMIN, PROFILE, STAFF_HOME }
data class Session(val name: String, val role: Role, val email: String, val id: String = "", val avatarUrl: String? = null, val emailVerifiedAt: String? = null, val hasPassword: Boolean = true)
data class AuthForm(val name: String = "", val email: String = "", val password: String = "", val confirm: String = "", val consent: Boolean = false) {
    override fun toString() = "AuthForm(name=$name, email=$email, password=<redacted>, confirm=<redacted>, consent=$consent)"
}
data class Movie(val id: String, val name: String, val original: String, val year: Int, val genre: String, val minutes: Int, val age: String, val soon: Boolean, val poster: Int, val ratio: Float, val director: String, val cast: String, val description: String, val posterUrl: String? = null, val releaseDate: String? = null, val language: String? = null, val sourceUrl: String? = null)
sealed interface LoginResult {
    data class Success(val session: Session) : LoginResult
    data object InvalidCredentials : LoginResult
    data object Forbidden : LoginResult
    data object StaffRequired : LoginResult
}
data class Notice(val title: String, val message: String, val account: Boolean = false)
