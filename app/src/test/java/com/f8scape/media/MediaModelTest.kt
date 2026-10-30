package com.f8scape.media

import org.junit.Assert.*
import org.junit.Test

class MediaModelTest {

    @Test
    fun defaultMediaTrackInfo_isNotPlayingAndNoSession() {
        val info = MediaTrackInfo()
        assertFalse(info.isPlaying)
        assertFalse(info.hasActiveSession)
        assertEquals("", info.title)
        assertEquals("", info.artist)
    }

    @Test
    fun mediaPlaybackRepository_updatesStateCorrectly() {
        val updated = MediaTrackInfo(
            title = "Ambient Echoes",
            artist = "Solar Fields",
            album = "Origin",
            isPlaying = true,
            hasActiveSession = true
        )
        MediaPlaybackRepository.updateTrackInfo(updated)

        val current = MediaPlaybackRepository.mediaTrackState.value
        assertEquals("Ambient Echoes", current.title)
        assertEquals("Solar Fields", current.artist)
        assertEquals("Origin", current.album)
        assertTrue(current.isPlaying)
        assertTrue(current.hasActiveSession)
    }

    @Test
    fun mediaPlaybackRepository_routesTransportActions() {
        var played = false
        var paused = false
        var nextSkipped = false
        var prevSkipped = false

        val testHandler = object : MediaTransportActionHandler {
            override fun onPlay() { played = true }
            override fun onPause() { paused = true }
            override fun onSkipToNext() { nextSkipped = true }
            override fun onSkipToPrevious() { prevSkipped = true }
        }

        MediaPlaybackRepository.setTransportHandler(testHandler)

        MediaPlaybackRepository.play()
        assertTrue(played)

        MediaPlaybackRepository.pause()
        assertTrue(paused)

        MediaPlaybackRepository.skipToNext()
        assertTrue(nextSkipped)

        MediaPlaybackRepository.skipToPrevious()
        assertTrue(prevSkipped)

        // Clean up
        MediaPlaybackRepository.setTransportHandler(null)
    }

    @Test
    fun mediaPlaybackRepository_togglePlayPause_invertsState() {
        var playCalled = false
        var pauseCalled = false

        MediaPlaybackRepository.setTransportHandler(object : MediaTransportActionHandler {
            override fun onPlay() { playCalled = true }
            override fun onPause() { pauseCalled = true }
            override fun onSkipToNext() {}
            override fun onSkipToPrevious() {}
        })

        // When not playing -> toggle calls play
        MediaPlaybackRepository.updateTrackInfo(MediaTrackInfo(isPlaying = false))
        MediaPlaybackRepository.togglePlayPause()
        assertTrue(playCalled)
        assertFalse(pauseCalled)

        // When playing -> toggle calls pause
        MediaPlaybackRepository.updateTrackInfo(MediaTrackInfo(isPlaying = true))
        MediaPlaybackRepository.togglePlayPause()
        assertTrue(pauseCalled)

        // Clean up
        MediaPlaybackRepository.setTransportHandler(null)
    }
}
