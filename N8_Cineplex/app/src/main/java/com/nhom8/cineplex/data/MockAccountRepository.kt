package com.nhom8.cineplex.data

import com.nhom8.cineplex.model.*
import java.util.Locale

/** Mock credentials live only in this process; no disk, saved state or logging. */
class MockAccountRepository {
    private class Account(val name: String, val email: String, val password: String, val role: Role)
    private val accounts = mutableListOf(
        Account("Khách hàng", "user@cineplex.test", "123456", Role.USER),
        Account("Quản trị viên", "admin@cineplex.test", "123456", Role.ADMIN)
    )
    fun normalizeEmail(value: String) = value.trim().lowercase(Locale.ROOT)
    fun validate(form: AuthForm, signup: Boolean): Map<String, String> = buildMap {
        if (signup && form.name.isBlank()) put("name", "Vui lòng nhập họ và tên.")
        if (form.email.isBlank()) put("email", "Vui lòng nhập email.")
        else if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(form.email.trim()))
            put("email", "Email chưa đúng định dạng. Hãy nhập dạng ten@mien.com.")
        else if (signup && accounts.any { it.email == normalizeEmail(form.email) })
            put("email", "Email đã được đăng ký. Hãy đăng nhập hoặc dùng email khác.")
        if (form.password.isEmpty()) put("password", "Vui lòng nhập mật khẩu.")
        else if (signup && form.password.codePointCount(0, form.password.length) < 8)
            put("password", "Mật khẩu phải có ít nhất 8 ký tự.")
        if (signup) {
            if (form.confirm.isEmpty()) put("confirm", "Vui lòng xác nhận mật khẩu.")
            else if (form.confirm != form.password) put("confirm", "Xác nhận mật khẩu không khớp. Vui lòng nhập lại.")
            if (!form.consent) put("consent", "Vui lòng đồng ý với điều khoản và chính sách bảo mật.")
        }
    }
    fun login(form: AuthForm, staff: Boolean): LoginResult {
        val account = accounts.find { it.email == normalizeEmail(form.email) && it.password == form.password }
            ?: return LoginResult.InvalidCredentials
        if (staff && account.role != Role.ADMIN) return LoginResult.Forbidden
        if (!staff && account.role == Role.ADMIN) return LoginResult.StaffRequired
        return LoginResult.Success(Session(account.name, account.role))
    }
    fun register(form: AuthForm): Map<String, String> {
        val errors = validate(form, true)
        if (errors.isEmpty()) accounts.add(Account(form.name.trim(), normalizeEmail(form.email), form.password, Role.USER))
        return errors
    }
    companion object { val process = MockAccountRepository() }
}
