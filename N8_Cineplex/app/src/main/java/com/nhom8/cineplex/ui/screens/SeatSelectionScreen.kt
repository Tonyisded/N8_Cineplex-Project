package com.nhom8.cineplex.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.model.Movie
import com.nhom8.cineplex.ui.CineplexViewModel
import com.nhom8.cineplex.ui.SeatSelectionViewModel
import com.nhom8.cineplex.ui.SeatViewport
import com.nhom8.cineplex.ui.theme.CineplexColors as C
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

private val MapBlack=Color(0xFF111111)
private val SelectedRed=Color(0xFFB02635)
private val Normal=Color(0xFFAA9F92)
private val Vip=Color(0xFF8D4356)
private val Couple=Color(0xFFD50E68)
private val Gold=Color(0xFF775522)
private fun vnd(value: Long)=NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value)+" đ"
private fun timeText(seconds: Int)="%02d:%02d".format(Locale.ROOT,seconds/60,seconds%60)
private fun kindColor(kind: String)=when(kind) { "VIP"->Vip; "COUPLE"->Couple; "GOLD"->Gold; else->Normal }
private fun seatColor(seat: SeatDto,selected: Boolean)=when { selected->SelectedRed; seat.status=="SOLD"->Color(0xFFD6D4C9); seat.status!="AVAILABLE"->Color(0xFF454545); else->kindColor(seat.seatType) }
private fun statusLabel(seat: SeatDto,selected: Boolean)=when { selected->"Đang chọn"; seat.status=="SOLD"->"Đã đặt"; seat.status=="HELD"->"Người khác giữ"; seat.status=="BLOCKED"->"Không khả dụng"; else->"Còn trống" }

@Composable
fun SeatSelectionScreen(vm: CineplexViewModel,state: SeatSelectionViewModel,movie: Movie,onBack: ()->Unit) {
    val slot=vm.selectedShowtime ?: return
    Scaffold(containerColor=MapBlack, topBar={ SeatHeader("Chọn ghế",onBack,true) }, bottomBar={
        Surface(color=Color.White,shadowElevation=12.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(if(state.chosen.isEmpty()) "Chưa chọn ghế" else state.chosen.joinToString(" · ") { it.seatCode },
                    maxLines=2,overflow=TextOverflow.Ellipsis,color=C.Text,fontSize=14.sp)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${state.places} chỗ",color=C.Muted,fontSize=12.sp)
                        Text(vnd(state.total),color=SelectedRed,fontSize=22.sp,fontWeight=FontWeight.Bold,modifier=Modifier.testTag("seat-total"))
                    }
                    Button(onClick={ if(state.held) vm.seatHandoff() else state.continueToSummary(vm::seatHandoff) },
                        enabled=if(state.held) state.secondsLeft>0 else state.canContinue,
                        colors=ButtonDefaults.buttonColors(containerColor=SelectedRed),shape=RoundedCornerShape(28.dp),
                        modifier=Modifier.heightIn(min=52.dp).testTag("seat-continue")) {
                        Text(if(state.confirming) "Đang giữ ghế…" else if(state.held) "XEM GIỮ CHỖ" else "ĐẶT VÉ",fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).testTag("seat-selection-screen")) {
            BookingContext(movie,slot,true)
            if(state.deadlineMillis!=null) HoldTime(state)
            if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                !state.online && !state.loading -> Row(Modifier.fillMaxWidth().padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("Không thể đồng bộ ghế",Modifier.weight(1f),color=Color(0xFFFFBCBC),fontSize=12.sp)
                    TextButton(state::refresh,enabled=!state.syncing) { Text("Thử lại",color=Color.White) }
                }
                !state.realtime && state.online -> Text("Đang cập nhật ghế qua máy chủ",Modifier.padding(horizontal=14.dp),color=Color(0xFFBDBDBD),fontSize=11.sp)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val layout=state.layout
                if(layout!=null && state.seats.isNotEmpty()) SeatMap(state,layout)
                else Text(if(state.loading) "Đang tải sơ đồ ghế…" else "Chưa có dữ liệu ghế cho suất này",Modifier.align(Alignment.Center).padding(20.dp),color=Color.White)
            }
            SeatLegend(state.seats.map { it.seatType }.distinct())
            if(state.message.isNotBlank()) Text(state.message,Modifier.padding(horizontal=16.dp,vertical=6.dp).testTag("seat-message"),color=Color(0xFFFFD1D1),fontSize=12.sp)
            if(state.held) TextButton({state.cancelHold()},Modifier.align(Alignment.CenterHorizontally),enabled=!state.confirming) { Text("Hủy giữ chỗ để đổi ghế",color=Color.White) }
        }
    }
    SeatDialogs(vm,state)
}

