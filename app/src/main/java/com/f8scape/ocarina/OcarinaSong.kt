package com.f8scape.ocarina

import androidx.compose.ui.graphics.Color
import com.f8scape.R

enum class OcarinaButton(
    val label: String,
    val arrowText: String,
    val noteName: String,
    val frequency: Float,
    val isCButton: Boolean,
    val buttonColor: Color,
    val textColor: Color
) {
    A(
        label = "A",
        arrowText = "A",
        noteName = "D4",
        frequency = 293.665f,
        isCButton = false,
        buttonColor = Color(0xFF2979FF),
        textColor = Color.White
    ),
    C_UP(
        label = "C-Up",
        arrowText = "▲",
        noteName = "D5",
        frequency = 587.330f,
        isCButton = true,
        buttonColor = Color(0xFFFFD54F),
        textColor = Color(0xFF1B1B1D)
    ),
    C_DOWN(
        label = "C-Down",
        arrowText = "▼",
        noteName = "F4",
        frequency = 349.228f,
        isCButton = true,
        buttonColor = Color(0xFFFFD54F),
        textColor = Color(0xFF1B1B1D)
    ),
    C_LEFT(
        label = "C-Left",
        arrowText = "◀",
        noteName = "B4",
        frequency = 493.883f,
        isCButton = true,
        buttonColor = Color(0xFFFFD54F),
        textColor = Color(0xFF1B1B1D)
    ),
    C_RIGHT(
        label = "C-Right",
        arrowText = "▶",
        noteName = "A4",
        frequency = 440.000f,
        isCButton = true,
        buttonColor = Color(0xFFFFD54F),
        textColor = Color(0xFF1B1B1D)
    );

    companion object {
        fun fromFrequency(freq: Float): OcarinaButton? {
            return entries.minByOrNull { kotlin.math.abs(it.frequency - freq) }
                ?.takeIf { kotlin.math.abs(it.frequency - freq) < 15f }
        }
    }
}

data class OcarinaNote(
    val button: OcarinaButton?,
    val frequency: Float,
    val durationMs: Long,
    val isRest: Boolean = false
)

data class OcarinaSong(
    val id: String,
    val title: String,
    val subtitle: String,
    val lore: String,
    val buttonSequence: List<OcarinaButton>,
    val accentColor: Color,
    val category: String, // "Melody" or "Warp"
    val melodyNotes: List<OcarinaNote>
) {
    val iconRes: Int get() = OcarinaSongs.iconFor(id)
}

object OcarinaSongs {

