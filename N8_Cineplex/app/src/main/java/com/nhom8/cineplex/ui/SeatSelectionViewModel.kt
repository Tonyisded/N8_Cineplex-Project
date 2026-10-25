package com.nhom8.cineplex.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom8.cineplex.data.DemoSeat
import com.nhom8.cineplex.data.SeatAvailability
import com.nhom8.cineplex.data.SeatDemo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/** UI-only reservation simulation. No order, server hold, or shared realtime channel exists yet. */
class SeatSelectionViewModel(private val clock: () -> Long = System::currentTimeMillis) : ViewModel() {
    companion object { var debugCurrent: WeakReference<SeatSelectionViewModel>? = null; private set }
    var showtimeId by mutableStateOf(""); private set
    var seats by mutableStateOf(SeatDemo.initial()); private set
    private var snapshot = seats.associate { it.id to it.availability }.toMutableMap()
    private var codeSnapshot = seats.flatMap { seat -> seat.codes.map { it to seat.availability } }.toMap().toMutableMap()
    var selected by mutableStateOf<Set<String>>(emptySet()); private set
    var pending by mutableStateOf<String?>(null); private set
    var deadlineMillis by mutableStateOf<Long?>(null); private set
    var secondsLeft by mutableStateOf(0); private set
    var online by mutableStateOf(true); private set
    var syncing by mutableStateOf(false); private set
    var confirming by mutableStateOf(false); private set
    var expiredDialog by mutableStateOf(false); private set
    var leaveDialog by mutableStateOf(false); private set
    var message by mutableStateOf(""); private set
    private var epoch = 0
    private var pendingJob: Job? = null
    private var syncJob: Job? = null
    private val failNext = mutableSetOf<String>()

    init { debugCurrent = WeakReference(this) }

    val chosen: List<DemoSeat> get() = seats.filter { it.id in selected || it.id == pending }
    val places: Int get() = chosen.sumOf { it.places }
    val total: Long get() = chosen.sumOf { it.price }
    val canContinue: Boolean get() = online && !syncing && !confirming && pending == null && selected.isNotEmpty() && secondsLeft > 0 &&
        selected.all { id -> seats.any { it.id == id && it.availability == SeatAvailability.AVAILABLE } }

    fun bind(id: String) {
        if (id == showtimeId) { refreshClock(); return }
        release()
        showtimeId = id
        seats = SeatDemo.initial()
        snapshot = seats.associate { it.id to it.availability }.toMutableMap()
        codeSnapshot = seats.flatMap { seat -> seat.codes.map { it to seat.availability } }.toMap().toMutableMap()
        online = true; syncing = false; message = ""
    }

    fun refreshClock() {
        val deadline = deadlineMillis
        secondsLeft = if (deadline == null) 0 else ((deadline - clock() + 999) / 1_000).coerceAtLeast(0).toInt()
        if (deadline != null && secondsLeft == 0) expire()
    }

    private fun expire() {
        epoch++; pendingJob?.cancel(); pending = null; selected = emptySet(); deadlineMillis = null
        confirming = false; leaveDialog = false; secondsLeft = 0; expiredDialog = true
        message = "Đã hết thời gian. Lựa chọn mô phỏng được xóa."
    }

    fun dismissExpired() { expiredDialog = false }
    fun requestLeave() { if (selected.isNotEmpty() || pending != null || confirming) leaveDialog = true else release() }
    fun stay() { leaveDialog = false }
    fun release() {
        epoch++; pendingJob?.cancel(); syncJob?.cancel()
        selected = emptySet(); pending = null; deadlineMillis = null; secondsLeft = 0
        confirming = false; syncing = false; expiredDialog = false; leaveDialog = false; failNext.clear()
    }

    fun select(id: String) {
        refreshClock()
        val seat = seats.firstOrNull { it.id == id } ?: return
        if (!online) { message = "Mất kết nối mô phỏng. Chờ đồng bộ để chọn ghế."; return }
        if (syncing || confirming || pending != null) { message = "Đang cập nhật ghế. Vui lòng đợi."; return }
        if (seat.availability != SeatAvailability.AVAILABLE) {
            message = "Ghế ${seat.label} ${if (seat.availability == SeatAvailability.SOLD) "đã bán" else "đang được người khác giữ"}."
            return
        }
        if (id in selected) {
            selected = selected - id
            message = "Đã bỏ chọn ${seat.label}${if (seat.places == 2) " (cả cặp)" else ""}."
            return
        }
        val token = epoch
        pending = id
        message = "Đang xử lý ${seat.label}…"
        pendingJob = viewModelScope.launch {
            delay(650)
            if (token != epoch || pending != id || !online) return@launch
            pending = null
            val actual = snapshot[id] ?: SeatAvailability.AVAILABLE
            if (id in failNext || actual != SeatAvailability.AVAILABLE) {
                failNext.remove(id)
                val conflict = if (actual == SeatAvailability.AVAILABLE) SeatAvailability.HELD else actual
                snapshot[id] = conflict
                seat.codes.forEach { codeSnapshot[it] = conflict }
                updateSeat(id, conflict)
                message = "Ghế ${seat.label} vừa được người khác giữ. Hãy chọn ghế khác."
                return@launch
            }
            refreshClock()
            selected = selected + id
            if (deadlineMillis == null) deadlineMillis = clock() + 600_000
            refreshClock()
            message = "Đã chọn ${seat.label} trong bản mô phỏng · ${seat.places} chỗ."
        }
    }

