package com.nhom8.cineplex.data

import com.nhom8.cineplex.model.*
import kotlinx.coroutines.delay
import java.util.Locale

class ApiFailure(val code: String, override val message: String, val fields: Map<String, String> = emptyMap()) : Exception(message)
interface AccountRepository {
    fun normalizeEmail(value: String) = value.trim().lowercase(Locale.ROOT)
    fun validate(form: AuthForm, signup: Boolean): Map<String, String>
    suspend fun register(form: AuthForm): String
    suspend fun login(form: AuthForm, staff: Boolean): LoginResult
    suspend fun restore(): Session? = null
    suspend fun me(): Session = throw ApiFailure("UNAVAILABLE", "Không đọc được profile.")
    suspend fun logout() {}
    suspend fun google(idToken: String): Session = throw ApiFailure("UNAVAILABLE", "Google Login chưa khả dụng.")
    suspend fun forgot(email: String): String = throw ApiFailure("UNAVAILABLE", "Chưa gửi được email.")
    suspend fun resend(email: String): String = throw ApiFailure("UNAVAILABLE", "Chưa gửi được email.")
    suspend fun edit(name: String): Session = throw ApiFailure("UNAVAILABLE", "Chưa cập nhật được profile.")
    suspend fun changePassword(current: String, replacement: String): String = throw ApiFailure("UNAVAILABLE", "Chưa đổi được mật khẩu.")
    suspend fun changeEmail(email: String, password: String?, idToken: String?): String = throw ApiFailure("UNAVAILABLE", "Chưa đổi được email.")
    suspend fun avatar(bytes: ByteArray, mime: String): Session = throw ApiFailure("UNAVAILABLE", "Chưa cập nhật được ảnh.")
    suspend fun deleteAvatar(): Session = throw ApiFailure("UNAVAILABLE", "Chưa xóa được ảnh.")
}

/** Explicit test adapter. The application never selects it on network failure. */
class MockAccountAdapter(private val mock: MockAccountRepository) : AccountRepository {
    override fun validate(form: AuthForm, signup: Boolean) = mock.validate(form, signup)
    override suspend fun register(form: AuthForm): String {
        delay(900); val errors = mock.register(form)
        if (errors.isNotEmpty()) throw ApiFailure("VALIDATION_ERROR", "Dữ liệu không hợp lệ.", errors)
        return "Đăng ký thành công! Vui lòng đăng nhập"
    }
    override suspend fun login(form: AuthForm, staff: Boolean): LoginResult { delay(900); return mock.login(form, staff) }
}

fun validateAuth(form: AuthForm, signup: Boolean): Map<String, String> = buildMap {
    if (signup && (form.name.isBlank() || form.name.trim().length > 100)) put("name", "Họ tên phải có 1–100 ký tự.")
    if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(form.email.trim()) || form.email.trim().length > 254) put("email", "Email chưa đúng định dạng.")
    if (form.password.isEmpty() || form.password.length > 128 || (signup && form.password.length < 8)) put("password", if (signup) "Mật khẩu phải có 8–128 ký tự." else "Vui lòng nhập mật khẩu, tối đa 128 ký tự.")
    if (signup && form.confirm != form.password) put("confirm", "Xác nhận mật khẩu không khớp.")
    if (signup && !form.consent) put("consent", "Vui lòng đồng ý điều khoản và chính sách.")
}
