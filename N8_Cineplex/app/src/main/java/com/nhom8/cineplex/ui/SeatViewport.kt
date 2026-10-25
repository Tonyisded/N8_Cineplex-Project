package com.nhom8.cineplex.ui

import kotlin.math.max
import kotlin.math.min

data class SeatViewport(val scale: Float, val x: Float, val y: Float) {
    fun bounded(width: Float, height: Float, mapWidth: Float, mapHeight: Float): SeatViewport = copy(
        x=if(mapWidth*scale<=width) (width-mapWidth*scale)/2 else x.coerceIn(width-mapWidth*scale,0f),
        y=if(mapHeight*scale<=height) (height-mapHeight*scale)/2 else y.coerceIn(height-mapHeight*scale,0f))
    fun zoom(cx: Float, cy: Float, factor: Float, panX: Float, panY: Float, minimum: Float, maximum: Float,
        width: Float, height: Float, mapWidth: Float, mapHeight: Float): SeatViewport {
        val next=(scale*factor).coerceIn(minimum,max(minimum,maximum))
        return SeatViewport(next,cx-(cx-x)*next/scale+panX,cy-(cy-y)*next/scale+panY).bounded(width,height,mapWidth,mapHeight)
    }
    companion object {
        fun fit(width: Float,height: Float,mapWidth: Float,mapHeight: Float): SeatViewport {
            val scale=min(width/mapWidth,height/mapHeight).coerceAtLeast(0.01f)
            return SeatViewport(scale,0f,0f).bounded(width,height,mapWidth,mapHeight)
        }
    }
}
