package com.simonslauncher.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.simonslauncher.app.model.Weather
import com.simonslauncher.app.model.WeatherKind
import com.simonslauncher.app.model.kind

private val SunColor = Color(0xFFFDBA2C)
private val MoonColor = Color(0xFFE8E4D0)
private val CloudTop = Color(0xFFFFFFFF)
private val CloudBottom = Color(0xFFCFD3DA)
private val RainColor = Color(0xFF6CB4FF)

@Composable
fun WeatherIcon(weather: Weather, modifier: Modifier = Modifier) {
    val kind = weather.kind()
    val isDay = weather.isDay
    Canvas(modifier) {
        when (kind) {
            WeatherKind.CLEAR -> drawSun(Offset(size.width * 0.5f, size.height * 0.5f), size.height * 0.36f, isDay)
            WeatherKind.PARTLY_CLOUDY -> {
                drawSun(Offset(size.width * 0.66f, size.height * 0.34f), size.height * 0.27f, isDay)
                drawCloud(top = size.height * 0.3f)
            }
            WeatherKind.CLOUDY -> drawCloud(top = size.height * 0.2f)
            WeatherKind.RAIN -> {
                drawCloud(top = size.height * 0.05f, bottomFraction = 0.72f)
                val y0 = size.height * 0.8f
                for (i in 0..2) {
                    val x = size.width * (0.3f + i * 0.2f)
                    drawLine(RainColor, Offset(x, y0), Offset(x - size.width * 0.06f, size.height), size.height * 0.08f, StrokeCap.Round)
                }
            }
        }
    }
}

private fun DrawScope.drawSun(center: Offset, radius: Float, isDay: Boolean) {
    drawCircle(if (isDay) SunColor else MoonColor, radius, center)
}

private fun DrawScope.drawCloud(top: Float, bottomFraction: Float = 1f) {
    val w = size.width
    val bottom = size.height * bottomFraction
    val h = bottom - top
    val brush = Brush.verticalGradient(listOf(CloudTop, CloudBottom), startY = top, endY = bottom)
    drawCircle(brush, h * 0.36f, Offset(w * 0.3f, bottom - h * 0.36f))
    drawCircle(brush, h * 0.5f, Offset(w * 0.52f, top + h * 0.5f))
    drawCircle(brush, h * 0.32f, Offset(w * 0.76f, bottom - h * 0.32f))
    drawRoundRect(
        brush,
        topLeft = Offset(w * 0.08f, bottom - h * 0.5f),
        size = Size(w * 0.86f, h * 0.5f),
        cornerRadius = CornerRadius(h * 0.25f)
    )
}
