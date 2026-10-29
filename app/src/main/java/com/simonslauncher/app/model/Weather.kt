package com.simonslauncher.app.model

data class Weather(
    val temperature: Int,
    val code: Int,
    val isDay: Boolean,
    val city: String
)

enum class WeatherKind { CLEAR, PARTLY_CLOUDY, CLOUDY, RAIN }

/** WMO weather codes as returned by Open-Meteo. */
fun Weather.kind(): WeatherKind = when (code) {
    0, 1 -> WeatherKind.CLEAR
    2 -> WeatherKind.PARTLY_CLOUDY
    in 51..67, in 80..82, in 95..99 -> WeatherKind.RAIN
    else -> WeatherKind.CLOUDY
}
