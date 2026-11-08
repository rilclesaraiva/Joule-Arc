package com.joulearc.app.features.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.joulearc.app.MainActivity
import com.joulearc.app.R
import com.joulearc.app.data.BatteryReader
import com.joulearc.app.data.BatteryRepository
import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.ChargeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Foreground service that keeps sampling the battery and updates a persistent notification. */
class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val reader = BatteryReader(this)
        val first = reader.read()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(first),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (!loopStarted) {
            loopStarted = true
            scope.launch {
                while (isActive) {
                    val snapshot = reader.read()
                    if (snapshot != null) {
                        BatteryRepository.publish(snapshot)
                        getSystemService(NotificationManager::class.java)
                            .notify(NOTIFICATION_ID, buildNotification(snapshot))
                    }
                    delay(if (snapshot?.isCharging == true) CHARGING_INTERVAL_MS else IDLE_INTERVAL_MS)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        loopStarted = false
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.monitor_channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(snapshot: BatterySnapshot?): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (snapshot == null) {
            getString(R.string.monitor_starting)
        } else {
            getString(
                R.string.notification_text,
                getString(statusLabel(snapshot.status)),
                snapshot.currentMa ?: 0,
                snapshot.levelPercent,
                snapshot.temperatureC,
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "monitor"
        private const val NOTIFICATION_ID = 1
        private const val CHARGING_INTERVAL_MS = 2_000L
        private const val IDLE_INTERVAL_MS = 30_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MonitorService::class.java))
        }

        fun statusLabel(status: ChargeStatus): Int = when (status) {
            ChargeStatus.CHARGING -> R.string.status_charging
            ChargeStatus.DISCHARGING -> R.string.status_discharging
            ChargeStatus.FULL -> R.string.status_full
            ChargeStatus.NOT_CHARGING -> R.string.status_not_charging
            ChargeStatus.UNKNOWN -> R.string.status_unknown
        }
    }
}
