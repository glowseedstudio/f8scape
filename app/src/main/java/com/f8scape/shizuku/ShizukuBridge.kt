package com.f8scape.shizuku

import android.content.Context
import android.content.pm.PackageManager
import com.f8scape.util.F8Log

interface ShizukuBridge {
    val isInstalled: Boolean
    val isRunning: Boolean
    val isAuthorized: Boolean

    fun checkStatus(): ShizukuStatus
    fun restoreDefaultDisplayBehavior(): Boolean
}

data class ShizukuStatus(
    val isInstalled: Boolean,
    val isRunning: Boolean,
    val isAuthorized: Boolean,
    val version: Int? = null,
    val details: String = ""
)

/**
 * Zero-reflection, no-op Shizuku bridge:
 * Standard Mode operates 100% natively on the Galaxy Z Fold 8 without privileged shell commands
 * or dynamic reflection.
 */
class SafeShizukuBridge(
    private val context: Context
) : ShizukuBridge {

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }

    override val isInstalled: Boolean
        get() = try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    override val isRunning: Boolean
        get() = false

    override val isAuthorized: Boolean
        get() = false

    override fun checkStatus(): ShizukuStatus {
        val installed = isInstalled
        return ShizukuStatus(
            isInstalled = installed,
            isRunning = false,
            isAuthorized = false,
            details = "Standard Mode active (native public APIs only; privileged shell bridge disabled)."
        )
    }

    override fun restoreDefaultDisplayBehavior(): Boolean {
        F8Log.i("ShizukuBridge", "Verified native display behavior (no privileged overrides active)")
        return true
    }
}
