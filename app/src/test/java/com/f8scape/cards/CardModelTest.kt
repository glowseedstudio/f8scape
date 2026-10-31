package com.f8scape.cards

import org.junit.Assert.*
import org.junit.Test

class CardModelTest {

    @Test
    fun defaultDashboardLayout_containsExpectedCards() {
        val layout = DefaultCardLayouts.getDashboardLayout()
        assertEquals(3, layout.size)
        assertEquals(CardType.CLOCK, layout[0].type)
        assertEquals(CardType.MEDIA, layout[1].type)
        assertEquals(CardType.CALENDAR, layout[2].type)
    }

    @Test
    fun minimalLayout_containsOnlyClock() {
        val layout = DefaultCardLayouts.getMinimalLayout()
        assertEquals(1, layout.size)
        assertEquals(CardType.CLOCK, layout[0].type)
    }

    @Test
    fun serializationAndDeserialization_preservesCardOrder() {
        val original = listOf(
            CardItem(id = "clock_1", type = CardType.CLOCK),
            CardItem(id = "media_1", type = CardType.MEDIA),
            CardItem(id = "calendar_1", type = CardType.CALENDAR)
        )

        val serialized = DefaultCardLayouts.serialize(original)
        val deserialized = DefaultCardLayouts.deserialize(serialized)

        assertEquals(3, deserialized.size)
        assertEquals("clock_1", deserialized[0].id)
        assertEquals(CardType.CLOCK, deserialized[0].type)
        assertEquals("media_1", deserialized[1].id)
        assertEquals(CardType.MEDIA, deserialized[1].type)
        assertEquals("calendar_1", deserialized[2].id)
        assertEquals(CardType.CALENDAR, deserialized[2].type)
    }

    @Test
    fun deserializeEmptyString_returnsFallbackPreset() {
        val minimalFallback = DefaultCardLayouts.deserialize("", fallbackPreset = "Minimal")
        assertEquals(1, minimalFallback.size)
        assertEquals(CardType.CLOCK, minimalFallback[0].type)

        val dashboardFallback = DefaultCardLayouts.deserialize("", fallbackPreset = "Dashboard")
        assertEquals(3, dashboardFallback.size)

        val mediaFallback = DefaultCardLayouts.deserialize("", fallbackPreset = "Media")
        assertEquals(2, mediaFallback.size)
        assertEquals(CardType.CLOCK, mediaFallback[0].type)
        assertEquals(CardType.MEDIA, mediaFallback[1].type)
    }

    @Test
    fun getMediaLayout_containsClockAndMedia() {
        val layout = DefaultCardLayouts.getMediaLayout()
        assertEquals(2, layout.size)
        assertEquals(CardType.CLOCK, layout[0].type)
        assertEquals(CardType.MEDIA, layout[1].type)
    }

    @Test
    fun photoFrameLayout_containsClockCard() {
        val layout = DefaultCardLayouts.getPhotoFrameLayout()
        assertEquals(1, layout.size)
        assertEquals(CardType.CLOCK, layout[0].type)

        val photoFrameFallback = DefaultCardLayouts.deserialize("", fallbackPreset = "Photo Frame")
        assertEquals(1, photoFrameFallback.size)
        assertEquals(CardType.CLOCK, photoFrameFallback[0].type)
    }
}
