package com.joulearc.app.features.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.joulearc.app.MainActivity
import com.joulearc.app.R
import com.joulearc.app.core.ACTION_REFRESH_WIDGETS
import com.joulearc.app.data.BatteryReader
import com.joulearc.app.data.BatteryRepository
import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.ChargeEstimator
import com.joulearc.app.data.ChargeStatus
import com.joulearc.app.data.LevelHistoryRepository
import com.joulearc.app.data.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Foreground service that keeps sampling the battery and updates a persistent notification.
 * It samples fast while the app is visible or the device charges, and wakes up immediately
 * on any battery broadcast (plug, unplug, level change).
 */
class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val estimator = ChargeEstimator()
    private var started = false
    private var lastNotifyMs = 0L
    private var lastWidgetMs = 0L
    private var lastNotifyText: String? = null
    private var lastSnapshot: BatterySnapshot? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            wake.trySend(Unit)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        SessionRepository.init(this)
        LevelHistoryRepository.init(this)
        val reader = BatteryReader(this)
        val first = reader.read()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(notificationText(first)),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (!started) {
            started = true
            ContextCompat.registerReceiver(
                this,
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            ContextCompat.registerReceiver(
                this,
                batteryReceiver,
                IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_SCREEN_OFF) },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            scope.launch {
                BatteryRepository.uiVisible.collect { wake.trySend(Unit) }
            }
            scope.launch {
                while (isActive) {
                    sampleOnce(reader)
                    withTimeoutOrNull(nextIntervalMs()) { wake.receive() }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (started) {
            unregisterReceiver(batteryReceiver)
            started = false
        }
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun sampleOnce(reader: BatteryReader) {
        val snapshot = reader.read() ?: return
        lastSnapshot = snapshot
        BatteryRepository.publish(snapshot)
        BatteryRepository.publishEstimate(estimator.update(snapshot))
        BatteryRepository.publishCapacity(estimator.capacityMah)
        SessionRepository.onSnapshot(snapshot)
        LevelHistoryRepository.onSnapshot(snapshot)

        val text = notificationText(snapshot)
        val now = System.currentTimeMillis()
        if (text != lastNotifyText && now - lastNotifyMs >= NOTIFY_MIN_INTERVAL_MS) {
            lastNotifyText = text
            lastNotifyMs = now
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
        }

        val widgetInterval = if (snapshot.isCharging || BatteryRepository.uiVisible.value) {
            WIDGET_FAST_INTERVAL_MS
        } else WIDGET_SLOW_INTERVAL_MS
        if (now - lastWidgetMs >= widgetInterval) {
            lastWidgetMs = now
            sendBroadcast(Intent(ACTION_REFRESH_WIDGETS).setPackage(packageName))
        }
    }

    private fun nextIntervalMs(): Long = when {
        BatteryRepository.uiVisible.value -> VISIBLE_INTERVAL_MS
        lastSnapshot?.isCharging == true -> CHARGING_INTERVAL_MS
        else -> IDLE_INTERVAL_MS
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.monitor_channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notificationText(snapshot: BatterySnapshot?): String =
        if (snapshot == null) {
            getString(R.string.monitor_starting)
        } else {
            getString(
                R.string.notification_text,
                getString(statusLabel(snapshot.status)),
                snapshot.signedCurrentMa ?: 0,
                snapshot.levelPercent,
                snapshot.temperatureC,
            )
        }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
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
        private const val VISIBLE_INTERVAL_MS = 500L
        private const val CHARGING_INTERVAL_MS = 2_000L
        private const val IDLE_INTERVAL_MS = 30_000L
        private const val NOTIFY_MIN_INTERVAL_MS = 1_000L
        private const val WIDGET_FAST_INTERVAL_MS = 10_000L
        private const val WIDGET_SLOW_INTERVAL_MS = 60_000L

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
