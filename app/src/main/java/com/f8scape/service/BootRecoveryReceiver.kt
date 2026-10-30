package com.f8scape.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.f8scape.F8scapeApplication
import com.f8scape.shizuku.SafeShizukuBridge
import com.f8scape.util.F8Log

/**
 * Ensures clean state recovery after device boot or app package update:
 * - Resets any stale experimental display state overrides safely
 * - Removes any orphaned overlay views
 * - Ensures StandbyAutomationCoordinator is running
 */
class BootRecoveryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            F8Log.i("BootRecoveryReceiver", "Received $action — verifying clean StandBy state")

            // 1. Clean up any stale display override safely
            try {
                SafeShizukuBridge(context).restoreDefaultDisplayBehavior()
            } catch (e: Exception) {
                F8Log.e("BootRecoveryReceiver", "Error restoring display defaults on boot", e)
            }

            // 2. Ensure no orphaned overlay is lingering
            if (CoverOverlayService.isServiceRunning) {
                CoverOverlayService.activeInstance?.removeStandbyOverlay()
            }

            // 3. Ensure application automation coordinator is active
            val app = context.applicationContext as? F8scapeApplication
            app?.automationCoordinator?.start()
        }
    }
}
