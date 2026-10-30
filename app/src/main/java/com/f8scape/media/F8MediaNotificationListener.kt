package com.f8scape.media

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
        MediaPlaybackRepository.setTransportHandler(this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        F8Log.i("F8Media", "F8MediaNotificationListener connected")
        MediaPlaybackRepository.setTransportHandler(this)
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = ComponentName(this, F8MediaNotificationListener::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            refreshActiveSessions()
        } catch (e: Exception) {
            F8Log.e("F8Media", "Failed to register active sessions changed listener", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        F8Log.i("F8Media", "F8MediaNotificationListener disconnected")
        cleanupController()
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (ignored: Exception) {}
        mediaSessionManager = null
        MediaPlaybackRepository.setTransportHandler(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupController()
        MediaPlaybackRepository.setTransportHandler(null)
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

        MediaPlaybackRepository.updateTrackInfo(
            MediaTrackInfo(
                title = title.ifBlank { "Media Playing" },
                artist = artist.ifBlank { controller.packageName ?: "" },
                album = album,
                artwork = artBitmap,
                isPlaying = isPlaying,
                durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
                positionMs = playbackState?.position ?: 0L,
                packageName = controller.packageName,
                hasActiveSession = true,
                canPlayPause = canPlayPause,
                canSkipNext = canSkipNext,
                canSkipPrevious = canSkipPrevious
            )
        )
    }

    private fun updatePlaybackState(state: PlaybackState?) {
        val current = MediaPlaybackRepository.mediaTrackState.value
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        MediaPlaybackRepository.updateTrackInfo(
            current.copy(
                isPlaying = isPlaying,
                positionMs = state?.position ?: current.positionMs
            )
        )
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        activeController?.let { syncState(it) }
    }

    private fun cleanupController() {
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

    override fun onSkipToPrevious() {
        try {
            activeController?.transportControls?.skipToPrevious()
        } catch (e: Exception) {
            F8Log.e("F8Media", "Failed to trigger skipToPrevious", e)
        }
    }

    companion object {
        fun isNotificationAccessGranted(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(packageName)
        }
    }
}
