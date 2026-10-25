package com.nhom8.cineplex.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom8.cineplex.data.*
import kotlinx.coroutines.*
import java.io.Closeable
import java.util.UUID

class SeatSelectionViewModel(private val repository: SeatRepository? = null,
    private val monotonicMillis: () -> Long = { System.nanoTime()/1_000_000 }) : ViewModel() {
    var userId = ""; private set
    var showtimeId by mutableStateOf(""); private set
    var seats by mutableStateOf<List<SeatDto>>(emptyList()); private set
    var layout by mutableStateOf<SeatLayout?>(null); private set
    var selected by mutableStateOf<Set<String>>(emptySet()); private set
    var order by mutableStateOf<HoldOrder?>(null); private set
    var deadlineMillis by mutableStateOf<Long?>(null); private set
    var secondsLeft by mutableStateOf(0); private set
    var online by mutableStateOf(false); private set
    var syncing by mutableStateOf(false); private set
    var confirming by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var expiredDialog by mutableStateOf(false); private set
    var leaveDialog by mutableStateOf(false); private set
    var message by mutableStateOf(""); private set
    var realtime by mutableStateOf(false); private set
    var resumeOrder by mutableStateOf<HoldOrder?>(null); private set
    private var resumeJob: Job?=null
    private var until=0L
    private var epoch=0
    private var request: HoldRequest? = null
    private var socket: Closeable? = null
    private var foreground=false
    private var poll: Job? = null
    private var load: Job? = null
    private var socketJob: Job? = null
    private var action: Job? = null
    private var dirty=false
    val pending: String? get() = if(confirming) "order" else null
    val chosen get() = seats.filter { it.id in selected }
    val places get() = if(held) order!!.seats.size else chosen.size
    val total get() = if(held) order!!.totalAmount.toLong() else chosen.sumOf { it.price.toLong() }
    val held get() = order?.status=="PENDING_PAYMENT"
    val canContinue get() = online && !loading && !confirming && !syncing && selected.isNotEmpty() && !held &&
        selected.all { id -> seats.any { it.id==id && it.status=="AVAILABLE" } }

    fun bind(user: String, id: String) {
        if(user==userId && id==showtimeId) return
        detach(); epoch++; action?.cancel(); load?.cancel()
        userId=user; showtimeId=id; seats=emptyList(); layout=null; selected=emptySet(); order=null; request=null
        deadlineMillis=null; secondsLeft=0; until=0; expiredDialog=false; leaveDialog=false; message=""; online=false; confirming=false
        val version=epoch
        load=viewModelScope.launch {
            loading=true
            try {
                val repo=checkNotNull(repository) { "Không có kết nối dữ liệu ghế." }
                val saved=repo.saved(user,id)
                val snapshot=repo.seats(id)
                if(version!=epoch) return@launch
                seats=snapshot.items; layout=snapshot.layout; online=true; request=saved
                if(saved!=null) {
                    try { accept(repo.order(saved.key)) }
                    catch(e: ApiFailure) { if(e.code!="ORDER_NOT_FOUND") throw e else { selected=saved.seatIds.toSet(); message="Yêu cầu giữ chỗ chưa được xác nhận. Nhấn Đặt vé để thử lại." } }
                }
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) { if(version==epoch) { online=false; message=e.message ?: "Không tải được sơ đồ ghế." } }
            finally { if(version==epoch) { loading=false; load=null; if(foreground) attach(); if(dirty) refresh() } }
        }
    }
    private fun pair(id: String): List<SeatDto> {
        val seat=seats.firstOrNull { it.id==id } ?: return emptyList()
        if(seat.seatType!="COUPLE") return listOf(seat)
        val partner="${seat.rowName}${if(seat.seatNumber%2==1) seat.seatNumber+1 else seat.seatNumber-1}"
        return seats.filter { it.id==id || it.seatCode==partner }.takeIf { it.size==2 && it.all { s->s.seatType=="COUPLE" } } ?: emptyList()
    }
    fun select(id: String) {
        if(held) { message="Hãy hủy giữ chỗ hiện tại trước khi đổi ghế."; return }
        if(!online || loading || syncing || confirming) return
        if(request!=null) { message="Hãy xác nhận hoặc hủy yêu cầu giữ chỗ trước khi đổi ghế."; return }
        val group=pair(id)
        if(group.isEmpty() || group.any { it.status!="AVAILABLE" }) { message="Ghế không còn khả dụng."; return }
        val ids=group.map { it.id }.toSet()
        selected=if(selected.containsAll(ids)) selected-ids else selected+ids; message=""
    }
    fun continueToSummary(onSuccess: () -> Unit) {
        if(!canContinue) return
        val version=epoch; val user=userId; val id=showtimeId; confirming=true
        action=viewModelScope.launch {
            try {
                val repo=checkNotNull(repository)
                val sent=request ?: HoldRequest(UUID.randomUUID().toString(),id,selected.sorted()).also { request=it }
                repo.remember(user,id,sent)
                val response=repo.hold(sent)
                if(version!=epoch) return@launch
                accept(response)
                if(held && secondsLeft>0) onSuccess()
                refresh()
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) { if(version==epoch) {
                  message=e.message ?: "Chưa xác nhận được giữ chỗ. Vui lòng thử lại."
                  if(e is ApiFailure && e.code !in setOf("NETWORK_ERROR","HTTP_502","HTTP_503","HTTP_504")) {
                      request=null; repository?.remember(user,id,null); refresh()
                  }
              } }
            finally { if(version==epoch) confirming=false }
        }
    }
    private suspend fun accept(response: HoldResponse) {
        val current=response.item
        check(current.showtimeId==showtimeId && current.id==request?.key)
        check(current.status in setOf("PENDING_PAYMENT","EXPIRED","CANCELED","PAID","REFUNDED"))
        check(current.seats.map { it.id }.sorted()==request!!.seatIds.sorted())
        isoMillis(response.serverTime); isoMillis(current.expiresAt); check(current.totalAmount.toLong()>=0)
        order=current
        if(held) {
            selected=current.seats.map { it.id }.toSet()
            val deadline=isoMillis(current.expiresAt)
            val anchor=monotonicMillis()+(deadline-isoMillis(response.serverTime)).coerceAtLeast(0)
            until=if(deadlineMillis==deadline && until>0) minOf(until,anchor) else anchor
            deadlineMillis=deadline; expiredDialog=false; refreshClock()
        } else {
            selected=emptySet(); deadlineMillis=null; secondsLeft=0
            expiredDialog=current.status=="EXPIRED"
            repository?.remember(userId,showtimeId,null); request=null
        }
    }
    fun refreshClock() {
        secondsLeft=if(deadlineMillis==null) 0 else ((until-monotonicMillis()+999)/1000).coerceAtLeast(0).toInt()
        if(held && secondsLeft==0 && !expiredDialog) { expiredDialog=true; refresh() }
    }
    fun refresh() {
        if(showtimeId.isBlank()) return
        if(load?.isActive==true) { dirty=true; return }
        val version=epoch; val id=showtimeId; dirty=false
        load=viewModelScope.launch {
            syncing=true
            try {
                val snapshot=checkNotNull(repository).seats(id)
                if(version!=epoch || dirty) return@launch
                seats=snapshot.items; layout=snapshot.layout; online=true
                if(request!=null) {
                    try { accept(repository.order(request!!.key)) }
                    catch(e: ApiFailure) { if(e.code!="ORDER_NOT_FOUND") throw e }
                } else {
                    val invalid=selected.filter { seatId -> pair(seatId).isEmpty() || pair(seatId).any { it.status!="AVAILABLE" } }.toSet()
                    if(invalid.isNotEmpty()) { selected=selected-invalid; message="Một số ghế không còn khả dụng. Lựa chọn và tổng tiền đã cập nhật." }
                }
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) { if(version==epoch) { online=false; message=e.message ?: "Không đồng bộ được ghế." } }
            finally { if(version==epoch) { syncing=false; load=null; if(dirty) refresh() } }
        }
    }
    fun cancelHold(onSuccess: () -> Unit = {}) {
        if(confirming) return
        if(request==null) { release(); onSuccess(); return }
        val sent=request!!; val version=epoch; confirming=true
        action=viewModelScope.launch {
            try {
                val repo=checkNotNull(repository)
                if(order==null) repo.hold(sent)
                val response=repo.cancel(sent.key)
                if(version!=epoch) return@launch
                accept(response)
                if(!held) { message="Đã hủy giữ chỗ."; leaveDialog=false; onSuccess(); refresh() }
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) { if(version==epoch) {
                  if(e is ApiFailure && e.code=="ORDER_NOT_FOUND") { repository?.remember(userId,showtimeId,null); request=null; release(); onSuccess() }
                  else message="Chưa xác nhận được hủy giữ chỗ. Ghế vẫn được giữ đến hạn nếu server chưa nhận yêu cầu."
              } }
            finally { if(version==epoch) confirming=false }
        }
    }
    fun requestLeave() { leaveDialog=true }
    fun findResume(user: String) {
        val repo=repository ?: return
        resumeJob?.cancel(); val version=epoch
        resumeJob=viewModelScope.launch {
            try {
                resumeOrder=null
                for(saved in repo.requests(user)) {
                    val response=try { repo.order(saved.key) } catch(e: ApiFailure) { if(e.code=="ORDER_NOT_FOUND") continue else throw e }
                    if(version!=epoch) return@launch
                    if(response.item.status=="PENDING_PAYMENT" && isoMillis(response.item.expiresAt)>isoMillis(response.serverTime)) { resumeOrder=response.item; break }
                    repo.remember(user,saved.showtimeId,null)
                }
            } catch(e: CancellationException) { throw e } catch(_: Exception) { /* Retry on the next Home/resume. */ }
        }
    }
    fun stay() { leaveDialog=false }
    fun dismissExpired() { expiredDialog=false }
    fun clearSession() {
        detach(); epoch++; action?.cancel(); load?.cancel(); resumeJob?.cancel(); resumeOrder=null; load=null
        userId=""; showtimeId=""; seats=emptyList(); layout=null; selected=emptySet(); order=null; request=null
        deadlineMillis=null; secondsLeft=0; confirming=false; loading=false; syncing=false; online=false; leaveDialog=false; expiredDialog=false; message=""
    }
    fun release() {
        if(request!=null || held || confirming) { requestLeave(); return }
        selected=emptySet(); leaveDialog=false; message=""
    }
    fun foreground(active: Boolean) {
        foreground=active
        if(active) { refreshClock(); refresh(); attach() } else detach()
    }
    private fun attach() {
        if(!foreground || showtimeId.isBlank() || repository==null) return
        if(poll?.isActive!=true) poll=viewModelScope.launch { while(isActive) { delay(10_000); refresh(); if(!realtime) openSocket() } }
        openSocket()
    }
    private fun openSocket() {
        if(socketJob?.isActive==true || realtime || !foreground || repository==null) return
        val version=epoch; val id=showtimeId
        socketJob=viewModelScope.launch {
            try {
                socket?.close(); socket=null
                val opened=repository.watch(id, { viewModelScope.launch { if(version==epoch) refresh() } },
                    { connected -> viewModelScope.launch { if(version==epoch) realtime=connected } })
                if(version!=epoch || !foreground) opened.close() else socket=opened
            } catch(e: CancellationException) { throw e } catch(_: Exception) { if(version==epoch) realtime=false }
        }
    }
    private fun detach() { poll?.cancel(); poll=null; socketJob?.cancel(); socket?.close(); socket=null; realtime=false }
    override fun onCleared() { detach() }
}
