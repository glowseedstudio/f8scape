package com.f8scape.fold

import android.app.Activity
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Public AndroidX WindowManager implementation.
 * Provides reliable FLAT or HALF_OPENED geometry when the activity window is active.
 */
class WindowPostureSource(
    private val activity: Activity
) : PostureSource {
    override val name: String = "AndroidX WindowManager (WindowInfoTracker)"
    override val isAvailable: Boolean = true

    override fun observePosture(): Flow<PostureData> {
        val tracker = WindowInfoTracker.getOrCreate(activity)
        return tracker.windowLayoutInfo(activity).map { layoutInfo ->
            val foldingFeature = layoutInfo.displayFeatures
                .filterIsInstance<FoldingFeature>()
                .firstOrNull()

            val posture = when (foldingFeature?.state) {
                FoldingFeature.State.FLAT -> FoldPosture.FLAT
                FoldingFeature.State.HALF_OPENED -> FoldPosture.HALF_OPENED
                null -> FoldPosture.CLOSED // Or non-folding window
                else -> FoldPosture.UNKNOWN
            }

            PostureData(
                posture = posture,
                isCoverScreenActive = true, // Detected via display coordinator
                stabilityConfidence = 1.0f,
                hingeAngleDeg = null, // WindowManager does not expose exact continuous degrees
                source = name
            )
        }
    }
}
