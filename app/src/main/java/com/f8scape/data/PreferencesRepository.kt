package com.f8scape.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.f8scape.automation.TriggerPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "f8scape_settings")

data class UserPreferences(
    val isAutoStandbyEnabled: Boolean = true,
    val onlyWhileCharging: Boolean = false,
    val minBatteryPercent: Int = 10,
    val dwellTimeMs: Long = 1000L,
    val cooldownDurationMs: Long = 2000L,
    val activePreset: String = "Dashboard", // Minimal, Dashboard, Media
    val screenDimLevel: Float = 0.2f, // 0.0 (darkest) to 1.0 (system)
    val keepScreenOn: Boolean = true,
    val customLayoutSerialized: String = "",
    val weatherCity: String = "Canberra",
    val calendarMaskLocked: Boolean = false,
    val kenBurnsEnabled: Boolean = true,
    val showWallpaperBackground: Boolean = true,
    val contentBrightness: Float = 1.0f,
    val showWeather: Boolean = true,
    val showBattery: Boolean = true,
    val clockStyle: String = "DEFAULT",
    val clockColorId: String = "PURE_WHITE",
    val clockFullColor: Boolean = false,
    val calendarStyle: String = "DEFAULT",
    val calendarColorId: String = "SIGNATURE_BLUE",
    val calendarFullColor: Boolean = false,
    val powerProfile: String = "AMBIENT", // AMBIENT or ECO
    val autoEcoOnBatterySaver: Boolean = true,
    val autoOffMinutes: Int = 0, // 0 = Never, 15, 30, 60
    val suppressOnCalls: Boolean = true
)

