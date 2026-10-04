package com.nhom8.cineplex.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom8.cineplex.data.MockAccountRepository
import com.nhom8.cineplex.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CineplexViewModel(private val repository: MockAccountRepository = MockAccountRepository.process) : ViewModel() {
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
    private var request: Job? = null
    fun update(value: AuthForm) { if (!loading) form = value }
    fun validateField(key: String) {
        if (loading) return
        val found = repository.validate(form, screen == Screen.SIGNUP)
        errors = errors.toMutableMap().apply {
            remove(key); found[key]?.let { put(key, it) }
            if (key == "password" && form.confirm.isNotEmpty()) { remove("confirm"); found["confirm"]?.let { put("confirm", it) } }
        }
    }
    fun navigate(target: Screen) {
        if (target !in listOf(Screen.LOGIN, Screen.SIGNUP, Screen.STAFF)) return
        request?.cancel(); request = null; loading = false
        session = null; screen = target; form = AuthForm(); errors = emptyMap()
        feedback = ""; success = false; staffRequired = false; notice = null
    }
    fun submit() {
        if (loading || screen !in listOf(Screen.LOGIN, Screen.SIGNUP, Screen.STAFF)) return
        val signup = screen == Screen.SIGNUP
        errors = repository.validate(form, signup); feedback = ""; success = false; staffRequired = false
        if (errors.isNotEmpty()) return
        val submitted = form
        val staff = screen == Screen.STAFF
        loading = true
        request = viewModelScope.launch {
            delay(900)
            if (signup) {
                errors = repository.register(submitted)
                if (errors.isEmpty()) {
                    screen = Screen.LOGIN
                    form = AuthForm(email = repository.normalizeEmail(submitted.email))
                    feedback = "Đăng ký thành công! Vui lòng đăng nhập"; success = true
                }
            } else when (val result = repository.login(submitted, staff)) {
                is LoginResult.Success -> {
                    session = result.session; form = AuthForm(); errors = emptyMap()
                    screen = if (result.session.role == Role.ADMIN) Screen.ADMIN else Screen.HOME
                }
                LoginResult.InvalidCredentials -> feedback = "Sai tài khoản hoặc mật khẩu. Vui lòng kiểm tra và thử lại."
                LoginResult.Forbidden -> feedback = "Tài khoản không có quyền truy cập"
                LoginResult.StaffRequired -> {
                    feedback = "Đây là tài khoản quản trị viên. Vui lòng chuyển sang đăng nhập nhân viên."
                    staffRequired = true
                }
            }
            loading = false
        }
    }
    fun openMovie(value: Movie) {
        if (session?.role == Role.USER) { movie = value; screen = Screen.DETAIL }
    }
    fun back() {
        if (notice != null) { dismissNotice(); return }
        when(screen) {
            Screen.SIGNUP, Screen.STAFF -> navigate(Screen.LOGIN)
            Screen.DETAIL -> { movie = null; screen = Screen.HOME }
            else -> Unit
        }
    }
    fun logout() {
        query = ""; soon = false; movie = null
        navigate(Screen.LOGIN)
    }
    fun dismissNotice() { notice = null }
    fun account() {
        session?.let { notice = Notice("Tài khoản Cineplex", "${it.name} · ${if(it.role == Role.ADMIN) "Quản trị viên" else "Khách hàng"}", true) }
    }
    fun coming(feature: String) {
        notice = Notice("Sắp ra mắt", if(feature == "Đặt vé") "Tính năng đặt vé đang được chuẩn bị. Bạn có thể tiếp tục khám phá các bộ phim." else "Mục $feature đang được chuẩn bị. Hãy quay lại sau nhé.")
    }
    fun forgot() { notice = Notice("Chức năng sẽ được bổ sung", "Tính năng khôi phục mật khẩu chưa được triển khai.") }
    fun terms(privacy: Boolean) {
        notice = if(privacy) Notice("Chính sách bảo mật — mẫu", "Prototype chỉ giữ tài khoản trong bộ nhớ phiên thử nghiệm. Không gửi dữ liệu tới backend, không lưu mật khẩu vào storage hoặc log. Kết thúc tiến trình app sẽ xóa tài khoản mới. Đây chưa phải chính sách chính thức.")
        else Notice("Điều khoản sử dụng — mẫu", "Bạn dùng tài khoản để đặt vé và quản lý vé. Hãy cung cấp thông tin chính xác và bảo vệ thông tin đăng nhập. Đây là nội dung minh họa, chưa phải điều khoản chính thức.")
    }
}
