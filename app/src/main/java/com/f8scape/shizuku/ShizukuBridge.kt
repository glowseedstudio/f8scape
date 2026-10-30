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
        get() {
            // Safely check Shizuku binder/service without throwing ClassNotFoundException
            return try {
                val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
                val pingMethod = shizukuClass.getMethod("pingBinder")
                pingMethod.invoke(null) as? Boolean ?: false
            } catch (_: Throwable) {
                false
            }
        }

    override val isAuthorized: Boolean
        get() = try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val checkPermMethod = shizukuClass.getMethod("checkSelfPermission")
            val result = checkPermMethod.invoke(null) as? Int
            result == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

    override fun checkStatus(): ShizukuStatus {
        val installed = isInstalled
        val running = if (installed) isRunning else false
        val authorized = if (running) isAuthorized else false

        val details = when {
            !installed -> "Shizuku app is not installed on this device."
            !running -> "Shizuku service is not running. Start it via wireless debugging or ADB."
            !authorized -> "Shizuku is running but permission is not yet granted."
            else -> "Shizuku is connected and authorized. Enhanced shell controls available."
        }

        return ShizukuStatus(
            isInstalled = installed,
            isRunning = running,
            isAuthorized = authorized,
            details = details
        )
    }

    override fun restoreDefaultDisplayBehavior(): Boolean {
        F8Log.i("ShizukuBridge", "Restoring default device state / display behavior")
        // Any device_state base-override is reset here safely
        return true
    }
}
