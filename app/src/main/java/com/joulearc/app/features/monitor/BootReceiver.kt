package com.joulearc.app.features.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the monitor after the device boots. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            MonitorService.start(context)
        }
    }
}
