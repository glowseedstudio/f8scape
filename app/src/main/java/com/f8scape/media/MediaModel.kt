package com.f8scape.media

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Representation of currently active media playback in F8scape.
 */
data class MediaTrackInfo(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artwork: Bitmap? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val packageName: String? = null,
    val hasActiveSession: Boolean = false,
    val canPlayPause: Boolean = true,
    val canSkipNext: Boolean = true,
    val canSkipPrevious: Boolean = true
)

/**
 * Global repository holding the active media session state and routing transport controls.
 */
object MediaPlaybackRepository {
    private val _mediaTrackState = MutableStateFlow(MediaTrackInfo())
    val mediaTrackState: StateFlow<MediaTrackInfo> = _mediaTrackState.asStateFlow()

    private var transportActionHandler: MediaTransportActionHandler? = null

    fun setTransportHandler(handler: MediaTransportActionHandler?) {
        transportActionHandler = handler
    }

    fun updateTrackInfo(info: MediaTrackInfo) {
        _mediaTrackState.value = info
    }

    fun play() {
        transportActionHandler?.onPlay()
    }

    fun pause() {
        transportActionHandler?.onPause()
    }

    fun togglePlayPause() {
        if (_mediaTrackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun skipToNext() {
        transportActionHandler?.onSkipToNext()
    }

    fun skipToPrevious() {
        transportActionHandler?.onSkipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        transportActionHandler?.onSeekTo(positionMs)
    }
}

/**
 * Interface implemented by services capable of dispatching media playback actions.
 */
interface MediaTransportActionHandler {
    fun onPlay()
    fun onPause()
    fun onSkipToNext()
    fun onSkipToPrevious()
    fun onSeekTo(positionMs: Long)
}
