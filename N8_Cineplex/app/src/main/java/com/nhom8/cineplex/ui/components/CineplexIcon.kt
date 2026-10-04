package com.nhom8.cineplex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/** Native equivalents of the prototype's outline paths; no icon library dependency. */
@Composable
fun CineplexIcon(name: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(modifier.size(24.dp)) {
        val s = size.width / 24f
        val p = Path()
        fun m(x: Float,y: Float) = p.moveTo(x*s,y*s)
        fun l(x: Float,y: Float) = p.lineTo(x*s,y*s)
        fun circle(x: Float,y: Float,r: Float) = drawCircle(color,r*s,Offset(x*s,y*s),style = Stroke(2*s))
        when(name) {
            "film" -> { m(4f,3f);l(20f,3f);l(20f,21f);l(4f,21f);p.close();m(8f,3f);l(8f,21f);m(16f,3f);l(16f,21f);m(4f,8f);l(20f,8f);m(4f,16f);l(20f,16f) }
            "home" -> { m(3f,10f);l(12f,3f);l(21f,10f);m(5f,9f);l(5f,21f);l(10f,21f);l(10f,14f);l(14f,14f);l(14f,21f);l(19f,21f);l(19f,9f) }
            "user" -> { circle(12f,7f,4f);m(4f,21f);p.cubicTo(4*s,9*s,20*s,9*s,20*s,21*s) }
            "mail" -> { m(3f,5f);l(21f,5f);l(21f,19f);l(3f,19f);p.close();m(3f,6f);l(12f,13f);l(21f,6f) }
            "lock" -> { m(5f,10f);l(19f,10f);l(19f,21f);l(5f,21f);p.close();m(8f,10f);l(8f,7f);p.cubicTo(8*s,2*s,16*s,2*s,16*s,7*s);l(16f,10f);m(12f,14f);l(12f,17f) }
            "back" -> { m(19f,12f);l(5f,12f);m(11f,6f);l(5f,12f);l(11f,18f) }
            "arrow" -> { m(5f,12f);l(19f,12f);m(13f,6f);l(19f,12f);l(13f,18f) }
            "close" -> { m(6f,6f);l(18f,18f);m(6f,18f);l(18f,6f) }
            "exit" -> { m(10f,4f);l(4f,4f);l(4f,20f);l(10f,20f);m(9f,12f);l(21f,12f);m(16f,7f);l(21f,12f);l(16f,17f) }
            "search" -> { circle(10f,10f,7f);m(15f,15f);l(21f,21f) }
            "clock" -> { circle(12f,12f,10f);m(12f,8f);l(12f,12f);l(15f,14f) }
            "eye", "eye_off" -> { m(2f,12f);p.cubicTo(8*s,3*s,16*s,3*s,22*s,12*s);p.cubicTo(16*s,21*s,8*s,21*s,2*s,12*s);circle(12f,12f,3f);if(name=="eye_off"){m(3f,3f);l(21f,21f)} }
            else -> { m(3f,7f);l(21f,7f);l(21f,19f);l(3f,19f);p.close();m(15f,7f);l(15f,19f) }
        }
        drawPath(p,color,style = Stroke(2*s,cap = StrokeCap.Round,join = StrokeJoin.Round))
    }
}
