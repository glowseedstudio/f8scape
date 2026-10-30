package com.f8scape.automation

import org.junit.Assert.*
import org.junit.Test

class TriggerPolicyTest {

    @Test
    fun policyDisabled_cannotActivate() {
        val policy = TriggerPolicy(isAutoStandbyEnabled = false)
        assertFalse(policy.canActivate(isCharging = true, batteryPercent = 100, isCallActive = false))
    }

    @Test
    fun onlyWhileCharging_rejectsWhenUnplugged() {
        val policy = TriggerPolicy(onlyWhileCharging = true)
        assertFalse(policy.canActivate(isCharging = false, batteryPercent = 90, isCallActive = false))
        assertTrue(policy.canActivate(isCharging = true, batteryPercent = 90, isCallActive = false))
    }

    @Test
    fun lowBattery_rejectsWhenUnplugged() {
        val policy = TriggerPolicy(minBatteryPercent = 20)
        assertFalse(policy.canActivate(isCharging = false, batteryPercent = 15, isCallActive = false))
        // If charging, allows even if below minBatteryPercent
        assertTrue(policy.canActivate(isCharging = true, batteryPercent = 15, isCallActive = false))
    }

    @Test
    fun activePhoneCall_suppressesActivation() {
        val policy = TriggerPolicy(suppressDuringCalls = true)
        assertFalse(policy.canActivate(isCharging = true, batteryPercent = 100, isCallActive = true))
    }
}
