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
    val kenBurnsEnabled: Boolean = true
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
                kenBurnsEnabled = preferences[PreferencesKeys.KEN_BURNS_ENABLED] ?: true
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
}
