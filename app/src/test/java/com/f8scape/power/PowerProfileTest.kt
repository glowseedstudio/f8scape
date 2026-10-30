package com.f8scape.power

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerProfileTest {

    @Test
    fun `PowerProfile fromId resolves AMBIENT and ECO safely`() {
        assertEquals(PowerProfile.AMBIENT, PowerProfile.fromId("AMBIENT"))
        assertEquals(PowerProfile.ECO, PowerProfile.fromId("ECO"))
        assertEquals(PowerProfile.AMBIENT, PowerProfile.fromId("UNKNOWN"))
    }

    @Test
    fun `shouldEnforceEco returns true when user selects ECO`() {
        val state = SystemResilienceState(
            isBatterySaverActive = false,
            isThermalElevated = false
        )
        assertTrue(state.shouldEnforceEco(PowerProfile.ECO, autoEcoOnBatterySaver = false))
    }

    @Test
    fun `shouldEnforceEco respects autoEcoOnBatterySaver flag`() {
        val state = SystemResilienceState(
            isBatterySaverActive = true,
            isThermalElevated = false
        )
        assertTrue(state.shouldEnforceEco(PowerProfile.AMBIENT, autoEcoOnBatterySaver = true))
        assertFalse(state.shouldEnforceEco(PowerProfile.AMBIENT, autoEcoOnBatterySaver = false))
    }

    @Test
    fun `shouldEnforceEco always enforces eco when thermal status is elevated`() {
        val state = SystemResilienceState(
            isBatterySaverActive = false,
            isThermalElevated = true
        )
        assertTrue(state.shouldEnforceEco(PowerProfile.AMBIENT, autoEcoOnBatterySaver = false))
    }
}
