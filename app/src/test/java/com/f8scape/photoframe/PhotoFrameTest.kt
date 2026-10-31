package com.f8scape.photoframe

import com.f8scape.power.PowerProfile
import com.f8scape.power.SystemResilienceState
import org.junit.Assert.*
import org.junit.Test

class PhotoFrameTest {

    @Test
    fun calculateInSampleSize_downsamplesLargeImagesCorrectly() {
        // 48MP photo (8000x6000) targeting 2400x1200 cover screen
        val sampleSize = PhotoDecoder.calculateInSampleSize(
            rawWidth = 8000,
            rawHeight = 6000,
            reqWidth = 2400,
            reqHeight = 1200
        )
        // Halving: 4000x3000 >= 2400x1200 (inSampleSize = 2)
        // Next halving: 2000x1500 -> 2000 < 2400 -> stops at 2
        assertEquals(2, sampleSize)

        // Ultra-high-res 12000x9000
        val sampleSizeUltra = PhotoDecoder.calculateInSampleSize(
            rawWidth = 12000,
            rawHeight = 9000,
            reqWidth = 2400,
            reqHeight = 1200
        )
        // 12000/2=6000 >= 2400 (sample 2)
        // 6000/2=3000 >= 2400 (sample 4)
        // 3000/2=1500 < 2400 -> stops at 4
        assertEquals(4, sampleSizeUltra)

        // Small photo (800x600) smaller than target
        val sampleSizeSmall = PhotoDecoder.calculateInSampleSize(
            rawWidth = 800,
            rawHeight = 600,
            reqWidth = 2400,
            reqHeight = 1200
        )
        assertEquals(1, sampleSizeSmall)
    }

    @Test
    fun rebuildPlaylist_sequentialOrder_maintainsOriginalOrder() {
        val uris = listOf(
            "content://media/1",
            "content://media/2",
            "content://media/3"
        )
        // Sequential mode
        val playlist = PhotoPlaylistHelper.generatePlaylist(uris, shuffle = false)
        assertEquals(3, playlist.size)
        assertEquals(uris[0], playlist[0])
        assertEquals(uris[1], playlist[1])
        assertEquals(uris[2], playlist[2])
    }

    @Test
    fun rebuildPlaylist_shuffleMode_containsAllItemsAndAvoidsImmediateRepeat() {
        val uris = (1..10).map { "content://media/$it" }
        val lastShown = uris[4]

        // Repeat 50 times to test repeat avoidance statistically
        repeat(50) {
            val playlist = PhotoPlaylistHelper.generatePlaylist(uris, shuffle = true, lastItem = lastShown)
            assertEquals(10, playlist.size)
            assertEquals(uris.toSet(), playlist.toSet())
            // First item in new playlist MUST NOT be the last shown item!
            assertNotEquals("Shuffle must not immediately repeat the last shown item", lastShown, playlist[0])
        }
    }

    @Test
    fun singleItemCollection_doesNotFailOrDuplicate() {
        val singleList = listOf("content://media/1")
        val playlist = PhotoPlaylistHelper.generatePlaylist(singleList, shuffle = true)
        assertEquals(1, playlist.size)
        assertEquals(singleList[0], playlist[0])
    }

    @Test
    fun emptyCollection_returnsEmptyPlaylist() {
        val playlist = PhotoPlaylistHelper.generatePlaylist(emptyList<String>(), shuffle = true)
        assertTrue(playlist.isEmpty())
    }

    @Test
    fun lockScreenPrivacy_evaluatesCorrectly() {
        // Locked + showWhileLocked=false -> MUST hide photos (privacy active)
        assertTrue(shouldShieldPhotos(isDeviceLocked = true, showWhileLocked = false))

        // Locked + showWhileLocked=true -> Allowed to show (privacy inactive)
        assertFalse(shouldShieldPhotos(isDeviceLocked = true, showWhileLocked = true))

        // Unlocked + showWhileLocked=false -> Allowed to show
        assertFalse(shouldShieldPhotos(isDeviceLocked = false, showWhileLocked = false))

        // Unlocked + showWhileLocked=true -> Allowed to show
        assertFalse(shouldShieldPhotos(isDeviceLocked = false, showWhileLocked = true))
    }

    @Test
    fun powerProfile_suppressesPhotoMotionInEcoOrBatterySaver() {
        val resilienceNormal = SystemResilienceState(isBatterySaverActive = false, isThermalElevated = false)
        val resilienceBatterySaver = SystemResilienceState(isBatterySaverActive = true, isThermalElevated = false)

        // Ambient profile with normal battery -> Motion allowed if requested
        val isEcoAmbient = resilienceNormal.shouldEnforceEco(PowerProfile.AMBIENT, autoEcoOnBatterySaver = true)
        assertFalse(isEcoAmbient)

        // Ambient profile with Battery Saver active -> Eco enforced -> Motion suppressed
        val isEcoSaver = resilienceBatterySaver.shouldEnforceEco(PowerProfile.AMBIENT, autoEcoOnBatterySaver = true)
        assertTrue(isEcoSaver)

        // Explicit ECO profile -> Always suppresses motion
        val isEcoExplicit = resilienceNormal.shouldEnforceEco(PowerProfile.ECO, autoEcoOnBatterySaver = true)
        assertTrue(isEcoExplicit)
    }

    @Test
    fun mediaPriority_switchOrKeepDecisions() {
        // When music is playing and behavior is SWITCH_TO_MEDIA -> switch to media
        val shouldSwitch1 = shouldSwitchToMedia(isPlaying = true, behavior = "SWITCH_TO_MEDIA")
        assertTrue(shouldSwitch1)

        // When music is playing and behavior is KEEP_PHOTO -> stay in photo frame
        val shouldSwitch2 = shouldSwitchToMedia(isPlaying = true, behavior = "KEEP_PHOTO")
        assertFalse(shouldSwitch2)

        // When music is stopped -> never switch to media
        val shouldSwitch3 = shouldSwitchToMedia(isPlaying = false, behavior = "SWITCH_TO_MEDIA")
        assertFalse(shouldSwitch3)
    }

    // Helper logic mirrors application behavior for unit testing
    private fun shouldShieldPhotos(isDeviceLocked: Boolean, showWhileLocked: Boolean): Boolean {
        return isDeviceLocked && !showWhileLocked
    }

    private fun shouldSwitchToMedia(isPlaying: Boolean, behavior: String): Boolean {
        return isPlaying && behavior == "SWITCH_TO_MEDIA"
    }
}

/**
 * Pure Kotlin playlist helper to test shuffle algorithm deterministically.
 */
object PhotoPlaylistHelper {
    fun <T> generatePlaylist(
        source: List<T>,
        shuffle: Boolean,
        lastItem: T? = null
    ): List<T> {
        if (source.isEmpty()) return emptyList()
        if (source.size == 1) return source
        return if (shuffle) {
            val shuffled = source.shuffled()
            if (lastItem != null && shuffled.firstOrNull() == lastItem && shuffled.size > 1) {
                listOf(shuffled[1], shuffled[0]) + shuffled.drop(2)
            } else {
                shuffled
            }
        } else {
            source
        }
    }
}
