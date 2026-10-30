package com.f8scape.cards

import org.junit.Assert.*
import org.junit.Test

class CardModelTest {

    @Test
    fun defaultDashboardLayout_containsExpectedCards() {
        val layout = DefaultCardLayouts.getDashboardLayout()
        assertEquals(4, layout.size)
        assertEquals(CardType.CLOCK, layout[0].type)
        assertEquals(CardType.WEATHER, layout[1].type)
        assertEquals(CardType.BATTERY, layout[2].type)
        assertEquals(CardType.CALENDAR, layout[3].type)
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
            CardItem(id = "weather_1", type = CardType.WEATHER),
            CardItem(id = "clock_1", type = CardType.CLOCK),
            CardItem(id = "battery_1", type = CardType.BATTERY)
        )

        val serialized = DefaultCardLayouts.serialize(original)
        val deserialized = DefaultCardLayouts.deserialize(serialized)

        assertEquals(3, deserialized.size)
        assertEquals("weather_1", deserialized[0].id)
        assertEquals(CardType.WEATHER, deserialized[0].type)
        assertEquals("clock_1", deserialized[1].id)
        assertEquals(CardType.CLOCK, deserialized[1].type)
        assertEquals("battery_1", deserialized[2].id)
        assertEquals(CardType.BATTERY, deserialized[2].type)
    }

    @Test
    fun deserializeEmptyString_returnsFallbackPreset() {
        val minimalFallback = DefaultCardLayouts.deserialize("", fallbackPreset = "Minimal")
        assertEquals(1, minimalFallback.size)
        assertEquals(CardType.CLOCK, minimalFallback[0].type)

        val dashboardFallback = DefaultCardLayouts.deserialize("", fallbackPreset = "Dashboard")
        assertEquals(4, dashboardFallback.size)
    }
}
