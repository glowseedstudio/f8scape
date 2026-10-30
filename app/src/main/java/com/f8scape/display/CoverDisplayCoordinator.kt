package com.f8scape.display

import android.view.Display

class CoverDisplayCoordinator(
    private val screenStateRepository: ScreenStateRepository
) {
    /**
     * Determines whether the current active display is the outer/cover screen.
     * On standard Galaxy Z Fold 8, the passport cover screen has distinct dimensions
     * (wider aspect ratio than older tall/narrow Folds, but still distinct from the large square-ish inner main panel).
     */
    fun isCoverScreenActive(currentDisplayId: Int): Boolean {
        val report = screenStateRepository.getDisplaysReport(currentDisplayId)
        val currentDisplay = report.items.find { it.displayId == currentDisplayId }
            ?: return true

        // Check if current display matches cover characteristics
        return currentDisplay.isCoverCandidate
    }
}
