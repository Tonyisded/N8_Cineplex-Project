package com.nhom8.cineplex.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.nhom8.cineplex.data.*
import com.nhom8.cineplex.ui.theme.CineplexColors as C

@Composable
fun ShowtimeGroups(slots: List<ShowtimeDto>, open: (ShowtimeDto) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier,verticalArrangement = Arrangement.spacedBy(16.dp)) {
        slots.groupBy { it.format to it.presentationLabel.orEmpty() }.values.sortedBy { it.minOf { s -> s.startAt } }.forEach { group ->
            val first = group.first()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text((if(first.format.endsWith("3D")) "3D" else "2D") + first.presentationLabel?.let { " $it" }.orEmpty(),style = MaterialTheme.typography.titleSmall)
                    FormatBadge(first.format)
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val gap = 8.dp
                    val minCell = 96.dp * LocalDensity.current.fontScale
                    val columns = ((maxWidth + gap) / (minCell + gap)).toInt().coerceIn(1,4)
                    val cell = (maxWidth - gap * (columns - 1)) / columns
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(gap),verticalArrangement = Arrangement.spacedBy(gap)) {
                        group.sortedWith(compareBy({ it.startAt }, { it.id })).forEach { slot ->
                            Column(Modifier.width(cell).heightIn(min = 92.dp).clip(RoundedCornerShape(10.dp)).background(C.Surface).border(1.dp,C.Line,RoundedCornerShape(10.dp)).clickable(role = Role.Button,onClick = { open(slot) }).testTag("showtime-${slot.id}").semantics { contentDescription = "Suất ${showtimeRange(slot)}, ${seatSummary(slot)}" }) {
                                val range = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold,fontSize = 17.sp)) { append(vietnamTime(slot.startAt)) }
                                    withStyle(SpanStyle(fontSize = 12.sp,color = C.Muted)) { append("–" + vietnamTime(slot.endAt)); if(showtimeRange(slot).contains("+1")) append(" (+1 ngày)") }
                                }
                                Text(range,Modifier.fillMaxWidth().padding(8.dp),lineHeight = 24.sp)
                                Column(Modifier.fillMaxWidth().background(C.Raised.copy(alpha = .5f)).padding(8.dp)) {
                                    Text(seatSummary(slot),style = MaterialTheme.typography.labelSmall,color = C.Muted)
                                    if(slot.totalSeats > 0 && slot.availableSeats == 0) Text("Hết ghế",style = MaterialTheme.typography.labelSmall,color = C.Error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatBadge(format: String) {
    val label = when(format) { "IMAX_2D", "IMAX_3D" -> "IMAX"; "SCREENX" -> "SCREENX"; "GOLD_CLASS" -> "GOLD CLASS"; else -> return }
    val background = when(label) { "IMAX" -> Color(0xFF087AB9); "SCREENX" -> Color.Black; else -> C.Primary }
    Surface(color = background,shape = RoundedCornerShape(50),modifier = Modifier.testTag("format-$label")) {
        Text(if(label == "GOLD CLASS") label else "$label ›",Modifier.padding(horizontal = 10.dp,vertical = 3.dp),color = Color.White,fontWeight = FontWeight.ExtraBold,fontStyle = if(label == "IMAX") FontStyle.Italic else FontStyle.Normal,fontSize = 12.sp)
    }
}