    fun continueToSummary(onSuccess: () -> Unit) {
        refreshClock()
        if (!canContinue) { message = "Hãy chọn ghế hợp lệ và kiểm tra kết nối để tiếp tục."; return }
        confirming = true
        val token = epoch
        viewModelScope.launch {
            delay(800)
            if (token != epoch) return@launch
            confirming = false
            refreshClock()
            if (canContinue) onSuccess() else message = "Ghế hoặc thời gian đã thay đổi. Hãy kiểm tra lại."
        }
    }

    private fun updateSeat(id: String, status: SeatAvailability) {
        seats = seats.map { if (it.id == id) it.copy(availability = status) else it }
        if (status != SeatAvailability.AVAILABLE) selected = selected - id
    }

    /** Called only from the separate debug control surface. */
    fun debugChange(id: String, status: SeatAvailability) {
        val seat = seats.firstOrNull { it.id == id } ?: return
        seat.codes.forEach { codeSnapshot[it] = status }
        snapshot[id] = status
        if (online && !syncing) {
            val removed = id in selected && status != SeatAvailability.AVAILABLE
            updateSeat(id, status)
            message = if (removed) "Ghế $id không còn khả dụng; lựa chọn và tổng tiền đã cập nhật." else "Ghế $id: ${status.name.lowercase()} (mô phỏng)."
        } else message = "Có thay đổi ghế chờ đồng bộ mô phỏng."
    }

    fun debugChangeCode(code: String, status: SeatAvailability) {
        val seat = seats.firstOrNull { code in it.codes } ?: return
        codeSnapshot[code] = status
        val pairStatus = when {
            seat.codes.any { codeSnapshot[it] == SeatAvailability.SOLD } -> SeatAvailability.SOLD
            seat.codes.any { codeSnapshot[it] == SeatAvailability.HELD } -> SeatAvailability.HELD
            else -> SeatAvailability.AVAILABLE
        }
        snapshot[seat.id] = pairStatus
        if (online && !syncing) {
            val removed = seat.id in selected && pairStatus != SeatAvailability.AVAILABLE
            updateSeat(seat.id, pairStatus)
            message = if (removed) "Một chỗ của ${seat.label} không còn khả dụng; cả cặp đã bỏ chọn." else "${seat.label}: một chỗ đổi trạng thái (mô phỏng)."
        } else message = "Có thay đổi ghế chờ đồng bộ mô phỏng."
    }

    fun debugFailNext(id: String) { if (seats.any { it.id == id }) failNext.add(id) }
    fun debugOffline() {
        online = false; syncing = false; epoch++; pendingJob?.cancel(); pending = null; confirming = false
        message = "Mất kết nối mô phỏng. Không thể chọn ghế hoặc tiếp tục."
    }
    fun debugReconnect() {
        online = true; syncing = true
        val token = ++epoch
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(900)
            if (token != epoch || !online) return@launch
            val removed = selected.filter { snapshot[it] != SeatAvailability.AVAILABLE }
            seats = seats.map { it.copy(availability = snapshot[it.id] ?: SeatAvailability.AVAILABLE) }
            selected = selected - removed.toSet()
            syncing = false
            refreshClock()
            message = if (removed.isEmpty()) "Đã đồng bộ trạng thái mô phỏng và thời gian còn lại." else "Ghế ${removed.joinToString()} không còn khả dụng; tổng tiền đã cập nhật."
        }
    }
    fun debugSetRemaining(millis: Long) {
        if (deadlineMillis != null) { deadlineMillis = clock() + millis; refreshClock() }
    }
    fun debugReset() {
        release()
        seats = SeatDemo.initial()
        snapshot = seats.associate { it.id to it.availability }.toMutableMap()
        codeSnapshot = seats.flatMap { seat -> seat.codes.map { it to seat.availability } }.toMap().toMutableMap()
        online = true
        message = "Đã đặt lại phiên mô phỏng."
    }
}
