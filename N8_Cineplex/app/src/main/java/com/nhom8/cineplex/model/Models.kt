package com.nhom8.cineplex.model

enum class Role { USER, ADMIN }
enum class Screen { LOGIN, SIGNUP, STAFF, HOME, DETAIL, ADMIN }
data class Session(val name: String, val role: Role)
data class AuthForm(val name: String = "", val email: String = "", val password: String = "", val confirm: String = "", val consent: Boolean = false) {
    override fun toString() = "AuthForm(name=$name, email=$email, password=<redacted>, confirm=<redacted>, consent=$consent)"
}
data class Movie(val id: String, val name: String, val original: String, val year: Int, val genre: String, val minutes: Int, val age: String, val soon: Boolean, val poster: Int, val ratio: Float, val director: String, val cast: String, val description: String)
sealed interface LoginResult {
    data class Success(val session: Session) : LoginResult
    data object InvalidCredentials : LoginResult
    data object Forbidden : LoginResult
    data object StaffRequired : LoginResult
}
data class Notice(val title: String, val message: String, val account: Boolean = false)
