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
import java.text.SimpleDateFormat
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

data class HourlyForecast(
    val timeLabel: String,
    val tempC: Int,
    val weatherCode: Int,
    val rainProbability: Int
)

data class DailyForecast(
    val dayLabel: String,
    val weatherCode: Int,
    val condition: String,
    val minTempC: Int,
    val maxTempC: Int,
    val rainProbability: Int
)

data class WeatherIntelligence(
    val cityName: String,
    val currentTempC: Int,
    val apparentTempC: Int,
    val condition: String,
    val weatherCode: Int,
    val humidityPercent: Int,
    val windSpeedKmH: Double,
    val uvIndex: Double,
    val minTempC: Int,
    val maxTempC: Int,
    val rainProbability: Int,
    val sunrise: String,
    val sunset: String,
    val hourlyForecast: List<HourlyForecast>,
    val dailyForecast: List<DailyForecast>,
    val isCached: Boolean = false
)

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
        private const val MAX_RESPONSE_CHARS = 131_072
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

    suspend fun getWeatherIntelligence(cityName: String = "Canberra"): WeatherIntelligence = withContext(Dispatchers.IO) {
        val safeCityInput = cityName.trim().take(64).ifBlank { "Canberra" }

        try {
            var targetLat: Double? = null
            var targetLon: Double? = null
            var resolvedName = safeCityInput

            // 1. Geocode
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

            val lat = targetLat ?: -35.2809
            val lon = targetLon ?: 149.1300

            // 2. Query rich weather intelligence
            val weatherUrl = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,uv_index&hourly=temperature_2m,weather_code,precipitation_probability&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset&forecast_days=7&timezone=auto")
            val weatherConnection = (weatherUrl.openConnection() as javax.net.ssl.HttpsURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
            }

            try {
                if (weatherConnection.responseCode != 200) {
                    return@withContext getFallbackIntelligence(resolvedName)
                }

                val weatherResponse = readBoundedResponse(weatherConnection)
                val rootJson = JSONObject(weatherResponse)

                val current = rootJson.getJSONObject("current")
                val curTemp = current.optDouble("temperature_2m", 20.0).roundToInt()
                val curApparent = current.optDouble("apparent_temperature", curTemp.toDouble()).roundToInt()
                val curCode = current.optInt("weather_code", 0)
                val curCondition = mapWeatherCodeToDescription(curCode)
                val curHumidity = current.optInt("relative_humidity_2m", 50)
                val curWind = current.optDouble("wind_speed_10m", 12.0)
                val curUv = current.optDouble("uv_index", 2.0)

                // Daily forecast (7 days)
                val dailyJson = rootJson.optJSONObject("daily")
                val dailyTimes = dailyJson?.optJSONArray("time")
                val dailyCodes = dailyJson?.optJSONArray("weather_code")
                val dailyMaxs = dailyJson?.optJSONArray("temperature_2m_max")
                val dailyMins = dailyJson?.optJSONArray("temperature_2m_min")
                val dailyRains = dailyJson?.optJSONArray("precipitation_probability_max")
                val dailySunrises = dailyJson?.optJSONArray("sunrise")
                val dailySunsets = dailyJson?.optJSONArray("sunset")

                val todayMin = dailyMins?.optDouble(0, curTemp.toDouble() - 5)?.roundToInt() ?: (curTemp - 5)
                val todayMax = dailyMaxs?.optDouble(0, curTemp.toDouble() + 5)?.roundToInt() ?: (curTemp + 5)
                val todayRain = dailyRains?.optInt(0, 0) ?: 0
                val sunriseStr = formatSunriseSunset(dailySunrises?.optString(0, "") ?: "")
                val sunsetStr = formatSunriseSunset(dailySunsets?.optString(0, "") ?: "")

                val dailyList = mutableListOf<DailyForecast>()
                if (dailyTimes != null) {
                    for (i in 0 until dailyTimes.length().coerceAtMost(7)) {
                        val dateStr = dailyTimes.optString(i, "")
                        val code = dailyCodes?.optInt(i, 0) ?: 0
                        val minT = dailyMins?.optDouble(i, 10.0)?.roundToInt() ?: 10
                        val maxT = dailyMaxs?.optDouble(i, 20.0)?.roundToInt() ?: 20
                        val rainP = dailyRains?.optInt(i, 0) ?: 0
                        dailyList.add(
                            DailyForecast(
                                dayLabel = formatDayLabel(dateStr, i),
                                weatherCode = code,
                                condition = mapWeatherCodeToDescription(code),
                                minTempC = minT,
                                maxTempC = maxT,
                                rainProbability = rainP
                            )
                        )
                    }
                }

                // Hourly forecast (next 12-14 hours)
                val hourlyJson = rootJson.optJSONObject("hourly")
                val hourlyTimes = hourlyJson?.optJSONArray("time")
                val hourlyTemps = hourlyJson?.optJSONArray("temperature_2m")
                val hourlyCodes = hourlyJson?.optJSONArray("weather_code")
                val hourlyRains = hourlyJson?.optJSONArray("precipitation_probability")

                val hourlyList = mutableListOf<HourlyForecast>()
                if (hourlyTimes != null && hourlyTemps != null) {
                    val currentTimeStr = current.optString("time", "")
                    var startIndex = 0
                    for (i in 0 until hourlyTimes.length()) {
                        if (hourlyTimes.optString(i, "") >= currentTimeStr) {
                            startIndex = i
                            break
                        }
                    }
                    val count = (hourlyTimes.length() - startIndex).coerceAtMost(14)
                    for (k in 0 until count) {
                        val idx = startIndex + k
                        val hTime = hourlyTimes.optString(idx, "")
                        val hTemp = hourlyTemps.optDouble(idx, curTemp.toDouble()).roundToInt()
                        val hCode = hourlyCodes?.optInt(idx, curCode) ?: curCode
                        val hRain = hourlyRains?.optInt(idx, 0) ?: 0
                        hourlyList.add(
                            HourlyForecast(
                                timeLabel = formatHourLabel(hTime, k == 0),
                                tempC = hTemp,
                                weatherCode = hCode,
                                rainProbability = hRain
                            )
                        )
                    }
                }

                // Cache basic values for dashboard
                sharedPrefs.edit()
                    .putInt("cached_temp", curTemp)
                    .putString("cached_condition", curCondition)
                    .putInt("cached_code", curCode)
                    .putString("cached_city", resolvedName)
                    .apply()

                F8Log.i("WeatherIntelligence", "Fetched live weather intelligence for $resolvedName")
                WeatherIntelligence(
                    cityName = resolvedName,
                    currentTempC = curTemp,
                    apparentTempC = curApparent,
                    condition = curCondition,
                    weatherCode = curCode,
                    humidityPercent = curHumidity,
                    windSpeedKmH = curWind,
                    uvIndex = curUv,
                    minTempC = todayMin,
                    maxTempC = todayMax,
                    rainProbability = todayRain,
                    sunrise = sunriseStr,
                    sunset = sunsetStr,
                    hourlyForecast = hourlyList,
                    dailyForecast = dailyList,
                    isCached = false
                )
            } finally {
                weatherConnection.disconnect()
            }
        } catch (e: Exception) {
            F8Log.w("WeatherIntelligence", "Failed to fetch weather intelligence: ${e.message}")
            getFallbackIntelligence(safeCityInput)
        }
    }

    private fun getFallbackIntelligence(cityName: String): WeatherIntelligence {
        val cachedTemp = sharedPrefs.getInt("cached_temp", 21)
        val cachedCondition = sharedPrefs.getString("cached_condition", "Clear") ?: "Clear"
        val cachedCode = sharedPrefs.getInt("cached_code", 0)
        val cachedCity = sharedPrefs.getString("cached_city", cityName) ?: cityName

        return WeatherIntelligence(
            cityName = cachedCity,
            currentTempC = cachedTemp,
            apparentTempC = cachedTemp - 1,
            condition = cachedCondition,
            weatherCode = cachedCode,
            humidityPercent = 55,
            windSpeedKmH = 14.0,
            uvIndex = 3.5,
            minTempC = cachedTemp - 6,
            maxTempC = cachedTemp + 3,
            rainProbability = 10,
            sunrise = "06:15 AM",
            sunset = "07:45 PM",
            hourlyForecast = listOf(
                HourlyForecast("Now", cachedTemp, cachedCode, 0),
                HourlyForecast("+2h", cachedTemp + 1, cachedCode, 0),
                HourlyForecast("+4h", cachedTemp + 2, cachedCode, 10),
                HourlyForecast("+6h", cachedTemp - 1, cachedCode, 20),
                HourlyForecast("+8h", cachedTemp - 3, cachedCode, 10)
            ),
            dailyForecast = listOf(
                DailyForecast("Today", cachedCode, cachedCondition, cachedTemp - 6, cachedTemp + 3, 10),
                DailyForecast("Tomorrow", 1, "Partly Cloudy", cachedTemp - 5, cachedTemp + 2, 20),
                DailyForecast("Day 3", 2, "Partly Cloudy", cachedTemp - 4, cachedTemp + 4, 10),
                DailyForecast("Day 4", 0, "Clear", cachedTemp - 5, cachedTemp + 5, 0),
                DailyForecast("Day 5", 61, "Rain", cachedTemp - 3, cachedTemp + 1, 60)
            ),
            isCached = true
        )
    }

    private fun formatSunriseSunset(isoString: String): String {
        if (isoString.isBlank()) return ""
        return try {
            val timePart = isoString.substringAfter("T")
            val parts = timePart.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            val period = if (hour >= 12) "PM" else "AM"
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, period)
        } catch (_: Exception) {
            isoString.substringAfter("T").take(5)
        }
    }

    private fun formatDayLabel(isoDate: String, index: Int): String {
        if (index == 0) return "Today"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(isoDate) ?: return isoDate
            SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        } catch (_: Exception) {
            isoDate
        }
    }

    private fun formatHourLabel(isoDateTime: String, isFirst: Boolean): String {
        if (isFirst) return "Now"
        return try {
            val timePart = isoDateTime.substringAfter("T")
            val hour = timePart.split(":")[0].toInt()
            val period = if (hour >= 12) "PM" else "AM"
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            "$h12 $period"
        } catch (_: Exception) {
            isoDateTime.substringAfter("T").take(5)
        }
    }
}