@Composable
private fun SeatMap(state: SeatSelectionViewModel,layout: SeatLayout) {
    val density=LocalDensity.current
    var width by remember { mutableFloatStateOf(0f) }; var height by remember { mutableFloatStateOf(0f) }
    var scale by rememberSaveable(state.showtimeId,layout.key) { mutableFloatStateOf(0f) }
    var x by rememberSaveable(state.showtimeId,layout.key) { mutableFloatStateOf(0f) }
    var y by rememberSaveable(state.showtimeId,layout.key) { mutableFloatStateOf(0f) }
    val fitted=if(width>0 && height>0) SeatViewport.fit(width,height,layout.width,layout.height) else SeatViewport(1f,0f,0f)
    val minimum=fitted.scale
    val maximum=with(density) { 64.dp.toPx() }/.9f
    fun apply(v: SeatViewport) { scale=v.scale; x=v.x; y=v.y }
    fun current()=if(scale>0) SeatViewport(scale,x,y) else fitted
    fun zoom(cx: Float,cy: Float,factor: Float,panX: Float=0f,panY: Float=0f) {
        apply(current().zoom(cx,cy,factor,panX,panY,minimum,maximum,width,height,layout.width,layout.height))
    }
    val byId=remember(state.seats) { state.seats.associateBy { it.id } }
    val paint=remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign=Paint.Align.CENTER; typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL) } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("MÀN HÌNH",Modifier.weight(1f),color=Color.LightGray,fontSize=11.sp)
            TextButton({ apply(fitted) },Modifier.testTag("seat-fit")) { Text("Vừa màn hình",color=Color.White,fontSize=12.sp) }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(MapBlack).testTag("seat-map")
            .onSizeChanged { size ->
                val old=current(); val oldW=width; val oldH=height
                width=size.width.toFloat(); height=size.height.toFloat()
                val fit=SeatViewport.fit(width,height,layout.width,layout.height)
                if(scale==0f || oldW==0f || oldH==0f) apply(fit)
                else apply(SeatViewport(old.scale.coerceAtLeast(fit.scale),width/2-(oldW/2-old.x)/old.scale*old.scale,height/2-(oldH/2-old.y)/old.scale*old.scale).bounded(width,height,layout.width,layout.height))
            }
            .pointerInput(layout.key,width,height) {
                fun tap(point: Offset) {
                    val view=current(); val wx=(point.x-view.x)/view.scale; val wy=(point.y-view.y)/view.scale
                    val hit=layout.seats.firstOrNull { wx>=it.x && wx<=it.x+.9f && wy>=it.y && wy<=it.y+.9f }
                    if(hit!=null) state.select(hit.seatId)
                }
                awaitEachGesture {
                    val down=awaitFirstDown(requireUnconsumed=true)
                    var moved=false; var multi=false; var accumulated=Offset.Zero
                    var released=down
                    do {
                        val event=awaitPointerEvent()
                        released=event.changes.first()
                        val count=event.changes.count { it.pressed }
                        if(count>1) multi=true
                        val pan=event.calculatePan()
                        accumulated+=pan
                        if(multi || accumulated.getDistance()>viewConfiguration.touchSlop || moved) {
                            val center=event.calculateCentroid(useCurrent=false)
                            if(center.x.isFinite() && center.y.isFinite()) {
                                val delta=if(!moved && !multi) accumulated else pan
                                zoom(center.x,center.y,event.calculateZoom(),delta.x,delta.y)
                            }
                            moved=true
                            event.changes.forEach { it.consume() }
                        }
                    } while(event.changes.any { it.pressed })
                    if(!moved && !multi) tap(released.position)
                }
            }) {
            val view=current()
            Canvas(Modifier.fillMaxSize()) {
                fun point(wx: Float,wy: Float)=Offset(view.x+wx*view.scale,view.y+wy*view.scale)
                val screenY=1.25f
                drawLine(Color(0xFFBBBBBB),point(layout.width*.12f,screenY),point(layout.width*.88f,screenY),3f)
                paint.color=android.graphics.Color.LTGRAY; paint.textSize=view.scale*.45f
                drawContext.canvas.nativeCanvas.drawText("MÀN HÌNH",point(layout.width/2,.9f).x,point(layout.width/2,.9f).y,paint)
                if("LEFT" in layout.screens) {
                    drawLine(Color(0xFF586A76),point(.35f,2.2f),point(.35f,layout.height-1),view.scale*.12f)
                    drawLine(Color(0xFF586A76),point(layout.width-.35f,2.2f),point(layout.width-.35f,layout.height-1),view.scale*.12f)
                }
                layout.seats.forEach { pos ->
                    val seat=byId[pos.seatId] ?: return@forEach
                    val top=point(pos.x,pos.y); val cell=.9f*view.scale
                    if(top.x+cell<0 || top.y+cell<0 || top.x>size.width || top.y>size.height) return@forEach
                    val selected=seat.id in state.selected
                    drawRect(seatColor(seat,selected),top,Size(cell,cell))
                    if(seat.seatType=="COUPLE") drawRect(Couple,top,Size(cell,cell),style=Stroke(view.scale*.06f))
                    if(seat.status=="SOLD") {
                        drawLine(Color.White,top,top+Offset(cell,cell),view.scale*.04f)
                        drawLine(Color.White,top+Offset(0f,cell),top+Offset(cell,0f),view.scale*.04f)
                    } else {
                        paint.color=android.graphics.Color.WHITE; paint.textSize=cell*.33f
                        val label=if(seat.status=="BLOCKED") "—" else if(seat.status=="HELD" && !selected) "•" else seat.seatCode
                        drawContext.canvas.nativeCanvas.drawText(label,top.x+cell/2,top.y+cell/2-(paint.ascent()+paint.descent())/2,paint)
                        if(selected) drawCircle(Color.White,cell*.055f,top+Offset(cell*.85f,cell*.15f))
                    }
                }
            }
            layout.seats.forEach { pos ->
                val seat=byId[pos.seatId] ?: return@forEach
                val px=view.x+pos.x*view.scale; val py=view.y+pos.y*view.scale; val cell=.9f*view.scale
                if(px+cell>=0 && py+cell>=0 && px<width && py<height) Box(Modifier
                    .offset { IntOffset(px.roundToInt(),py.roundToInt()) }
                    .size(with(density) { cell.toDp() }).testTag("seat-${seat.seatCode}")
                    .semantics {
                        role=Role.Button; selected=seat.id in state.selected
                        contentDescription="${seat.kindLabel} ${seat.seatCode}, ${vnd(seat.price.toLong())}, ${statusLabel(seat,seat.id in state.selected)}"
                        if(seat.status!="AVAILABLE" && seat.id !in state.selected) disabled()
                        onClick("Chọn ghế ${seat.seatCode}") { state.select(seat.id); true }
                    })
            }
            if(view.scale>minimum*1.05f) Canvas(Modifier.align(Alignment.TopStart).padding(8.dp).size(108.dp,80.dp)
                .background(Color(0xEE111111)).border(1.dp,Color.Gray).testTag("seat-minimap")
                .pointerInput(Unit) { detectTapGestures {} }) {
                val ratio=min(size.width/layout.width,size.height/layout.height)
                val ox=(size.width-layout.width*ratio)/2; val oy=(size.height-layout.height*ratio)/2
                layout.seats.forEach { pos -> byId[pos.seatId]?.let { s -> drawRect(seatColor(s,s.id in state.selected),Offset(ox+pos.x*ratio,oy+pos.y*ratio),Size(.8f*ratio,.8f*ratio)) } }
                val left=(-view.x/view.scale).coerceIn(0f,layout.width); val top=(-view.y/view.scale).coerceIn(0f,layout.height)
                val right=((width-view.x)/view.scale).coerceIn(0f,layout.width); val bottom=((height-view.y)/view.scale).coerceIn(0f,layout.height)
                drawRect(SelectedRed,Offset(ox+left*ratio,oy+top*ratio),Size((right-left)*ratio,(bottom-top)*ratio),style=Stroke(2f))
            }
        }
    }
}

