package com.nhom8.cineplex.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CineplexViewModel(private val repository: AccountRepository? = null) : ViewModel() {
    constructor(mock: MockAccountRepository) : this(MockAccountAdapter(mock))
    var screen by mutableStateOf(Screen.LOGIN); private set
    var form by mutableStateOf(AuthForm()); private set
    var errors by mutableStateOf<Map<String, String>>(emptyMap()); private set
    var feedback by mutableStateOf(""); private set
    var success by mutableStateOf(false); private set
    var staffRequired by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var session by mutableStateOf<Session?>(null); private set
    var query by mutableStateOf("")
    var soon by mutableStateOf(false)
    var movie by mutableStateOf<Movie?>(null); private set
    var notice by mutableStateOf<Notice?>(null); private set
    var dialog by mutableStateOf<String?>(null); private set
    var googleRequested by mutableStateOf(false); private set
    private var pendingGoogleEmail: String? = null
    private var request: Job? = null
    private var version = 0
    private val repo get() = repository ?: error("Account repository not initialized")
    init { if (repository != null && repository !is MockAccountAdapter) run { repository.restore()?.let(::signedIn) } }
    private fun home() = when (session?.role) { Role.ADMIN -> Screen.ADMIN; Role.STAFF -> Screen.STAFF_HOME; else -> Screen.HOME }
    private fun signedIn(value: Session) { session = value; form = AuthForm(); errors = emptyMap(); screen = home() }
    private fun run(force: Boolean = false, block: suspend () -> Unit) {
        if (loading && !force) return
        val epoch = ++version; loading = true; feedback = ""; errors = emptyMap()
        request = viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: ApiFailure) {
                if (epoch == version) {
                    feedback = e.message; success = false
                    errors = e.fields.mapKeys { if (it.key == "fullName") "name" else it.key }
                    staffRequired = e.code == "WRONG_PORTAL" && screen == Screen.LOGIN
                    if (session != null && e.code in listOf("SESSION_REVOKED", "ACCOUNT_UNAVAILABLE", "INVALID_TOKEN", "SESSION_CANCELED")) { session = null; screen = Screen.LOGIN; dialog = null }
                    if (screen !in listOf(Screen.LOGIN, Screen.SIGNUP, Screen.STAFF)) notice = Notice("Không thực hiện được", e.message)
                }
            } catch (_: Exception) { if (epoch == version) { feedback = "Không thực hiện được. Hãy thử lại."; if (session != null) notice = Notice("Lỗi", feedback) } }
            finally { if (epoch == version) loading = false }
        }
    }
    fun update(value: AuthForm) { if (!loading) form = value }
    fun validateField(key: String) {
        if (loading) return
        val found = repo.validate(form, screen == Screen.SIGNUP)
        errors = errors.toMutableMap().apply { remove(key); found[key]?.let { put(key, it) }; if (key == "password" && form.confirm.isNotEmpty()) { remove("confirm"); found["confirm"]?.let { put("confirm", it) } } }
    }
    fun navigate(target: Screen) {
        if (target !in listOf(Screen.LOGIN, Screen.SIGNUP, Screen.STAFF)) return
        version++; request?.cancel(); request = null; loading = false; googleRequested = false; pendingGoogleEmail = null
        session = null; screen = target; form = AuthForm(); errors = emptyMap(); feedback = ""; success = false; staffRequired = false; notice = null; dialog = null
    }
    fun submit() {
        if (loading || screen !in listOf(Screen.LOGIN, Screen.SIGNUP, Screen.STAFF)) return
        val signup = screen == Screen.SIGNUP
        errors = repo.validate(form, signup); feedback = ""; success = false; staffRequired = false
        if (errors.isNotEmpty()) return
        val submitted = form; val staff = screen == Screen.STAFF
        run {
            if (signup) { val message = repo.register(submitted); screen = Screen.LOGIN; form = AuthForm(email = repo.normalizeEmail(submitted.email)); feedback = message; success = true }
            else when (val r = repo.login(submitted, staff)) {
                is LoginResult.Success -> signedIn(r.session)
                LoginResult.InvalidCredentials -> feedback = "Sai tài khoản hoặc mật khẩu. Vui lòng kiểm tra và thử lại."
                LoginResult.Forbidden -> feedback = "Tài khoản không có quyền truy cập"
                LoginResult.StaffRequired -> { feedback = "Đây là tài khoản quản trị viên. Vui lòng chuyển sang đăng nhập nhân viên."; staffRequired = true }
            }
        }
    }
    fun openMovie(value: Movie) { if (session?.role == Role.CUSTOMER) { movie = value; screen = Screen.DETAIL } }
    fun back() {
        if (notice != null) { dismissNotice(); return }
        if (dialog != null) { if (!loading) dialog = null; return }
        when (screen) { Screen.SIGNUP, Screen.STAFF -> navigate(Screen.LOGIN); Screen.DETAIL -> { movie = null; screen = Screen.HOME }; Screen.PROFILE -> screen = home(); else -> Unit }
    }
    fun logout() {
        query = ""; soon = false; movie = null; navigate(Screen.LOGIN)
        if (repository !is MockAccountAdapter) run { repo.logout() }
    }
    fun dismissNotice() { notice = null }
    fun closeDialog() { if (!loading) { dialog = null; feedback = ""; errors = emptyMap() } }
    fun account() { if (session != null) { notice = null; screen = Screen.PROFILE; refreshProfile() } }
    fun refreshProfile() {
        if (session == null || repository is MockAccountAdapter || loading) return
        run { val oldEmail = session?.email; val current = repo.me(); if (oldEmail != null && current.email != oldEmail) { session = null; screen = Screen.LOGIN; repo.logout(); feedback = "Email đã thay đổi. Hãy đăng nhập lại." } else session = current }
    }
    fun google() {
        if (loading || screen != Screen.LOGIN) return
        if (repository is MockAccountAdapter) notice = Notice("Chức năng sẽ được bổ sung", "Google Login trong test adapter không tạo phiên thật.")
        else { pendingGoogleEmail = null; loading = true; googleRequested = true }
    }
    fun googleFailed(canceled: Boolean) { googleRequested = false; loading = false; pendingGoogleEmail = null; if (!canceled) { feedback = "Không lấy được thông tin Google. Hãy thử lại."; if (session != null) notice = Notice("Google", feedback) } }
    fun googleSuccess(token: String) {
        val email = pendingGoogleEmail; pendingGoogleEmail = null; googleRequested = false
        run(force = true) { if (email == null) signedIn(repo.google(token)) else { val message = repo.changeEmail(email, null, token); dialog = null; notice = Notice("Xác nhận email mới", message) } }
    }
    fun profileAction(feature: String) {
        if (session == null || screen != Screen.PROFILE || loading) return
        if (feature == "help") { notice = Notice("Trợ giúp", "Các chức năng phim/đặt vé sẽ được bổ sung theo tiến độ của đồ án."); return }
        if (feature == "password" && session?.hasPassword == false) { notice = Notice("Tài khoản Google", "Mật khẩu được quản lý bên Google. Tài khoản này không có mật khẩu Cineplex."); return }
        if (repository is MockAccountAdapter) notice = Notice("Chức năng sẽ được bổ sung", "Test adapter không cập nhật máy chủ.") else { feedback = ""; dialog = feature }
    }
    fun forgot() { if (!loading) { feedback = ""; dialog = "forgot" } }
    fun saveName(name: String) { if (name.isBlank() || name.trim().length > 100) { feedback = "Họ tên phải có 1–100 ký tự."; return }; run { session = repo.edit(name); dialog = null } }
    fun sendForgot(email: String) {
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())) { feedback = "Email chưa đúng định dạng."; return }
        run { val message = repo.forgot(email); dialog = null; notice = Notice("Kiểm tra email", message) }
    }
    fun resend() { val email = session?.email ?: form.email; if (email.isNotBlank()) run { notice = Notice("Kiểm tra email", repo.resend(email)) } }
    fun password(current: String, replacement: String, confirm: String) {
        if (current.isEmpty() || replacement.length !in 8..128 || replacement != confirm) { feedback = "Kiểm tra mật khẩu hiện tại, mật khẩu mới 8–128 ký tự và xác nhận."; return }
        run { val message = repo.changePassword(current, replacement); session = null; screen = Screen.LOGIN; dialog = null; repo.logout(); feedback = message; success = true }
    }
    fun email(value: String, password: String) {
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value.trim())) { feedback = "Email chưa đúng định dạng."; return }
        if (session?.hasPassword == false) { pendingGoogleEmail = repo.normalizeEmail(value); loading = true; googleRequested = true }
        else run { val message = repo.changeEmail(value, password, null); dialog = null; notice = Notice("Xác nhận email mới", message) }
    }
    fun avatar(bytes: ByteArray, mime: String) { run { session = repo.avatar(bytes, mime) } }
    fun deleteAvatar() { run { session = repo.deleteAvatar() } }
    fun avatarError() { notice = Notice("Ảnh không hợp lệ", "Hãy chọn JPEG/PNG/WebP không quá 5 MB và thử lại.") }
    fun coming(feature: String) { notice = Notice("Sắp ra mắt", if (feature == "Đặt vé") "Tính năng đặt vé đang được chuẩn bị. Bạn có thể tiếp tục khám phá các bộ phim." else "Mục $feature đang được chuẩn bị. Hãy quay lại sau nhé.") }
    fun terms(privacy: Boolean) {
        notice = if (privacy) Notice("Chính sách bảo mật", "Tài khoản/profile được xử lý bởi backend qua kết nối bảo mật. Mật khẩu Cineplex chỉ lưu dạng hash; refresh token trên thiết bị được mã hóa bằng Android Keystore và không sao lưu. Không ghi mật khẩu hoặc token vào log.")
        else Notice("Điều khoản sử dụng — mẫu", "Bạn dùng tài khoản để đặt vé và quản lý vé. Hãy cung cấp thông tin chính xác và bảo vệ thông tin đăng nhập. Đây là nội dung minh họa, chưa phải điều khoản chính thức.")
    }
}
