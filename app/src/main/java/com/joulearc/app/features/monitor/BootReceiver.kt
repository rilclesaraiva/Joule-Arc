package com.joulearc.app.features.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joulearc.app.data.SettingsRepository

/** Restarts the monitor after the device boots. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SettingsRepository.init(context)
            // With the persistent notification off, the monitor only runs while the app is open.
            if (SettingsRepository.persistentNotification.value) MonitorService.start(context)
        }
    }
}