@Composable
private fun SeatLegend(kinds: List<String>) {
    FlowRow(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        val legend=listOf(SelectedRed to "Đang chọn",Color(0xFFD6D4C9) to "Đã đặt ×",Color(0xFF454545) to "Đang giữ / Khóa")+
            kinds.map { kindColor(it) to when(it) { "VIP"->"VIP"; "COUPLE"->"Couple · 2 chỗ"; "GOLD"->"Gold Class"; else->"Thường" } }
        legend.forEach { (color,label) -> Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(14.dp).background(color)); Text(label,color=Color.White,fontSize=11.sp)
        } }
    }
}
@Composable
private fun SeatHeader(title: String,back: ()->Unit,dark: Boolean=false) {
    Row(Modifier.fillMaxWidth().background(if(dark) MapBlack else Color.White).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
        TextButton(back,modifier=Modifier.semantics { contentDescription="Quay lại" }) { Text("‹",fontSize=30.sp,color=if(dark) Color.White else C.Primary) }
        Text(title,fontWeight=FontWeight.Bold,color=if(dark) Color.White else C.Text,fontSize=18.sp)
    }
}
@Composable
private fun BookingContext(movie: Movie,slot: ShowtimeDto,dark: Boolean=false) {
    val ink=if(dark) Color.White else C.Text
    Column(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text("Thông tin suất chiếu",color=if(dark) Color.LightGray else C.Muted,fontSize=11.sp)
        Text(movie.name,color=ink,fontWeight=FontWeight.Bold,fontSize=16.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        Text("N8 Cineplex · ${slot.auditorium.name}",color=ink,fontSize=12.sp)
        Text("${displayCalendarDate(vietnamDate(java.util.Date(isoMillis(slot.startAt))))} · ${vietnamTime(slot.startAt)} · ${slot.format.replace('_',' ')} · ${movie.age}",color=ink,fontSize=12.sp)
    }
}
@Composable
private fun HoldTime(state: SeatSelectionViewModel) {
    if(state.deadlineMillis==null) return
    Text("${if(state.secondsLeft<120) "Sắp hết hạn giữ ghế" else "Thời gian giữ ghế còn lại"} · ${timeText(state.secondsLeft)}",
        Modifier.fillMaxWidth().background(if(state.secondsLeft<120) SelectedRed else Color(0xFF343434)).padding(horizontal=18.dp,vertical=10.dp).testTag("seat-countdown"),
        color=Color.White,fontWeight=FontWeight.Bold,fontSize=13.sp)
}
@Composable
private fun SeatDialogs(vm: CineplexViewModel,state: SeatSelectionViewModel) {
    if(state.leaveDialog) AlertDialog(onDismissRequest=state::stay,title={Text("Rời màn hình chọn ghế?")},
        text={Column { Text(if(state.held) "Hủy giữ chỗ và trở về?" else "Bỏ lựa chọn hiện tại và trở về?"); if(state.message.isNotBlank()) Text(state.message) }},
        confirmButton={TextButton({state.cancelHold(vm::back)},enabled=!state.confirming) {Text(if(state.held) "Hủy giữ chỗ và rời" else "Rời màn hình")} },
        dismissButton={TextButton(state::stay) {Text("Ở lại")} })
    if(state.expiredDialog) AlertDialog(onDismissRequest={},title={Text("Đã hết thời gian giữ ghế")},
        text={Text("Vui lòng kiểm tra lại sơ đồ và chọn ghế để tiếp tục.")},
        confirmButton={TextButton({state.dismissExpired(); state.refresh(); if(vm.screen==com.nhom8.cineplex.model.Screen.SEAT_HANDOFF) vm.back()}) {Text("Chọn lại ghế")} },
        dismissButton={TextButton({state.dismissExpired(); if(vm.screen==com.nhom8.cineplex.model.Screen.SEAT_HANDOFF) vm.back(); vm.back()}) {Text("Về lịch chiếu")} })
}
@Composable
fun SeatHandoffScreen(vm: CineplexViewModel,state: SeatSelectionViewModel,movie: Movie) {
    val slot=vm.selectedShowtime ?: return
    Scaffold(topBar={SeatHeader("Thông tin giữ chỗ",vm::back)},bottomBar={
        Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(vm::back,Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Quay lại sơ đồ")}
            TextButton({state.cancelHold(vm::back)},Modifier.fillMaxWidth(),enabled=!state.confirming && state.held) {Text("Hủy giữ chỗ")}
        }
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())) {
            BookingContext(movie,slot); HoldTime(state)
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(if(state.loading) "Đang tải giữ chỗ…" else if(state.held) "Đang giữ ghế" else "Giữ chỗ đã kết thúc",fontSize=24.sp,fontWeight=FontWeight.Bold)
                state.order?.seats?.forEach { seat -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(seat.seatCode); Text(vnd(seat.price.toLong()))} }
                HorizontalDivider(); Text("${state.places} chỗ · ${vnd(state.total)}",fontWeight=FontWeight.Bold,color=SelectedRed,fontSize=20.sp)
                Text("Ghế sẽ tự được giải phóng khi hết thời hạn giữ chỗ.",color=C.Muted)
                if(state.message.isNotBlank()) Text(state.message,color=C.Error)
            }
        }
    }
    SeatDialogs(vm,state)
}
