package com.f8scape.fold

sealed class PostureEvent {
    data class HingeChanged(val angleDeg: Float, val timestampMs: Long) : PostureEvent()
    data class WindowFoldingFeatureChanged(val state: FoldPosture, val isSeparating: Boolean) : PostureEvent()
    data class AccelerometerSample(val x: Float, val y: Float, val z: Float, val timestampMs: Long) : PostureEvent()
    data class CoverDisplayStateChanged(val isCoverActive: Boolean) : PostureEvent()
    data class StabilityScoreUpdated(val confidence: Float) : PostureEvent()
    object DevicePickedUp : PostureEvent()
}
