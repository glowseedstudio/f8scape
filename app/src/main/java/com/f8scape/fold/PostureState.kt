package com.f8scape.fold

/**
 * Representation of foldable device hinge and posture states.
 */
enum class FoldPosture {
    UNKNOWN,
    CLOSED,
    TENT_CANDIDATE,
    HALF_OPENED,
    FLAT
}

data class PostureData(
    val posture: FoldPosture,
    val isCoverScreenActive: Boolean,
    val stabilityConfidence: Float, // 0.0 to 1.0 (how still the phone is on a table/surface)
    val hingeAngleDeg: Float? = null,
    val isPortrait: Boolean = false,
    val source: String,
    val timestampMs: Long = System.currentTimeMillis()
)
