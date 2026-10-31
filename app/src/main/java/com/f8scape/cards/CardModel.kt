package com.f8scape.cards

/**
 * Supported native ambient cards in F8scape.
 */
enum class CardType(val displayName: String, val defaultTitle: String) {
    CLOCK("Clock & Date", "Clock"),
    WEATHER("Weather Conditions", "Weather"),
    BATTERY("Battery & Charging", "Battery"),
    CALENDAR("Upcoming Events", "Calendar"),
    MEDIA("Now Playing", "Media"),
    SPACER("Ambient Spacer", "Spacer")
}

/**
 * Representation of an ambient card instance in the layout grid.
 */
data class CardItem(
    val id: String,
    val type: CardType,
    val isVisible: Boolean = true
)

object DefaultCardLayouts {
    fun getMinimalLayout(): List<CardItem> = listOf(
        CardItem(id = "clock_main", type = CardType.CLOCK)
    )

    fun getDashboardLayout(): List<CardItem> = listOf(
        CardItem(id = "clock_main", type = CardType.CLOCK),
        CardItem(id = "media_card", type = CardType.MEDIA),
        CardItem(id = "calendar_card", type = CardType.CALENDAR)
    )

    fun getMediaLayout(): List<CardItem> = listOf(
        CardItem(id = "clock_main", type = CardType.CLOCK),
        CardItem(id = "media_card", type = CardType.MEDIA)
    )

    fun getPhotoFrameLayout(): List<CardItem> = listOf(
        CardItem(id = "clock_main", type = CardType.CLOCK)
    )

    fun serialize(items: List<CardItem>): String {
        return items.filter { it.isVisible }.joinToString(",") { "${it.id}:${it.type.name}" }
    }

    fun deserialize(serialized: String, fallbackPreset: String = "Dashboard"): List<CardItem> {
        if (serialized.isBlank()) {
            return when (fallbackPreset) {
                "Minimal" -> getMinimalLayout()
                "Media" -> getMediaLayout()
                "Photo Frame" -> getPhotoFrameLayout()
                else -> getDashboardLayout()
            }
        }
        return try {
            val parsed = serialized.split(",")
                .filter { it.isNotBlank() }
                .map { segment ->
                    val parts = segment.split(":")
                    val id = parts[0]
                    val type = CardType.valueOf(parts[1])
                    CardItem(id = id, type = type)
                }
            // Weather and battery are embedded directly into the Clock hero unit.
            // Filter out standalone legacy cards so the display is clean and unified.
            val filtered = parsed.filter { it.type != CardType.WEATHER && it.type != CardType.BATTERY }
            filtered.ifEmpty { getDashboardLayout() }
        } catch (_: Exception) {
            getDashboardLayout()
        }
    }
}
