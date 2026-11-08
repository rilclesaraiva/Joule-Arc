package com.joulearc.app.features.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.joulearc.app.R
import com.joulearc.app.data.BatteryReader
import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.ChargeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Quick Settings tile showing the live current. Refreshes once a second while the panel is open. */
class BatteryTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        job?.cancel()
        val reader = BatteryReader(applicationContext)
        job = scope.launch {
            while (isActive) {
                render(reader.read())
                delay(1_000L)
            }
        }
    }

    override fun onStopListening() {
        job?.cancel()
        super.onStopListening()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun render(s: BatterySnapshot?) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_bolt)
        if (s == null) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.app_name)
        } else {
            tile.state = if (s.isCharging) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = s.signedCurrentMa?.let { getString(R.string.tile_label, it) }
                ?: getString(R.string.app_name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val statusRes = when (s.status) {
                    ChargeStatus.CHARGING -> R.string.status_charging
                    ChargeStatus.DISCHARGING -> R.string.status_discharging
                    ChargeStatus.FULL -> R.string.status_full
                    ChargeStatus.NOT_CHARGING -> R.string.status_not_charging
                    ChargeStatus.UNKNOWN -> R.string.status_unknown
                }
                tile.subtitle = getString(R.string.tile_subtitle, getString(statusRes), s.levelPercent)
            }
        }
        tile.updateTile()
    }
}
