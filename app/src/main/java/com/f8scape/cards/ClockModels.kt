package com.f8scape.cards

import androidx.compose.ui.graphics.Color

/**
 * Available flagship clock face styles for F8scape StandBy.
 */
enum class ClockStyle(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    ONE_UI_MODERN("ONE_UI_MODERN", "One UI Modern", "Crisp flagship typographic clock"),
    RETRO_DIGITAL("RETRO_DIGITAL", "Retro LED", "Old-school 7-segment digital display"),
    CHRONO_ANALOG("CHRONO_ANALOG", "Horology Analog", "3, 6, 9, 12 dial with 60s ticker lines"),
    FLIP_CLOCK("FLIP_CLOCK", "Mechanical Flip", "Classic split-flap desk clock"),
    STACKED_BOLD("STACKED_BOLD", "Stacked Bold", "Architectural dual-row numerals"),
    NEON_STENCIL("NEON_STENCIL", "Neon Outline", "Precision stencil with live seconds");

    companion object {
        fun fromId(id: String): ClockStyle =
            entries.firstOrNull { it.id == id } ?: ONE_UI_MODERN
    }
}

/**
 * Curated clock color palette including classic/vibrant tones, old-school LED red/amber,
 * and soft pastel shades.
 */
enum class ClockColorOption(
    val id: String,
    val displayName: String,
    val color: Color,
    val isPastel: Boolean = false
) {
    // Classic & Vibrant
    PURE_WHITE("PURE_WHITE", "Pure White", Color(0xFFFFFFFF), false),
    CRIMSON_RED("CRIMSON_RED", "Retro LED Red", Color(0xFFFF2424), false),
    ONE_UI_BLUE("ONE_UI_BLUE", "Signature Blue", Color(0xFF2E75D3), false),
    AMBER_GOLD("AMBER_GOLD", "Nixie Amber", Color(0xFFFFAB00), false),
    EMERALD_GREEN("EMERALD_GREEN", "Phosphor Green", Color(0xFF00E676), false),
    CYBER_CYAN("CYBER_CYAN", "Electric Cyan", Color(0xFF00E5FF), false),

    // Soft Pastels
    PASTEL_ROSE("PASTEL_ROSE", "Sakura Rose", Color(0xFFFFB3C6), true),
    PASTEL_LAVENDER("PASTEL_LAVENDER", "Soft Lavender", Color(0xFFD8B4F8), true),
    PASTEL_MINT("PASTEL_MINT", "Matcha Mint", Color(0xFFA8E6CF), true),
    PASTEL_PEACH("PASTEL_PEACH", "Sunset Peach", Color(0xFFFFD3B6), true),
    PASTEL_SKY("PASTEL_SKY", "Powder Blue", Color(0xFFA0C4FF), true),
    PASTEL_CREAM("PASTEL_CREAM", "Butter Cream", Color(0xFFFDFFB6), true);

    companion object {
        fun fromId(id: String): ClockColorOption =
            entries.firstOrNull { it.id == id } ?: PURE_WHITE
    }
}
