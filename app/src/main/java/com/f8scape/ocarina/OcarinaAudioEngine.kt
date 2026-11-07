package com.f8scape.ocarina

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.f8scape.R
import com.f8scape.util.F8Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Authentic Ocarina Audio Engine:
 * Uses real acoustic sound samples extracted directly from The Legend of Zelda: Ocarina of Time.
 * Plays genuine ocarina notes via low-latency Android SoundPool for button taps and game mic prompts,
 * and authentic full game recordings via MediaPlayer for the full melody mode.
 */
class OcarinaAudioEngine(private val context: Context? = null) {

    companion object {
        private const val TAG = "OcarinaAudioEngine"
    }

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val playbackLock = Any()
    private var currentPlayGeneration = 0L
    private var playbackJob: Job? = null
    private var activeMediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSongId = MutableStateFlow<String?>(null)
    val currentSongId: StateFlow<String?> = _currentSongId.asStateFlow()

    private val _currentNoteIndex = MutableStateFlow(-1)
    val currentNoteIndex: StateFlow<Int> = _currentNoteIndex.asStateFlow()

    private val _isMicInputMode = MutableStateFlow(true)
    val isMicInputMode: StateFlow<Boolean> = _isMicInputMode.asStateFlow()

    fun setMicInputMode(enabled: Boolean) {
        _isMicInputMode.value = enabled
    }

    // Android SoundPool with real sampled ocarina notes
    private var soundPool: SoundPool? = null
    private val noteSoundIds = mutableMapOf<OcarinaButton, Int>()
    private var secretChimeSoundId: Int = 0

    init {
        if (context != null) {
            try {
                val pool = SoundPool.Builder()
                    .setMaxStreams(8)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .build()
                soundPool = pool

                noteSoundIds[OcarinaButton.A] = pool.load(context, R.raw.ocarina_note_d4, 1)
                noteSoundIds[OcarinaButton.C_DOWN] = pool.load(context, R.raw.ocarina_note_f4, 1)
                noteSoundIds[OcarinaButton.C_RIGHT] = pool.load(context, R.raw.ocarina_note_a4, 1)
                noteSoundIds[OcarinaButton.C_LEFT] = pool.load(context, R.raw.ocarina_note_b4, 1)
                noteSoundIds[OcarinaButton.C_UP] = pool.load(context, R.raw.ocarina_note_d5, 1)
                secretChimeSoundId = pool.load(context, R.raw.ocarina_song_correct, 1)
            } catch (e: Exception) {
                F8Log.e(TAG, "Error loading real ocarina sound samples: ${e.message}")
            }
        }
    }

