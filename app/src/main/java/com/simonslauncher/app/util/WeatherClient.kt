package com.simonslauncher.app.util

import com.simonslauncher.app.model.Weather
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

object WeatherClient {
    private const val CITY = "Napoli"
    private const val URL_NAPOLI =
        "https://api.open-meteo.com/v1/forecast?latitude=40.85&longitude=14.27" +
            "&current=temperature_2m,weather_code,is_day&timezone=Europe%2FRome"

    fun fetch(): Weather? = try {
        request()
    } catch (_: Exception) {
        null
    }

    private fun request(): Weather? {
        val conn = URL(URL_NAPOLI).openConnection() as HttpURLConnection
        conn.connectTimeout = 5_000
        conn.readTimeout = 5_000
        return try {
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).getJSONObject("current")
            Weather(
                temperature = current.getDouble("temperature_2m").roundToInt(),
                code = current.getInt("weather_code"),
                isDay = current.optInt("is_day", 1) == 1,
                city = CITY
            )
        } finally {
            conn.disconnect()
        }
    }
}
