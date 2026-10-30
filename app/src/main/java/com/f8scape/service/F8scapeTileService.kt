package com.f8scape.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.f8scape.data.PreferencesRepository
import com.f8scape.util.F8Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android Quick Settings Tile allowing instant enabling/disabling of F8scape StandBy automation.
 */
@RequiresApi(Build.VERSION_CODES.N)
class F8scapeTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferencesRepo: PreferencesRepository

    override fun onCreate() {
        super.onCreate()
        preferencesRepo = PreferencesRepository(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            try {
                val current = preferencesRepo.userPreferencesFlow.first().isAutoStandbyEnabled
                val target = !current
                preferencesRepo.setAutoStandbyEnabled(target)
                if (!target) {
                    CoverOverlayService.activeInstance?.removeStandbyOverlay()
                }
                updateTile(target)
                F8Log.i("F8scapeTileService", "Toggled StandBy via Quick Settings Tile: $target")
            } catch (e: Exception) {
                F8Log.e("F8scapeTileService", "Failed to toggle Quick Settings Tile", e)
            }
        }
    }

    private fun updateTileState() {
        serviceScope.launch {
            try {
                val enabled = preferencesRepo.userPreferencesFlow.first().isAutoStandbyEnabled
                updateTile(enabled)
            } catch (e: Exception) {
                F8Log.e("F8scapeTileService", "Failed to query state for Tile", e)
            }
        }
    }

    private fun updateTile(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "F8scape"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (enabled) "StandBy On" else "StandBy Off"
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