    /**
     * Plays the genuine acoustic Ocarina note sampled from the game.
     */
    fun playButtonNote(button: OcarinaButton) {
        val soundId = noteSoundIds[button] ?: return
        soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    /**
     * Plays the celebratory secret solved jingle.
     */
    fun playSecretSolvedChime() {
        if (secretChimeSoundId != 0) {
            soundPool?.play(secretChimeSoundId, 1.0f, 1.0f, 2, 0, 1.0f)
        }
    }

    /**
     * Plays a song.
     * In Game Mic Mode: Plays the exact prompt notes with real game ocarina samples,
     * calibrated with ~430ms note length and 90ms clear separation for controller mic recognition.
     * In Full Melody Mode: Plays the full authentic game audio track!
     *
     * Protected by generation tracking and synchronization lock so rapid button taps
     * can NEVER play audio tracks over the top of each other.
     */
    fun playSong(song: OcarinaSong) {
        val generation: Long
        synchronized(playbackLock) {
            currentPlayGeneration++
            generation = currentPlayGeneration

            // Cancel any in-flight playback job
            playbackJob?.cancel()
            playbackJob = null

            // Immediately halt and release previous MediaPlayer
            try {
                activeMediaPlayer?.stop()
                activeMediaPlayer?.release()
            } catch (_: Exception) {}
            activeMediaPlayer = null

            // Stop any active SoundPool note audio
            try {
                soundPool?.autoPause()
            } catch (_: Exception) {}
        }

        playbackJob = engineScope.launch {
            _isPlaying.value = true
            _currentSongId.value = song.id
            _currentNoteIndex.value = 0

            try {
                if (_isMicInputMode.value && song.category != "Jukebox") {
                    // Game Mic Mode: play the real game ocarina notes in sequence
                    for ((index, button) in song.buttonSequence.withIndex()) {
                        if (!isActive || generation != currentPlayGeneration) break
                        _currentNoteIndex.value = index

                        val soundId = noteSoundIds[button]
                        if (soundId != null && soundId != 0 && isActive && generation == currentPlayGeneration) {
                            soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                        }

                        // Authentic in-game tempo calibrated for Switch 2 controller mic recognition
                        delay(620L)
                        delay(140L)
                    }
                    delay(400L)
                } else {
                    // Full Melody Mode or Hyrule Jukebox OST Mode: check if dedicated track is bundled
                    val rawResId = getSongRawResId(song.id)
                    if (rawResId != null) {
                        playFullTrack(rawResId, song.buttonSequence.size.coerceAtLeast(6), generation)
                    } else {
                        // Warp melodies: play note-by-note with real samples
                        for ((index, note) in song.melodyNotes.withIndex()) {
                            if (!isActive || generation != currentPlayGeneration) break
                            _currentNoteIndex.value = index

                            if (note.button != null && isActive && generation == currentPlayGeneration) {
                                val soundId = noteSoundIds[note.button]
                                if (soundId != null && soundId != 0) {
                                    soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                                }
                            }

                            delay(note.durationMs)
                            delay(20L)
                        }
                        delay(500L)
                    }
                }
            } catch (_: CancellationException) {
                // Cancelled
            } catch (e: Exception) {
                F8Log.e(TAG, "Error playing song ${song.title}: ${e.message}")
            } finally {
                // Only clean up UI state if this coroutine is STILL the active generation
                synchronized(playbackLock) {
                    if (generation == currentPlayGeneration) {
                        stopPlaybackInternal()
                    }
                }
            }
        }
    }

    private suspend fun playFullTrack(resId: Int, numNotes: Int, generation: Long) {
        var mp: MediaPlayer? = null
        try {
            if (!coroutineContext.isActive || generation != currentPlayGeneration) return

            mp = MediaPlayer.create(context, resId) ?: return

            synchronized(playbackLock) {
                // If a newer song was tapped while create() was decoding, release and abort immediately
                if (!coroutineContext.isActive || generation != currentPlayGeneration) {
                    try { mp.release() } catch (_: Exception) {}
                    return
                }
                activeMediaPlayer = mp
            }

            mp.isLooping = false
            mp.start()

            // Advance note index across playback duration
            val durationMs = mp.duration.toLong().coerceAtLeast(1000L)
            val stepMs = (durationMs / (numNotes + 1)).coerceAtLeast(200L)

            for (i in 0 until numNotes) {
                if (!coroutineContext.isActive || generation != currentPlayGeneration) break
                _currentNoteIndex.value = i
                delay(stepMs)
            }

            // Wait until song completes or is cancelled/superseded
            while (coroutineContext.isActive && generation == currentPlayGeneration && mp.isPlaying) {
                delay(100L)
            }
        } catch (_: CancellationException) {
            // Cancelled
        } catch (e: Exception) {
            F8Log.e(TAG, "MediaPlayer error: ${e.message}")
        } finally {
            synchronized(playbackLock) {
                try {
                    mp?.stop()
                    mp?.release()
                } catch (_: Exception) {}
                if (activeMediaPlayer == mp) {
                    activeMediaPlayer = null
                }
            }
        }
    }

    private fun getSongRawResId(songId: String): Int? {
        return when (songId) {
            "zeldas_lullaby" -> R.raw.ocarina_song_zeldas_lullaby
            "eponas_song" -> R.raw.ocarina_song_eponas_song
            "sarias_song" -> R.raw.ocarina_song_sarias_song
            "suns_song" -> R.raw.ocarina_song_suns_song
            "song_of_time" -> R.raw.ocarina_song_song_of_time
            "song_of_storms" -> R.raw.ocarina_song_song_of_storms
            "minuet_of_forest" -> R.raw.ocarina_song_minuet_of_forest
            "bolero_of_fire" -> R.raw.ocarina_song_bolero_of_fire
            "serenade_of_water" -> R.raw.ocarina_song_serenade_of_water
            "requiem_of_spirit" -> R.raw.ocarina_song_requiem_of_spirit
            "nocturne_of_shadow" -> R.raw.ocarina_song_nocturne_of_shadow
            "prelude_of_light" -> R.raw.ocarina_song_prelude_of_light
            "jukebox_title_theme" -> R.raw.jukebox_title_theme
            "jukebox_hyrule_field" -> R.raw.jukebox_hyrule_field
            "jukebox_gerudo_valley" -> R.raw.jukebox_gerudo_valley
            "jukebox_lost_woods" -> R.raw.jukebox_lost_woods
            "jukebox_kakariko_village" -> R.raw.jukebox_kakariko_village
            "jukebox_zoras_domain" -> R.raw.jukebox_zoras_domain
            else -> null
        }
    }

    fun stopPlayback() {
        synchronized(playbackLock) {
            currentPlayGeneration++
            playbackJob?.cancel()
            playbackJob = null
            stopPlaybackInternal()
            try {
                soundPool?.autoPause()
            } catch (_: Exception) {}
        }
    }

    private fun stopPlaybackInternal() {
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
        } catch (ignored: Exception) {}
        activeMediaPlayer = null

        _isPlaying.value = false
        _currentSongId.value = null
        _currentNoteIndex.value = -1
    }

    fun release() {
        stopPlayback()
        engineScope.cancel()
        try {
            soundPool?.release()
        } catch (ignored: Exception) {}
        soundPool = null
    }
}
