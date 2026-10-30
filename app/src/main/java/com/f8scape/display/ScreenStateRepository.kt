package com.f8scape.display

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.view.Display
import com.f8scape.util.F8Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ScreenStateRepository(
    private val context: Context
) {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    fun isInteractive(): Boolean {
        return powerManager?.isInteractive ?: true
    }

    fun getDisplaysReport(currentDisplayId: Int): DisplaysReport {
        val displays = displayManager?.displays ?: emptyArray()
        val items = displays.map { display ->
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            display.getRealMetrics(metrics)

            val stateStr = when (display.state) {
                Display.STATE_ON -> "ON"
                Display.STATE_OFF -> "OFF"
                Display.STATE_DOZE -> "DOZE"
                Display.STATE_DOZE_SUSPEND -> "DOZE_SUSPEND"
                else -> "UNKNOWN(${display.state})"
            }

            // On Samsung foldables, the cover screen has narrower width/higher aspect ratio or smaller diagonal
            val isCover = (metrics.widthPixels < metrics.heightPixels && metrics.widthPixels < 1100) ||
                    (metrics.heightPixels < metrics.widthPixels && metrics.heightPixels < 1100)

            DisplayItemInfo(
                displayId = display.displayId,
                name = display.name ?: "Display #${display.displayId}",
                widthPx = metrics.widthPixels,
                heightPx = metrics.heightPixels,
                densityDpi = metrics.densityDpi,
                refreshRateHz = display.refreshRate,
                rotationDeg = display.rotation * 90,
                state = stateStr,
                isDefault = display.displayId == Display.DEFAULT_DISPLAY,
                isCoverCandidate = isCover
            )
        }

        return DisplaysReport(
            activeDisplayCount = items.size,
            currentActivityDisplayId = currentDisplayId,
            items = items
        )
    }

    fun observeDisplayChanges(): Flow<Unit> = callbackFlow {
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                F8Log.d("ScreenState", "Display added: id=$displayId")
                trySend(Unit)
            }

            override fun onDisplayRemoved(displayId: Int) {
                F8Log.d("ScreenState", "Display removed: id=$displayId")
                trySend(Unit)
            }

            override fun onDisplayChanged(displayId: Int) {
                F8Log.d("ScreenState", "Display changed: id=$displayId")
                trySend(Unit)
            }
        }

        displayManager?.registerDisplayListener(listener, null)
        trySend(Unit) // Emit initial

        awaitClose {
            displayManager?.unregisterDisplayListener(listener)
        }
    }
}
