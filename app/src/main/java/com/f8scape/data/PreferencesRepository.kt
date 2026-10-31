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
    val suppressOnCalls: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
    val weatherTempUnit: String = "SYSTEM", // SYSTEM, CELSIUS, FAHRENHEIT
    val enableNotificationCardsView: Boolean = false,
    val enableWeatherPage: Boolean = false,
    val photoFrameSourceType: String = "PHOTOS", // "PHOTOS" or "FOLDER"
    val photoFrameSelectedUrisSerialized: String = "",
    val photoFrameFolderUri: String = "",
    val photoFrameIntervalSeconds: Int = 60,
    val photoFrameShuffle: Boolean = true,
    val photoFrameFitMode: String = "CROP", // "CROP" or "FIT"
    val photoFrameMotion: Boolean = false,
    val photoFrameOverlayType: String = "CLOCK_PHOTO", // "IMMERSIVE", "CLOCK_PHOTO", "CALENDAR_PHOTO"
    val photoFrameOverlayPosition: String = "TOP_LEFT", // "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"
    val photoFrameShowWhileLocked: Boolean = false,
    val photoFrameMusicBehavior: String = "KEEP_PHOTO" // "KEEP_PHOTO" or "SWITCH_TO_MEDIA"
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
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val WEATHER_TEMP_UNIT = stringPreferencesKey("weather_temp_unit")
        val ENABLE_NOTIFICATION_CARDS_VIEW = booleanPreferencesKey("enable_notification_cards_view")
        val ENABLE_WEATHER_PAGE = booleanPreferencesKey("enable_weather_page")
        val PHOTO_FRAME_SOURCE_TYPE = stringPreferencesKey("photo_frame_source_type")
        val PHOTO_FRAME_SELECTED_URIS = stringPreferencesKey("photo_frame_selected_uris")
        val PHOTO_FRAME_FOLDER_URI = stringPreferencesKey("photo_frame_folder_uri")
        val PHOTO_FRAME_INTERVAL_SECONDS = intPreferencesKey("photo_frame_interval_seconds")
        val PHOTO_FRAME_SHUFFLE = booleanPreferencesKey("photo_frame_shuffle")
        val PHOTO_FRAME_FIT_MODE = stringPreferencesKey("photo_frame_fit_mode")
        val PHOTO_FRAME_MOTION = booleanPreferencesKey("photo_frame_motion")
        val PHOTO_FRAME_OVERLAY_TYPE = stringPreferencesKey("photo_frame_overlay_type")
        val PHOTO_FRAME_OVERLAY_POSITION = stringPreferencesKey("photo_frame_overlay_position")
        val PHOTO_FRAME_SHOW_WHILE_LOCKED = booleanPreferencesKey("photo_frame_show_while_locked")
        val PHOTO_FRAME_MUSIC_BEHAVIOR = stringPreferencesKey("photo_frame_music_behavior")
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
                suppressOnCalls = preferences[PreferencesKeys.SUPPRESS_ON_CALLS] ?: true,
                hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false,
                weatherTempUnit = preferences[PreferencesKeys.WEATHER_TEMP_UNIT] ?: "SYSTEM",
                enableNotificationCardsView = preferences[PreferencesKeys.ENABLE_NOTIFICATION_CARDS_VIEW] ?: false,
                enableWeatherPage = preferences[PreferencesKeys.ENABLE_WEATHER_PAGE] ?: false,
                photoFrameSourceType = preferences[PreferencesKeys.PHOTO_FRAME_SOURCE_TYPE] ?: "PHOTOS",
                photoFrameSelectedUrisSerialized = preferences[PreferencesKeys.PHOTO_FRAME_SELECTED_URIS] ?: "",
                photoFrameFolderUri = preferences[PreferencesKeys.PHOTO_FRAME_FOLDER_URI] ?: "",
                photoFrameIntervalSeconds = preferences[PreferencesKeys.PHOTO_FRAME_INTERVAL_SECONDS] ?: 60,
                photoFrameShuffle = preferences[PreferencesKeys.PHOTO_FRAME_SHUFFLE] ?: true,
                photoFrameFitMode = preferences[PreferencesKeys.PHOTO_FRAME_FIT_MODE] ?: "CROP",
                photoFrameMotion = preferences[PreferencesKeys.PHOTO_FRAME_MOTION] ?: false,
                photoFrameOverlayType = preferences[PreferencesKeys.PHOTO_FRAME_OVERLAY_TYPE] ?: "CLOCK_PHOTO",
                photoFrameOverlayPosition = preferences[PreferencesKeys.PHOTO_FRAME_OVERLAY_POSITION] ?: "TOP_LEFT",
                photoFrameShowWhileLocked = preferences[PreferencesKeys.PHOTO_FRAME_SHOW_WHILE_LOCKED] ?: false,
                photoFrameMusicBehavior = preferences[PreferencesKeys.PHOTO_FRAME_MUSIC_BEHAVIOR] ?: "KEEP_PHOTO"
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

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    suspend fun setWeatherTempUnit(unitId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEATHER_TEMP_UNIT] = unitId
        }
    }

    suspend fun setEnableNotificationCardsView(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_NOTIFICATION_CARDS_VIEW] = enabled
        }
    }

    suspend fun setEnableWeatherPage(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_WEATHER_PAGE] = enabled
        }
    }

    suspend fun setPhotoFrameSourceType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_SOURCE_TYPE] = type
        }
    }

    suspend fun setPhotoFrameSelectedUris(uris: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_SELECTED_URIS] = uris.distinct().joinToString("\n")
        }
    }

    suspend fun addPhotoFrameUris(newUris: List<String>) {
        context.dataStore.edit { preferences ->
            val existing = (preferences[PreferencesKeys.PHOTO_FRAME_SELECTED_URIS] ?: "")
                .split("\n")
                .filter { it.isNotBlank() }
            val combined = (existing + newUris).distinct()
            preferences[PreferencesKeys.PHOTO_FRAME_SELECTED_URIS] = combined.joinToString("\n")
        }
    }

    suspend fun removePhotoFrameUri(uriToRemove: String) {
        context.dataStore.edit { preferences ->
            val existing = (preferences[PreferencesKeys.PHOTO_FRAME_SELECTED_URIS] ?: "")
                .split("\n")
                .filter { it.isNotBlank() && it != uriToRemove }
            preferences[PreferencesKeys.PHOTO_FRAME_SELECTED_URIS] = existing.joinToString("\n")
        }
    }

    suspend fun clearPhotoFrameCollection() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_SELECTED_URIS] = ""
            preferences[PreferencesKeys.PHOTO_FRAME_FOLDER_URI] = ""
        }
    }

    suspend fun setPhotoFrameFolderUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_FOLDER_URI] = uri
        }
    }

    suspend fun setPhotoFrameIntervalSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_INTERVAL_SECONDS] = seconds
        }
    }

    suspend fun setPhotoFrameShuffle(shuffle: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_SHUFFLE] = shuffle
        }
    }

    suspend fun setPhotoFrameFitMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_FIT_MODE] = mode
        }
    }

    suspend fun setPhotoFrameMotion(motion: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_MOTION] = motion
        }
    }

    suspend fun setPhotoFrameOverlayType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_OVERLAY_TYPE] = type
        }
    }

    suspend fun setPhotoFrameOverlayPosition(position: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_OVERLAY_POSITION] = position
        }
    }

    suspend fun setPhotoFrameShowWhileLocked(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_SHOW_WHILE_LOCKED] = show
        }
    }

    suspend fun setPhotoFrameMusicBehavior(behavior: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_MUSIC_BEHAVIOR] = behavior
        }
    }

    suspend fun resetPhotoFrameSettings() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_INTERVAL_SECONDS] = 60
            preferences[PreferencesKeys.PHOTO_FRAME_SHUFFLE] = true
            preferences[PreferencesKeys.PHOTO_FRAME_FIT_MODE] = "CROP"
            preferences[PreferencesKeys.PHOTO_FRAME_MOTION] = false
            preferences[PreferencesKeys.PHOTO_FRAME_OVERLAY_TYPE] = "CLOCK_PHOTO"
            preferences[PreferencesKeys.PHOTO_FRAME_OVERLAY_POSITION] = "TOP_LEFT"
            preferences[PreferencesKeys.PHOTO_FRAME_SHOW_WHILE_LOCKED] = false
            preferences[PreferencesKeys.PHOTO_FRAME_MUSIC_BEHAVIOR] = "KEEP_PHOTO"
        }
    }
}
