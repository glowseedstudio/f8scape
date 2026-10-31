package com.f8scape.media

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.f8scape.notifications.ActiveNotificationRepository
import com.f8scape.notifications.StatusBarNotificationIcon
import com.f8scape.util.F8Log

/**
 * Android NotificationListenerService implementation allowing F8scape to detect active
 * media sessions (Spotify, YouTube Music, local players, podcasts) and route transport controls.
 */
class F8MediaNotificationListener : NotificationListenerService(), MediaTransportActionHandler {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        mainHandler.post {
            handleSessionsChanged(controllers)
        }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            mainHandler.post {
                updatePlaybackState(state)
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            mainHandler.post {
                updateMetadata(metadata)
            }
        }

        override fun onSessionDestroyed() {
            mainHandler.post {
                F8Log.d("F8Media", "Media session destroyed")
                activeController = null
                refreshActiveSessions()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        MediaPlaybackRepository.setTransportHandler(this)
        ActiveNotificationRepository.setRefreshCallback {
            mainHandler.post { refreshActiveNotifications() }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeInstance = this
        F8Log.i("F8Media", "F8MediaNotificationListener connected")
        MediaPlaybackRepository.setTransportHandler(this)
        ActiveNotificationRepository.setRefreshCallback {
            mainHandler.post { refreshActiveNotifications() }
        }
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = ComponentName(this, F8MediaNotificationListener::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            refreshActiveSessions()
            refreshActiveNotifications()
        } catch (e: Exception) {
            F8Log.e("F8Media", "Failed to register active sessions changed listener", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (activeInstance === this) {
            activeInstance = null
        }
        F8Log.i("F8Media", "F8MediaNotificationListener disconnected")
        cleanupController()
        ActiveNotificationRepository.setRefreshCallback(null)
        ActiveNotificationRepository.updateNotifications(emptyList())
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (ignored: Exception) {}
        mediaSessionManager = null
        MediaPlaybackRepository.setTransportHandler(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance === this) {
            activeInstance = null
        }
        cleanupController()
        ActiveNotificationRepository.setRefreshCallback(null)
        ActiveNotificationRepository.updateNotifications(emptyList())
        MediaPlaybackRepository.setTransportHandler(null)
    }

    fun refreshActiveNotifications() {
        try {
            val sbns = activeNotifications ?: return
            val myPackage = packageName
            F8Log.d("F8Notifications", "refreshActiveNotifications: total sbns = ${sbns.size}")

            val validSbns = sbns.filter { sbn ->
                val notif = sbn.notification ?: return@filter false
                if (sbn.packageName == myPackage) return@filter false

                // Exclude system ongoing/foreground background noise (USB debugging, charging status, etc.)
                val isSystemPkg = sbn.packageName == "android" || sbn.packageName == "com.android.systemui"
                val isSystemOngoing = isSystemPkg && (sbn.isOngoing || (notif.flags and Notification.FLAG_ONGOING_EVENT != 0))
                if (isSystemOngoing) return@filter false

                // Ignore MIN priority (silent/hidden background channels)
                if (notif.priority == Notification.PRIORITY_MIN) return@filter false

                // Must have a valid small icon or legacy icon resource
                notif.smallIcon != null || notif.icon != 0
            }

            F8Log.d("F8Notifications", "refreshActiveNotifications: validSbns = ${validSbns.size}")

            val grouped = validSbns.groupBy { it.packageName }
            val notificationIcons = grouped.mapNotNull { (pkg, list) ->
                val latest = list.maxByOrNull { it.postTime } ?: list.first()
                val bitmap = ActiveNotificationRepository.extractSmallIconBitmap(this, latest)
                if (bitmap != null) {
                    StatusBarNotificationIcon(
                        key = latest.key,
                        packageName = pkg,
                        iconBitmap = bitmap,
                        postTime = latest.postTime,
                        count = list.size
                    )
                } else {
                    F8Log.w("F8Notifications", "Could not extract icon bitmap for $pkg")
                    null
                }
            }.sortedByDescending { it.postTime }.take(6)

            F8Log.d("F8Notifications", "refreshActiveNotifications: loaded ${notificationIcons.size} icons")
            ActiveNotificationRepository.updateNotifications(notificationIcons)
        } catch (e: Exception) {
            F8Log.e("F8Notifications", "Error refreshing active notification icons", e)
        }
    }

    private fun refreshActiveSessions() {
        try {
            val component = ComponentName(this, F8MediaNotificationListener::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(component)
            handleSessionsChanged(controllers)
        } catch (e: SecurityException) {
            F8Log.w("F8Media", "Notification listener access not yet granted or restricted: ${e.message}")
        } catch (e: Exception) {
            F8Log.e("F8Media", "Error refreshing active sessions", e)
        }
    }

    private val positionTickRunnable = object : Runnable {
        override fun run() {
            val controller = activeController
            val state = controller?.playbackState
            if (controller != null && state?.state == PlaybackState.STATE_PLAYING) {
                val current = MediaPlaybackRepository.mediaTrackState.value
                val duration = current.durationMs
                val computedPos = computeCurrentPosition(state, duration)
                if (current.hasActiveSession) {
                    MediaPlaybackRepository.updateTrackInfo(
                        current.copy(positionMs = computedPos, isPlaying = true)
                    )
                }
                mainHandler.postDelayed(this, 500L)
            }
        }
    }

    private fun computeCurrentPosition(state: PlaybackState?, durationMs: Long): Long {
        if (state == null) return 0L
        val basePos = state.position
        if (state.state == PlaybackState.STATE_PLAYING && state.lastPositionUpdateTime > 0L) {
            val timeDelta = android.os.SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            val speed = if (state.playbackSpeed > 0f) state.playbackSpeed else 1.0f
            val calculated = basePos + (timeDelta * speed).toLong()
            return if (durationMs > 0L) calculated.coerceIn(0L, durationMs) else calculated.coerceAtLeast(0L)
        }
        return if (durationMs > 0L) basePos.coerceIn(0L, durationMs) else basePos.coerceAtLeast(0L)
    }

    private fun startPositionTicker() {
        mainHandler.removeCallbacks(positionTickRunnable)
        mainHandler.post(positionTickRunnable)
    }

    private fun stopPositionTicker() {
        mainHandler.removeCallbacks(positionTickRunnable)
    }

    private fun handleSessionsChanged(controllers: List<MediaController>?) {
        val newController = controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull()

        if (newController?.sessionToken != activeController?.sessionToken) {
            cleanupController()
            activeController = newController
            activeController?.registerCallback(controllerCallback, mainHandler)
        }

        if (activeController != null) {
            syncState(activeController!!)
        } else {
            stopPositionTicker()
            MediaPlaybackRepository.updateTrackInfo(MediaTrackInfo(hasActiveSession = false))
        }
    }

    private fun syncState(controller: MediaController) {
        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: ""
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""

        val artBitmap: Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val actions = playbackState?.actions ?: 0L

        val canPlayPause = (actions and (PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE)) != 0L || true
        val canSkipNext = (actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L || true
        val canSkipPrevious = (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L || true

        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val positionMs = computeCurrentPosition(playbackState, durationMs)

        MediaPlaybackRepository.updateTrackInfo(
            MediaTrackInfo(
                title = title.ifBlank { "Media Playing" },
                artist = artist.ifBlank { controller.packageName ?: "" },
                album = album,
                artwork = artBitmap,
                isPlaying = isPlaying,
                durationMs = durationMs,
                positionMs = positionMs,
                packageName = controller.packageName,
                hasActiveSession = true,
                canPlayPause = canPlayPause,
                canSkipNext = canSkipNext,
                canSkipPrevious = canSkipPrevious
            )
        )

        if (isPlaying) {
            startPositionTicker()
        } else {
            stopPositionTicker()
        }
    }

    private fun updatePlaybackState(state: PlaybackState?) {
        val current = MediaPlaybackRepository.mediaTrackState.value
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val currentPosition = computeCurrentPosition(state, current.durationMs)
        MediaPlaybackRepository.updateTrackInfo(
            current.copy(
                isPlaying = isPlaying,
                positionMs = currentPosition
            )
        )
        if (isPlaying) {
            startPositionTicker()
        } else {
            stopPositionTicker()
        }
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        activeController?.let { controller ->
            syncState(controller)
            val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            if (duration <= 0L) {
                mainHandler.postDelayed({
                    activeController?.let { syncState(it) }
                }, 350L)
            }
        }
    }

    private fun cleanupController() {
        stopPositionTicker()
        try {
            activeController?.unregisterCallback(controllerCallback)
        } catch (ignored: Exception) {}
        activeController = null
    }

    override fun onPlay() {
        try {
            activeController?.transportControls?.play()
        } catch (e: Exception) {
            F8Log.e("F8Media", "Failed to trigger play", e)
        }
    }

    override fun onPause() {
        try {
            activeController?.transportControls?.pause()
        } catch (e: Exception) {
            F8Log.e("F8Media", "Failed to trigger pause", e)
        }
    }

    override fun onSkipToNext() {
        try {
            activeController?.transportControls?.skipToNext()
        } catch (e: Exception) {
            F8Log.e("F8Media", "Failed to trigger skipToNext", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        // Refresh active sessions on notification changes (e.g., track changes from Spotify/YouTube Music)
        refreshActiveSessions()
        mainHandler.post {
            refreshActiveNotifications()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshActiveSessions()
        mainHandler.post {
            refreshActiveNotifications()
        }
    }

    override fun onSkipToPrevious() {
        try {
            activeController?.transportControls?.skipToPrevious()
        } catch (e: Exception) {
            F8Log.e("F8Media", "Failed to trigger skipToPrevious", e)
        }
    }

    override fun onSeekTo(positionMs: Long) {
        try {
            activeController?.transportControls?.seekTo(positionMs)
            val current = MediaPlaybackRepository.mediaTrackState.value
            MediaPlaybackRepository.updateTrackInfo(current.copy(positionMs = positionMs))
        } catch (e: Exception) {
            F8Log.e("F8Media", "Failed to trigger seekTo", e)
        }
    }

    companion object {
        var activeInstance: F8MediaNotificationListener? = null
            private set

        fun isNotificationAccessGranted(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(packageName)
        }
    }
}
