package com.nhom8.cineplex

import com.nhom8.cineplex.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Test-only committed snapshots; never constructed by customer code. */
class FakeSeatRepository(val type: String="STANDARD",private val clock: ()->Long={0L}) : SeatRepository {
    private val sizes=when(type) { "IMAX"->listOf(18,22,26,30,32,34,36,36,38,38,38,38,38,36,36); "GOLD"->List(4){8}; "SCREENX"->List(10){18}; else->listOf(12,14,14,14,15,15,15,15,15,15,16) }
    var items=sizes.flatMapIndexed { row,count -> (1..count).map { n ->
        val name="ABCDEFGHJKLMNOPQRSTUVWXYZ"[row].toString()
        val kind=when { type=="GOLD"->"GOLD"; type=="IMAX"->"VIP"; row==sizes.lastIndex->"COUPLE"; row<3->"NORMAL"; else->"VIP" }
        SeatDto("$name$n","$name$n",name,n,kind,if(type=="IMAX") "150000" else "100000","AVAILABLE")
    } }
    private val positions=items.map { s -> val ri="ABCDEFGHJKLMNOPQRSTUVWXYZ".indexOf(s.rowName); SeatPosition(s.id,1f+(sizes.max()-sizes[ri])/2f+sizes[ri]-s.seatNumber,2.4f+ri*1.15f,if(s.seatType=="COUPLE") "${s.rowName}${(s.seatNumber-1)/2*2+1}" else null) }
    val layout=SeatLayout("$type-fixture",sizes.max()+2f,sizes.size*1.15f+4,if(type=="SCREENX") listOf("FRONT","LEFT","RIGHT") else listOf("FRONT"),positions)
    var offline=false
    var loseResponse=false
    var holds=0
    private val orders=mutableMapOf<String,HoldOrder>()
    private val saved=mutableMapOf<String,HoldRequest>()
    private val format=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.ROOT).apply { timeZone=TimeZone.getTimeZone("UTC") }
    private fun now()=isoMillis("2026-10-25T00:00:00.000Z")+clock()
    private fun iso(ms: Long)=format.format(Date(ms))
    private fun checkOnline() { if(offline) throw ApiFailure("NETWORK_ERROR","Mất kết nối") }
    override suspend fun seats(showtimeId: String): SeatResponse { checkOnline(); return SeatResponse(iso(now()),showtimeId,items,layout) }
    override suspend fun hold(request: HoldRequest): HoldResponse {
        checkOnline(); orders[request.key]?.let { return order(it.id) }
        val chosen=items.filter { it.id in request.seatIds }
        if(chosen.size!=request.seatIds.size || chosen.any { it.status!="AVAILABLE" }) throw ApiFailure("SEAT_UNAVAILABLE","Ghế không còn khả dụng")
        val total=chosen.sumOf { it.price.toLong() }.toString(); holds++
        val o=HoldOrder(request.key,request.showtimeId,"PENDING_PAYMENT",total,"0","0",total,total,iso(now()+600000),chosen.map { OrderSeatDto(it.id,it.seatCode,it.price) })
        orders[o.id]=o; items=items.map { if(it.id in request.seatIds) it.copy(status="HELD") else it }
        if(loseResponse) { loseResponse=false; throw ApiFailure("NETWORK_ERROR","Mất phản hồi") }
        return HoldResponse(iso(now()),o)
    }
    override suspend fun order(id: String): HoldResponse {
        checkOnline(); var o=orders[id] ?: throw ApiFailure("ORDER_NOT_FOUND","Không tìm thấy đơn")
        if(o.status=="PENDING_PAYMENT" && isoMillis(o.expiresAt)<=now()) { o=o.copy(status="EXPIRED"); orders[id]=o; release(o) }
        return HoldResponse(iso(now()),o)
    }
    private fun release(o: HoldOrder) { items=items.map { if(o.seats.any { s->s.id==it.id }) it.copy(status="AVAILABLE") else it } }
    override suspend fun cancel(id: String): HoldResponse {
        val o=order(id).item
        if(o.status=="PENDING_PAYMENT") { orders[id]=o.copy(status="CANCELED"); release(o) }
        return order(id)
    }
    override suspend fun saved(userId: String,showtimeId: String)=saved["$userId:$showtimeId"]
    override suspend fun requests(userId: String)=saved.filterKeys {it.startsWith("$userId:")}.values.toList()
    override suspend fun remember(userId: String,showtimeId: String,request: HoldRequest?) { val key="$userId:$showtimeId"; if(request==null) saved.remove(key) else saved[key]=request }
}
