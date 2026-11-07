package com.f8scape.ocarina

import org.junit.Assert.*
import org.junit.Test

class OcarinaSongTest {

    @Test
    fun testAllSongsExistAndHaveNotes() {
        assertEquals(12, OcarinaSongs.allSongs.size)

        for (song in OcarinaSongs.allSongs) {
            assertTrue("Song ${song.title} button sequence should not be empty", song.buttonSequence.isNotEmpty())
            assertTrue("Song ${song.title} melody notes should not be empty", song.melodyNotes.isNotEmpty())
            assertTrue("Song ${song.title} lore should not be empty", song.lore.isNotBlank())

            // Ensure first notes match button sequence
            for (i in song.buttonSequence.indices) {
                val button = song.buttonSequence[i]
                val note = song.melodyNotes[i]
                assertEquals(
                    "Song ${song.title} note $i button should match sequence",
                    button,
                    note.button
                )
            }
        }
    }

    @Test
    fun testAudioEngineInitialization() {
        val engine = OcarinaAudioEngine()
        assertFalse(engine.isPlaying.value)
        assertNull(engine.currentSongId.value)
        assertEquals(-1, engine.currentNoteIndex.value)
        assertTrue(engine.isMicInputMode.value)

        engine.setMicInputMode(false)
        assertFalse(engine.isMicInputMode.value)
        engine.release()
    }

    @Test
    fun testButtonFromFrequency() {
        assertEquals(OcarinaButton.A, OcarinaButton.fromFrequency(293.66f))
        assertEquals(OcarinaButton.C_DOWN, OcarinaButton.fromFrequency(349.23f))
        assertEquals(OcarinaButton.C_RIGHT, OcarinaButton.fromFrequency(440.00f))
        assertEquals(OcarinaButton.C_LEFT, OcarinaButton.fromFrequency(493.88f))
        assertEquals(OcarinaButton.C_UP, OcarinaButton.fromFrequency(587.33f))
    }
}