    val ZELDAS_LULLABY = OcarinaSong(
        id = "zeldas_lullaby",
        title = "Zelda's Lullaby",
        subtitle = "Royal Family's Sacred Melody",
        lore = "Proves your connection to the Royal Family of Hyrule and triggers ancient gossip stones and royal seals.",
        buttonSequence = listOf(
            OcarinaButton.C_LEFT,
            OcarinaButton.C_UP,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_LEFT,
            OcarinaButton.C_UP,
            OcarinaButton.C_RIGHT
        ),
        accentColor = Color(0xFFB388FF), // Royal Purple
        category = "Melody",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 720L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 360L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 1080L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 720L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 360L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 1440L),
            // Continuation theme
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 720L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 360L),
            OcarinaNote(null, 880.00f, 720L), // A5
            OcarinaNote(null, 783.99f, 360L), // G5
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 720L),
            OcarinaNote(null, 523.25f, 360L), // C5
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 360L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 1600L)
        )
    )

    val EPONAS_SONG = OcarinaSong(
        id = "eponas_song",
        title = "Epona's Song",
        subtitle = "Lon Lon Ranch Melody",
        lore = "Taught by Malon; summons your faithful steed Epona from across the plains of Hyrule.",
        buttonSequence = listOf(
            OcarinaButton.C_UP,
            OcarinaButton.C_LEFT,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_UP,
            OcarinaButton.C_LEFT,
            OcarinaButton.C_RIGHT
        ),
        accentColor = Color(0xFFFFA726), // Amber
        category = "Melody",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 440L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 440L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 880L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 440L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 440L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 1320L),
            // Continuation
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 440L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 440L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 880L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 440L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 440L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 880L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 1400L)
        )
    )

    val SARIAS_SONG = OcarinaSong(
        id = "sarias_song",
        title = "Saria's Song",
        subtitle = "Lost Woods Kokiri Tune",
        lore = "A jaunty tune taught by childhood friend Saria. Lets you speak with her telepathically and cheers up Darunia.",
        buttonSequence = listOf(
            OcarinaButton.C_DOWN,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_LEFT,
            OcarinaButton.C_DOWN,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_LEFT
        ),
        accentColor = Color(0xFF4CAF50), // Kokiri Green
        category = "Melody",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 215L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 215L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 430L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 215L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 215L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 430L),
            // Lost Woods motif
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 215L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 215L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 215L),
            OcarinaNote(null, 659.25f, 215L), // E5
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 430L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 215L),
            OcarinaNote(null, 523.25f, 215L), // C5
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 215L),
            OcarinaNote(null, 392.00f, 215L), // G4
            OcarinaNote(null, 329.63f, 860L)  // E4
        )
    )

    val SUNS_SONG = OcarinaSong(
        id = "suns_song",
        title = "Sun's Song",
        subtitle = "Song of Sun and Moon",
        lore = "Carved upon the Royal Tomb; controls the heavenly bodies to instantly turn night into day or day into night.",
        buttonSequence = listOf(
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_DOWN,
            OcarinaButton.C_UP,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_DOWN,
            OcarinaButton.C_UP
        ),
        accentColor = Color(0xFFFFB300), // Sun Gold
        category = "Melody",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 260L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 260L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 780L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 260L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 260L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 1040L),
            // Dawn chime arpeggio
            OcarinaNote(OcarinaButton.A, 293.66f, 160L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 160L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 160L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 160L),
            OcarinaNote(null, 698.46f, 160L), // F5
            OcarinaNote(null, 880.00f, 900L)  // A5
        )
    )

    val SONG_OF_TIME = OcarinaSong(
        id = "song_of_time",
        title = "Song of Time",
        subtitle = "Temple of Time Secret Anthem",
        lore = "Passed down through the Hyrulean Royal Family; opens the Door of Time and manipulates time blocks.",
        buttonSequence = listOf(
            OcarinaButton.C_RIGHT,
            OcarinaButton.A,
            OcarinaButton.C_DOWN,
            OcarinaButton.C_RIGHT,
            OcarinaButton.A,
            OcarinaButton.C_DOWN
        ),
        accentColor = Color(0xFF29B6F6), // Temple Sky Blue
        category = "Melody",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 480L),
            OcarinaNote(OcarinaButton.A, 293.66f, 960L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 480L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 480L),
            OcarinaNote(OcarinaButton.A, 293.66f, 960L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 480L),
            // Continuation
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 240L),
            OcarinaNote(null, 523.25f, 240L), // C5
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 480L),
            OcarinaNote(null, 392.00f, 480L), // G4
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 240L),
            OcarinaNote(null, 392.00f, 240L), // G4
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 480L),
            OcarinaNote(OcarinaButton.A, 293.66f, 480L),
            OcarinaNote(null, 261.63f, 240L), // C4
            OcarinaNote(null, 329.63f, 240L), // E4
            OcarinaNote(OcarinaButton.A, 293.66f, 1400L)
        )
    )

    val SONG_OF_STORMS = OcarinaSong(
        id = "song_of_storms",
        title = "Song of Storms",
        subtitle = "Windmill Paradox Melody",
        lore = "A driving 3/4 waltz learned from the Windmill Man; summons pouring rain, lightning, and reveals hidden grottoes.",
        buttonSequence = listOf(
            OcarinaButton.A,
            OcarinaButton.C_DOWN,
            OcarinaButton.C_UP,
            OcarinaButton.A,
            OcarinaButton.C_DOWN,
            OcarinaButton.C_UP
        ),
        accentColor = Color(0xFF00E5FF), // Storm Cyan
        category = "Melody",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.A, 293.66f, 175L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 175L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 700L),
            OcarinaNote(OcarinaButton.A, 293.66f, 175L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 175L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 700L),
            // The glorious storm theme
            OcarinaNote(null, 659.25f, 525L), // E5
            OcarinaNote(null, 698.46f, 175L), // F5
            OcarinaNote(null, 659.25f, 175L), // E5
            OcarinaNote(null, 698.46f, 175L), // F5
            OcarinaNote(null, 659.25f, 175L), // E5
            OcarinaNote(null, 523.25f, 175L), // C5
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 700L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 350L),
            OcarinaNote(OcarinaButton.A, 293.66f, 350L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 175L),
            OcarinaNote(null, 392.00f, 175L), // G4
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 700L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 350L),
            OcarinaNote(OcarinaButton.A, 293.66f, 1200L)
        )
    )

    val MINUET_OF_FOREST = OcarinaSong(
        id = "minuet_of_forest",
        title = "Minuet of Forest",
        subtitle = "Warp to Forest Meadow",
        lore = "Taught by Sheik in the Sacred Forest Meadow; teleports Link to the entrance of the Forest Temple.",
        buttonSequence = listOf(
            OcarinaButton.A,
            OcarinaButton.C_UP,
            OcarinaButton.C_LEFT,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_LEFT,
            OcarinaButton.C_RIGHT
        ),
        accentColor = Color(0xFF81C784), // Meadow Green
        category = "Warp",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.A, 293.66f, 260L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 260L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 520L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 260L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 260L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 1200L)
        )
    )

    val BOLERO_OF_FIRE = OcarinaSong(
        id = "bolero_of_fire",
        title = "Bolero of Fire",
        subtitle = "Warp to Death Mountain Crater",
        lore = "Taught by Sheik above the fiery crater; teleports Link directly into the heart of Death Mountain.",
        buttonSequence = listOf(
            OcarinaButton.C_DOWN,
            OcarinaButton.A,
            OcarinaButton.C_DOWN,
            OcarinaButton.A,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_DOWN,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_DOWN
        ),
        accentColor = Color(0xFFFF5252), // Fire Red
        category = "Warp",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 280L),
            OcarinaNote(OcarinaButton.A, 293.66f, 280L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 280L),
            OcarinaNote(OcarinaButton.A, 293.66f, 280L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 280L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 280L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 280L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 1200L)
        )
    )

    val SERENADE_OF_WATER = OcarinaSong(
        id = "serenade_of_water",
        title = "Serenade of Water",
        subtitle = "Warp to Lake Hylia",
        lore = "Taught by Sheik inside the Ice Cavern; teleports Link atop the Water Temple in Lake Hylia.",
        buttonSequence = listOf(
            OcarinaButton.A,
            OcarinaButton.C_DOWN,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_LEFT
        ),
        accentColor = Color(0xFF00B0FF), // Water Azure
        category = "Warp",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.A, 293.66f, 440L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 440L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 660L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 220L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 1300L)
        )
    )

    val REQUIEM_OF_SPIRIT = OcarinaSong(
        id = "requiem_of_spirit",
        title = "Requiem of Spirit",
        subtitle = "Warp to Desert Colossus",
        lore = "Taught by Sheik in the Desert Colossus; teleports Link across the Haunted Wasteland to the Spirit Temple.",
        buttonSequence = listOf(
            OcarinaButton.A,
            OcarinaButton.C_DOWN,
            OcarinaButton.A,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_DOWN,
            OcarinaButton.A
        ),
        accentColor = Color(0xFFFF9100), // Spirit Amber
        category = "Warp",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.A, 293.66f, 440L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 220L),
            OcarinaNote(OcarinaButton.A, 293.66f, 440L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 440L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 440L),
            OcarinaNote(OcarinaButton.A, 293.66f, 1300L)
        )
    )

    val NOCTURNE_OF_SHADOW = OcarinaSong(
        id = "nocturne_of_shadow",
        title = "Nocturne of Shadow",
        subtitle = "Warp to Shadow Graveyard",
        lore = "Taught by Sheik amidst burning Kakariko; teleports Link to the high ledge above the Graveyard.",
        buttonSequence = listOf(
            OcarinaButton.C_LEFT,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_RIGHT,
            OcarinaButton.A,
            OcarinaButton.C_LEFT,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_DOWN
        ),
        accentColor = Color(0xFFBA68C8), // Shadow Orchid
        category = "Warp",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 380L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 380L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 190L),
            OcarinaNote(OcarinaButton.A, 293.66f, 190L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 380L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 380L),
            OcarinaNote(OcarinaButton.C_DOWN, 349.23f, 1300L)
        )
    )

    val PRELUDE_OF_LIGHT = OcarinaSong(
        id = "prelude_of_light",
        title = "Prelude of Light",
        subtitle = "Warp to Temple of Time",
        lore = "Taught by Sheik in the Temple of Time; teleports Link to the pedestal of the Master Sword.",
        buttonSequence = listOf(
            OcarinaButton.C_UP,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_UP,
            OcarinaButton.C_RIGHT,
            OcarinaButton.C_LEFT,
            OcarinaButton.C_UP
        ),
        accentColor = Color(0xFFFFF176), // Light Gold
        category = "Warp",
        melodyNotes = listOf(
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 220L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 440L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 220L),
            OcarinaNote(OcarinaButton.C_RIGHT, 440.00f, 440L),
            OcarinaNote(OcarinaButton.C_LEFT, 493.88f, 220L),
            OcarinaNote(OcarinaButton.C_UP, 587.33f, 1200L)
        )
    )

    // =========================================================================
    // Hyrule Jukebox Tracks (6 Iconic Full Game Soundtrack Themes - 2 Rows of 3)
    // =========================================================================

    val JUKEBOX_TITLE_THEME = OcarinaSong(
        id = "jukebox_title_theme",
        title = "Title Theme",
        subtitle = "Dawn Over Hyrule",
        lore = "The timeless piano and ocarina prelude as Link and Epona ride across Hyrule Field at dawn.",
        buttonSequence = emptyList(),
        accentColor = Color(0xFFFFD54F),
        category = "Jukebox",
        melodyNotes = emptyList()
    )

    val JUKEBOX_HYRULE_FIELD = OcarinaSong(
        id = "jukebox_hyrule_field",
        title = "Hyrule Field",
        subtitle = "Main Overworld Theme",
        lore = "Koji Kondo's sweeping orchestral anthem accompanying Link's grand journey across the plains of Hyrule.",
        buttonSequence = emptyList(),
        accentColor = Color(0xFF66BB6A),
        category = "Jukebox",
        melodyNotes = emptyList()
    )

    val JUKEBOX_GERUDO_VALLEY = OcarinaSong(
        id = "jukebox_gerudo_valley",
        title = "Gerudo Valley",
        subtitle = "Desert Canyon Fortress",
        lore = "Fiery flamenco guitars and driving rhythms echoing across the canyon bridge to Gerudo's Fortress.",
        buttonSequence = emptyList(),
        accentColor = Color(0xFFFF7043),
        category = "Jukebox",
        melodyNotes = emptyList()
    )

    val JUKEBOX_LOST_WOODS = OcarinaSong(
        id = "jukebox_lost_woods",
        title = "Lost Woods",
        subtitle = "Sacred Kokiri Forest",
        lore = "The enchanting, spirited woodland melody guiding travelers through the mist of the Lost Woods.",
        buttonSequence = emptyList(),
        accentColor = Color(0xFF4CAF50),
        category = "Jukebox",
        melodyNotes = emptyList()
    )

    val JUKEBOX_KAKARIKO_VILLAGE = OcarinaSong(
        id = "jukebox_kakariko_village",
        title = "Kakariko Village",
        subtitle = "Peaceful Hylian Hamlet",
        lore = "Serene harmonica and gentle acoustic strings beneath the shadow of Death Mountain and the spinning windmill.",
        buttonSequence = emptyList(),
        accentColor = Color(0xFFFFB74D),
        category = "Jukebox",
        melodyNotes = emptyList()
    )

    val JUKEBOX_ZORAS_DOMAIN = OcarinaSong(
        id = "jukebox_zoras_domain",
        title = "Zora's Domain",
        subtitle = "Sacred Waterfall Realm",
        lore = "Tranquil, crystalline tropical harmonies reverberating through the shimmering caverns of King Zora's realm.",
        buttonSequence = emptyList(),
        accentColor = Color(0xFF29B6F6),
        category = "Jukebox",
        melodyNotes = emptyList()
    )

    val allSongs: List<OcarinaSong> = listOf(
        ZELDAS_LULLABY,
        EPONAS_SONG,
        SARIAS_SONG,
        SUNS_SONG,
        SONG_OF_TIME,
        SONG_OF_STORMS,
        MINUET_OF_FOREST,
        BOLERO_OF_FIRE,
        SERENADE_OF_WATER,
        REQUIEM_OF_SPIRIT,
        NOCTURNE_OF_SHADOW,
        PRELUDE_OF_LIGHT
    )

    val jukeboxTracks: List<OcarinaSong> = listOf(
        JUKEBOX_TITLE_THEME,
        JUKEBOX_HYRULE_FIELD,
        JUKEBOX_GERUDO_VALLEY,
        JUKEBOX_LOST_WOODS,
        JUKEBOX_KAKARIKO_VILLAGE,
        JUKEBOX_ZORAS_DOMAIN
    )

    fun findById(id: String): OcarinaSong? =
        allSongs.firstOrNull { it.id == id } ?: jukeboxTracks.firstOrNull { it.id == id }

    fun iconFor(songId: String): Int = when (songId) {
        "zeldas_lullaby" -> R.drawable.ic_ocarina_zeldas_lullaby
        "eponas_song" -> R.drawable.ic_ocarina_eponas_song
        "sarias_song" -> R.drawable.ic_ocarina_sarias_song
        "suns_song" -> R.drawable.ic_ocarina_suns_song
        "song_of_time" -> R.drawable.ic_ocarina_song_of_time
        "song_of_storms" -> R.drawable.ic_ocarina_song_of_storms
        "minuet_of_forest" -> R.drawable.ic_ocarina_forest_medallion
        "bolero_of_fire" -> R.drawable.ic_ocarina_fire_medallion
        "serenade_of_water" -> R.drawable.ic_ocarina_water_medallion
        "requiem_of_spirit" -> R.drawable.ic_ocarina_spirit_medallion
        "nocturne_of_shadow" -> R.drawable.ic_ocarina_shadow_medallion
        "prelude_of_light" -> R.drawable.ic_ocarina_light_medallion
        "jukebox_title_theme" -> R.drawable.ic_jukebox_title_theme
        "jukebox_hyrule_field" -> R.drawable.ic_jukebox_hyrule_field
        "jukebox_gerudo_valley" -> R.drawable.ic_jukebox_gerudo_valley
        "jukebox_lost_woods" -> R.drawable.ic_jukebox_lost_woods
        "jukebox_kakariko_village" -> R.drawable.ic_jukebox_kakariko_village
        "jukebox_zoras_domain" -> R.drawable.ic_jukebox_zoras_domain
        else -> 0
    }

    /**
     * Returns the 3 authentic in-game Ocarina of Time scene screenshots for each Hyrule Jukebox track.
     */
    fun sceneScreenshotsFor(songId: String): List<Int> = when (songId) {
        "jukebox_title_theme" -> listOf(
            R.drawable.bg_jukebox_title_theme_1,
            R.drawable.bg_jukebox_title_theme_2,
            R.drawable.bg_jukebox_title_theme_3
        )
        "jukebox_hyrule_field" -> listOf(
            R.drawable.bg_jukebox_hyrule_field_1,
            R.drawable.bg_jukebox_hyrule_field_2,
            R.drawable.bg_jukebox_hyrule_field_3
        )
        "jukebox_gerudo_valley" -> listOf(
            R.drawable.bg_jukebox_gerudo_valley_1,
            R.drawable.bg_jukebox_gerudo_valley_2,
            R.drawable.bg_jukebox_gerudo_valley_3
        )
        "jukebox_lost_woods" -> listOf(
            R.drawable.bg_jukebox_lost_woods_1,
            R.drawable.bg_jukebox_lost_woods_2,
            R.drawable.bg_jukebox_lost_woods_3
        )
        "jukebox_kakariko_village" -> listOf(
            R.drawable.bg_jukebox_kakariko_village_1,
            R.drawable.bg_jukebox_kakariko_village_2,
            R.drawable.bg_jukebox_kakariko_village_3
        )
        "jukebox_zoras_domain" -> listOf(
            R.drawable.bg_jukebox_zoras_domain_1,
            R.drawable.bg_jukebox_zoras_domain_2,
            R.drawable.bg_jukebox_zoras_domain_3
        )
        else -> emptyList()
    }
}
