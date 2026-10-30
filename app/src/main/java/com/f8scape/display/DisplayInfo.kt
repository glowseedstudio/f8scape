package com.f8scape.display

data class DisplayItemInfo(
    val displayId: Int,
    val name: String,
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val refreshRateHz: Float,
    val rotationDeg: Int,
    val state: String, // ON, OFF, DOZE, etc.
    val isDefault: Boolean,
    val isCoverCandidate: Boolean
)

data class DisplaysReport(
    val activeDisplayCount: Int,
    val currentActivityDisplayId: Int,
    val items: List<DisplayItemInfo>
)
