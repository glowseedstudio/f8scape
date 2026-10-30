package com.f8scape.cards

import android.content.Context
import android.os.Build
import androidx.core.text.util.LocalePreferences
import com.f8scape.util.F8Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

enum class TemperatureUnit(val id: String, val displayName: String, val shortLabel: String) {
    SYSTEM("SYSTEM", "Phone Default", "System"),
    CELSIUS("CELSIUS", "Celsius (°C)", "°C"),
    FAHRENHEIT("FAHRENHEIT", "Fahrenheit (°F)", "°F");

    fun useFahrenheit(): Boolean {
        return when (this) {
            CELSIUS -> false
            FAHRENHEIT -> true
            SYSTEM -> isSystemFahrenheit()
        }
    }

    companion object {
        fun fromId(id: String): TemperatureUnit = entries.find { it.id == id } ?: SYSTEM

        fun isSystemFahrenheit(): Boolean {
            return try {
                val unit = LocalePreferences.getTemperatureUnit()
                if (unit == LocalePreferences.TemperatureUnit.FAHRENHEIT) {
                    true
                } else if (unit == LocalePreferences.TemperatureUnit.CELSIUS) {
                    false
                } else {
                    val country = Locale.getDefault().country.uppercase(Locale.ROOT)
                    country in setOf("US", "BS", "KY", "LR", "PW", "FM", "MH")
                }
            } catch (_: Throwable) {
                val country = Locale.getDefault().country.uppercase(Locale.ROOT)
                country in setOf("US", "BS", "KY", "LR", "PW", "FM", "MH")
            }
        }

        fun formatTemp(tempC: Int, unitPreferenceId: String = "SYSTEM"): String {
            val useF = fromId(unitPreferenceId).useFahrenheit()
            return if (useF) {
                val tempF = (tempC * 9.0 / 5.0 + 32.0).roundToInt()
                "${tempF}°F"
            } else {
                "${tempC}°C"
            }
        }
    }
}

data class WeatherInfo(
    val tempC: Int,
    val condition: String,
    val weatherCode: Int,
    val cityName: String,
    val isCached: Boolean = false
) {
    fun formattedTemp(unitPreferenceId: String = "SYSTEM"): String {
        return TemperatureUnit.formatTemp(tempC, unitPreferenceId)
    }
}

class WeatherDataProvider(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("f8scape_weather_cache", Context.MODE_PRIVATE)

    companion object {
        private const val MAX_RESPONSE_CHARS = 16_384
    }

    private fun readBoundedResponse(connection: javax.net.ssl.HttpsURLConnection): String {
        return connection.inputStream.bufferedReader().use { reader ->
            val buffer = CharArray(4096)
            val builder = StringBuilder()
            var read: Int
            while (reader.read(buffer).also { read = it } != -1) {
                builder.append(buffer, 0, read)
                if (builder.length >= MAX_RESPONSE_CHARS) break
            }
            builder.toString()
        }
    }

    suspend fun getWeather(cityName: String = "Canberra"): WeatherInfo = withContext(Dispatchers.IO) {
        val safeCityInput = cityName.trim().take(64).ifBlank { "Canberra" }
        val cachedTemp = sharedPrefs.getInt("cached_temp", 21)
        val cachedCondition = sharedPrefs.getString("cached_condition", "Clear") ?: "Clear"
        val cachedCode = sharedPrefs.getInt("cached_code", 0)
        val cachedCity = sharedPrefs.getString("cached_city", safeCityInput) ?: safeCityInput

        try {
            var targetLat: Double? = null
            var targetLon: Double? = null
            var resolvedName = safeCityInput

            // Geocode city name to lat/lon via Open-Meteo HTTPS geocoding
            if (safeCityInput.isNotBlank()) {
                val encodedCity = URLEncoder.encode(safeCityInput, "UTF-8")
                val geoUrl = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=en&format=json")
                val geoConnection = (geoUrl.openConnection() as javax.net.ssl.HttpsURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    requestMethod = "GET"
                }

                try {
                    if (geoConnection.responseCode == 200) {
                        val geoResponse = readBoundedResponse(geoConnection)
                        val geoJson = JSONObject(geoResponse)
                        val results = geoJson.optJSONArray("results")
                        if (results != null && results.length() > 0) {
                            val firstResult = results.getJSONObject(0)
                            targetLat = firstResult.getDouble("latitude").coerceIn(-90.0, 90.0)
                            targetLon = firstResult.getDouble("longitude").coerceIn(-180.0, 180.0)
                            resolvedName = firstResult.optString("name", safeCityInput).take(64)
                        }
                    }
                } finally {
                    geoConnection.disconnect()
                }
            }

            val lat = targetLat ?: -35.2809 // Canberra default coordinates
            val lon = targetLon ?: 149.1300 // Canberra default coordinates

            // 2. Query forecast for current weather over HTTPS
            val weatherUrl = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code")
            val weatherConnection = (weatherUrl.openConnection() as javax.net.ssl.HttpsURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }

            try {
                if (weatherConnection.responseCode != 200) {
                    return@withContext WeatherInfo(cachedTemp, cachedCondition, cachedCode, resolvedName, isCached = true)
                }

                val weatherResponse = readBoundedResponse(weatherConnection)
                val weatherJson = JSONObject(weatherResponse)
                val current = weatherJson.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toInt().coerceIn(-90, 70)
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
            } finally {
                weatherConnection.disconnect()
            }
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
