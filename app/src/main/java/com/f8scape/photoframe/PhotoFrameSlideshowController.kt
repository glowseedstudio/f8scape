package com.f8scape.photoframe

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.f8scape.util.F8Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class SlideshowState(
    val currentBitmap: Bitmap? = null,
    val currentUri: Uri? = null,
    val previousBitmap: Bitmap? = null,
    val transitionTrigger: Long = 0L,
    val totalPhotos: Int = 0,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false
)

/**
 * Production lifecycle-aware slideshow controller.
 * Handles immediate initial image loading, seamless background preloading,
 * automatic advancing with configurable intervals, and non-repeating shuffle mode.
 */
class PhotoFrameSlideshowController(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "SlideshowController"
    }

    private val _state = MutableStateFlow(SlideshowState())
    val state: StateFlow<SlideshowState> = _state.asStateFlow()

    private var activeUris: List<Uri> = emptyList()
    private var playlist: List<Uri> = emptyList()
    private var playlistIndex: Int = 0
    private var isShuffle: Boolean = true
    private var intervalSeconds: Int = 60

    private var timerJob: Job? = null
    private var preloadedBitmap: Bitmap? = null
    private var preloadedUri: Uri? = null

    /**
     * Updates the photo collection and playback parameters.
     * Starts or restarts slideshow automatically if active.
     */
    fun updateSource(
        uris: List<Uri>,
        shuffle: Boolean,
        intervalSec: Int
    ) {
        val urisChanged = activeUris != uris
        val shuffleChanged = isShuffle != shuffle
        val intervalChanged = intervalSeconds != intervalSec

        activeUris = uris
        isShuffle = shuffle
        intervalSeconds = intervalSec.coerceAtLeast(10)

        if (uris.isEmpty()) {
            stop()
            _state.value = SlideshowState(isEmpty = true, isLoading = false)
            return
        }

        if (urisChanged || shuffleChanged || playlist.isEmpty()) {
            rebuildPlaylist(activeUris, isShuffle)
            playlistIndex = 0
            loadInitialImage()
        } else if (intervalChanged) {
            restartTimer()
        }
    }

    /**
     * Generates the playback playlist with strict immediate-repeat avoidance in shuffle mode.
     */
    fun rebuildPlaylist(
        source: List<Uri>,
        shuffle: Boolean,
        lastUri: Uri? = null
    ): List<Uri> {
        if (source.isEmpty()) return emptyList()
        if (source.size == 1) return source

        return if (shuffle) {
            val shuffled = source.shuffled(Random(System.currentTimeMillis()))
            // Ensure first item in new shuffle is not the same as the last shown URI
            if (lastUri != null && shuffled.firstOrNull() == lastUri && shuffled.size > 1) {
                // Swap first with second
                listOf(shuffled[1], shuffled[0]) + shuffled.drop(2)
            } else {
                shuffled
            }
        } else {
            source
        }
    }

    private fun loadInitialImage() {
        timerJob?.cancel()
        coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true, isEmpty = false, totalPhotos = activeUris.size)
            val firstUri = playlist.getOrNull(playlistIndex)
            if (firstUri == null) {
                _state.value = SlideshowState(isEmpty = true, isLoading = false)
                return@launch
            }

            val bitmap = PhotoDecoder.decodePhoto(context, firstUri)
            if (bitmap != null) {
                _state.value = SlideshowState(
                    currentBitmap = bitmap,
                    currentUri = firstUri,
                    totalPhotos = activeUris.size,
                    isLoading = false,
                    isEmpty = false
                )
                preloadNext()
                startTimer()
            } else {
                // If initial photo fails, try advancing to next valid image
                advanceToNext(skipAnimation = true)
            }
        }
    }

    private fun preloadNext() {
        if (playlist.size <= 1) return
        val nextIdx = (playlistIndex + 1) % playlist.size
        val nextUri = playlist.getOrNull(nextIdx) ?: return
        if (nextUri == preloadedUri && preloadedBitmap != null) return

        coroutineScope.launch {
            val bmp = PhotoDecoder.decodePhoto(context, nextUri)
            if (bmp != null) {
                preloadedBitmap = bmp
                preloadedUri = nextUri
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        if (activeUris.size <= 1) return // Single photo, no timer needed

        timerJob = coroutineScope.launch {
            while (isActive) {
                delay(intervalSeconds * 1000L)
                advanceToNext()
            }
        }
    }

    private fun restartTimer() {
        startTimer()
    }

    /**
     * Advances to the next photograph in the playlist.
     */
    fun advanceToNext(skipAnimation: Boolean = false) {
        if (activeUris.isEmpty()) return
        if (activeUris.size == 1) return // No-op for single image

        coroutineScope.launch {
            val previousBitmap = _state.value.currentBitmap
            playlistIndex++

            if (playlistIndex >= playlist.size) {
                val lastUri = playlist.lastOrNull()
                playlist = rebuildPlaylist(activeUris, isShuffle, lastUri)
                playlistIndex = 0
            }

            val targetUri = playlist.getOrNull(playlistIndex) ?: return@launch

            // Use preloaded bitmap if matching target URI, otherwise decode fresh
            val newBitmap = if (preloadedUri == targetUri && preloadedBitmap != null) {
                val bmp = preloadedBitmap
                preloadedBitmap = null
                preloadedUri = null
                bmp
            } else {
                PhotoDecoder.decodePhoto(context, targetUri)
            }

            if (newBitmap != null) {
                _state.value = _state.value.copy(
                    currentBitmap = newBitmap,
                    currentUri = targetUri,
                    previousBitmap = if (skipAnimation) null else previousBitmap,
                    transitionTrigger = System.currentTimeMillis(),
                    totalPhotos = activeUris.size,
                    isLoading = false,
                    isEmpty = false
                )
                preloadNext()
            } else {
                // Corrupt or deleted image, skip to next silently
                F8Log.w(TAG, "Skipping inaccessible photo: $targetUri")
                advanceToNext(skipAnimation = true)
            }
        }
    }

    /**
     * Suspends timers and background decoding when StandBy is inactive.
     */
    fun stop() {
        timerJob?.cancel()
        timerJob = null
        preloadedBitmap = null
        preloadedUri = null
    }

    /**
     * Resumes slideshow advancement when StandBy activates again.
     */
    fun resume() {
        if (activeUris.size > 1 && timerJob == null) {
            preloadNext()
            startTimer()
        }
    }
}