class PreferencesRepository(
    private val context: Context
) {
    private object PreferencesKeys {
        val AUTO_STANDBY_ENABLED = booleanPreferencesKey("auto_standby_enabled")
        val ONLY_WHILE_CHARGING = booleanPreferencesKey("only_while_charging")
        val MIN_BATTERY_PERCENT = intPreferencesKey("min_battery_percent")
        val DWELL_TIME_MS = longPreferencesKey("dwell_time_ms")
        val COOLDOWN_DURATION_MS = longPreferencesKey("cooldown_duration_ms")
        val ACTIVE_PRESET = stringPreferencesKey("active_preset")
        val SCREEN_DIM_LEVEL = floatPreferencesKey("screen_dim_level")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val CUSTOM_LAYOUT = stringPreferencesKey("custom_layout")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val CALENDAR_MASK_LOCKED = booleanPreferencesKey("calendar_mask_locked")
        val KEN_BURNS_ENABLED = booleanPreferencesKey("ken_burns_enabled")
        val SHOW_WALLPAPER_BACKGROUND = booleanPreferencesKey("show_wallpaper_background")
        val CONTENT_BRIGHTNESS = floatPreferencesKey("content_brightness")
        val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val SHOW_BATTERY = booleanPreferencesKey("show_battery")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val CLOCK_COLOR_ID = stringPreferencesKey("clock_color_id")
        val CLOCK_FULL_COLOR = booleanPreferencesKey("clock_full_color")
        val CALENDAR_STYLE = stringPreferencesKey("calendar_style")
        val CALENDAR_COLOR_ID = stringPreferencesKey("calendar_color_id")
        val CALENDAR_FULL_COLOR = booleanPreferencesKey("calendar_full_color")
        val POWER_PROFILE = stringPreferencesKey("power_profile")
        val AUTO_ECO_ON_BATTERY_SAVER = booleanPreferencesKey("auto_eco_on_battery_saver")
        val AUTO_OFF_MINUTES = intPreferencesKey("auto_off_minutes")
        val SUPPRESS_ON_CALLS = booleanPreferencesKey("suppress_on_calls")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                isAutoStandbyEnabled = preferences[PreferencesKeys.AUTO_STANDBY_ENABLED] ?: true,
                onlyWhileCharging = preferences[PreferencesKeys.ONLY_WHILE_CHARGING] ?: false,
                minBatteryPercent = preferences[PreferencesKeys.MIN_BATTERY_PERCENT] ?: 10,
                dwellTimeMs = preferences[PreferencesKeys.DWELL_TIME_MS] ?: 1000L,
                cooldownDurationMs = preferences[PreferencesKeys.COOLDOWN_DURATION_MS] ?: 2000L,
                activePreset = preferences[PreferencesKeys.ACTIVE_PRESET] ?: "Dashboard",
                screenDimLevel = preferences[PreferencesKeys.SCREEN_DIM_LEVEL] ?: 0.2f,
                keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true,
                customLayoutSerialized = preferences[PreferencesKeys.CUSTOM_LAYOUT] ?: "",
                weatherCity = preferences[PreferencesKeys.WEATHER_CITY] ?: "Canberra",
                calendarMaskLocked = preferences[PreferencesKeys.CALENDAR_MASK_LOCKED] ?: false,
                kenBurnsEnabled = preferences[PreferencesKeys.KEN_BURNS_ENABLED] ?: true,
                showWallpaperBackground = preferences[PreferencesKeys.SHOW_WALLPAPER_BACKGROUND] ?: true,
                contentBrightness = preferences[PreferencesKeys.CONTENT_BRIGHTNESS] ?: 1.0f,
                showWeather = preferences[PreferencesKeys.SHOW_WEATHER] ?: true,
                showBattery = preferences[PreferencesKeys.SHOW_BATTERY] ?: true,
                clockStyle = preferences[PreferencesKeys.CLOCK_STYLE] ?: "DEFAULT",
                clockColorId = preferences[PreferencesKeys.CLOCK_COLOR_ID] ?: "PURE_WHITE",
                clockFullColor = preferences[PreferencesKeys.CLOCK_FULL_COLOR] ?: false,
                calendarStyle = preferences[PreferencesKeys.CALENDAR_STYLE] ?: "DEFAULT",
                calendarColorId = preferences[PreferencesKeys.CALENDAR_COLOR_ID] ?: "SIGNATURE_BLUE",
                calendarFullColor = preferences[PreferencesKeys.CALENDAR_FULL_COLOR] ?: false,
                powerProfile = preferences[PreferencesKeys.POWER_PROFILE] ?: "AMBIENT",
                autoEcoOnBatterySaver = preferences[PreferencesKeys.AUTO_ECO_ON_BATTERY_SAVER] ?: true,
                autoOffMinutes = preferences[PreferencesKeys.AUTO_OFF_MINUTES] ?: 0,
                suppressOnCalls = preferences[PreferencesKeys.SUPPRESS_ON_CALLS] ?: true
            )
        }

    val triggerPolicyFlow: Flow<TriggerPolicy> = userPreferencesFlow.map { prefs ->
        TriggerPolicy(
            isAutoStandbyEnabled = prefs.isAutoStandbyEnabled,
            onlyWhileCharging = prefs.onlyWhileCharging,
            minBatteryPercent = prefs.minBatteryPercent,
            dwellTimeMs = prefs.dwellTimeMs,
            cooldownDurationMs = prefs.cooldownDurationMs
        )
    }

    suspend fun setAutoStandbyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_STANDBY_ENABLED] = enabled
        }
    }

    suspend fun setOnlyWhileCharging(onlyWhileCharging: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONLY_WHILE_CHARGING] = onlyWhileCharging
        }
    }

    suspend fun setDwellTimeMs(dwellMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DWELL_TIME_MS] = dwellMs
        }
    }

    suspend fun setActivePreset(preset: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_PRESET] = preset
        }
    }

    suspend fun setScreenDimLevel(dimLevel: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCREEN_DIM_LEVEL] = dimLevel.coerceIn(0.01f, 1.0f)
        }
    }

    suspend fun setCustomLayout(serialized: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_LAYOUT] = serialized
        }
    }

    suspend fun setWeatherCity(cityName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEATHER_CITY] = cityName
        }
    }

    suspend fun setCalendarMaskLocked(mask: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALENDAR_MASK_LOCKED] = mask
        }
    }

    suspend fun setKenBurnsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEN_BURNS_ENABLED] = enabled
        }
    }

    suspend fun setShowWallpaperBackground(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_WALLPAPER_BACKGROUND] = enabled
        }
    }

    suspend fun setContentBrightness(brightness: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTENT_BRIGHTNESS] = brightness.coerceIn(0.15f, 1.0f)
        }
    }

    suspend fun setShowWeather(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_WEATHER] = enabled
        }
    }

    suspend fun setShowBattery(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_BATTERY] = enabled
        }
    }

    suspend fun setClockStyle(styleId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOCK_STYLE] = styleId
        }
    }

    suspend fun setClockColorId(colorId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOCK_COLOR_ID] = colorId
        }
    }

    suspend fun setClockFullColor(fullColor: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOCK_FULL_COLOR] = fullColor
        }
    }

    suspend fun setCalendarStyle(styleId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALENDAR_STYLE] = styleId
        }
    }

    suspend fun setCalendarColorId(colorId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALENDAR_COLOR_ID] = colorId
        }
    }

    suspend fun setCalendarFullColor(fullColor: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALENDAR_FULL_COLOR] = fullColor
        }
    }

    suspend fun setPowerProfile(profileId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POWER_PROFILE] = profileId
        }
    }

    suspend fun setAutoEcoOnBatterySaver(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_ECO_ON_BATTERY_SAVER] = enabled
        }
    }

    suspend fun setAutoOffMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_OFF_MINUTES] = minutes
        }
    }

    suspend fun setSuppressOnCalls(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUPPRESS_ON_CALLS] = enabled
        }
    }
}
