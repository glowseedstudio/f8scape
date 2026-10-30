package com.f8scape.cards

import android.content.Context
import com.f8scape.util.F8Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WeatherInfo(
    val tempC: Int,
    val condition: String,
    val weatherCode: Int,
    val cityName: String,
    val isCached: Boolean = false
)

class WeatherDataProvider(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("f8scape_weather_cache", Context.MODE_PRIVATE)

    suspend fun getWeather(cityName: String = "Melbourne"): WeatherInfo = withContext(Dispatchers.IO) {
        val cachedTemp = sharedPrefs.getInt("cached_temp", 21)
        val cachedCondition = sharedPrefs.getString("cached_condition", "Clear") ?: "Clear"
        val cachedCode = sharedPrefs.getInt("cached_code", 0)
        val cachedCity = sharedPrefs.getString("cached_city", cityName) ?: cityName

        try {
            // 1. Geocode city name to lat/lon via Open-Meteo geocoding
            val encodedCity = URLEncoder.encode(cityName, "UTF-8")
            val geoUrl = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=en&format=json")
            val geoConnection = (geoUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }

            if (geoConnection.responseCode != 200) {
                return@withContext WeatherInfo(cachedTemp, cachedCondition, cachedCode, cachedCity, isCached = true)
            }

            val geoResponse = geoConnection.inputStream.bufferedReader().use { it.readText() }
            val geoJson = JSONObject(geoResponse)
            val results = geoJson.optJSONArray("results")
            if (results == null || results.length() == 0) {
                return@withContext WeatherInfo(cachedTemp, cachedCondition, cachedCode, cachedCity, isCached = true)
            }

            val firstResult = results.getJSONObject(0)
            val lat = firstResult.getDouble("latitude")
            val lon = firstResult.getDouble("longitude")
            val resolvedName = firstResult.optString("name", cityName)

            // 2. Query forecast for current weather
            val weatherUrl = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code")
            val weatherConnection = (weatherUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }

            if (weatherConnection.responseCode != 200) {
                return@withContext WeatherInfo(cachedTemp, cachedCondition, cachedCode, resolvedName, isCached = true)
            }

            val weatherResponse = weatherConnection.inputStream.bufferedReader().use { it.readText() }
            val weatherJson = JSONObject(weatherResponse)
            val current = weatherJson.getJSONObject("current")
            val temp = current.getDouble("temperature_2m").toInt()
            val weatherCode = current.getInt("weather_code")

            val condition = mapWeatherCodeToDescription(weatherCode)

            // Cache locally
            sharedPrefs.edit()
                .putInt("cached_temp", temp)
                .putString("cached_condition", condition)
                .putInt("cached_code", weatherCode)
                .putString("cached_city", resolvedName)
                .apply()

            F8Log.i("Weather", "Updated live weather for $resolvedName: $temp°C, $condition")
            WeatherInfo(
                tempC = temp,
                condition = condition,
                weatherCode = weatherCode,
                cityName = resolvedName,
                isCached = false
            )
        } catch (e: Exception) {
            F8Log.w("Weather", "Network weather fetch failed, using cached values: ${e.message}")
            WeatherInfo(
                tempC = cachedTemp,
                condition = cachedCondition,
                weatherCode = cachedCode,
                cityName = cachedCity,
                isCached = true
            )
        }
    }

    private fun mapWeatherCodeToDescription(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Fog"
            51, 53, 55, 61, 63, 65 -> "Rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Clear"
        }
    }
}
