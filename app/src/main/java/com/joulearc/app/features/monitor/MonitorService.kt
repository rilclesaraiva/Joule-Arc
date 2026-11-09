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
import com.joulearc.app.R
import com.joulearc.app.core.ACTION_REFRESH_WIDGETS
import com.joulearc.app.core.formatTemperature
import com.joulearc.app.data.AlertKind
import com.joulearc.app.data.AlertTracker
import com.joulearc.app.data.BatteryReader
import com.joulearc.app.data.BatteryRepository
import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.ChargeEstimator
import com.joulearc.app.data.LevelHistoryRepository
import com.joulearc.app.data.PowerSource
import com.joulearc.app.data.PowerState
import com.joulearc.app.data.SessionRepository
import com.joulearc.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Foreground service that keeps sampling the battery, updates a persistent notification and sends
 * the charge and low battery reminders. It samples fast while the app is visible or the device
 * charges, and wakes up immediately on any battery or screen broadcast.
 */
class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val estimator = ChargeEstimator()
    private val alerts = AlertTracker()
    private var started = false
    private var foreground = false
    private var sawUi = false
    private val createdMs = System.currentTimeMillis()
    private var lastNotifyMs = 0L
    private var lastWidgetMs = 0L
    private var lastNotifyText: String? = null
    private var lastSnapshot: BatterySnapshot? = null

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            wake.trySend(Unit)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannels()
        SettingsRepository.init(this)
        SessionRepository.init(this)
        LevelHistoryRepository.init(this)
        val reader = BatteryReader(this)
        val first = reader.read()
        // Started as a foreground service, so it must call startForeground again on every start.
        if (SettingsRepository.persistentNotification.value) foreground = false
        applyNotificationSetting(first)
        if (!started) {
            started = true
            ContextCompat.registerReceiver(
                this,
                wakeReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            scope.launch {
                BatteryRepository.uiVisible.collect { wake.trySend(Unit) }
            }
            scope.launch {
                SettingsRepository.persistentNotification.collect { wake.trySend(Unit) }
            }
            scope.launch {
                while (isActive) {
                    sampleOnce(reader)
                    if (shouldStopQuietly()) {
                        stopSelf()
                        break
                    }
                    withTimeoutOrNull(nextIntervalMs()) { wake.receive() }
                }
            }
        }
        return if (foreground) START_STICKY else START_NOT_STICKY
    }

    /**
     * With the persistent notification on, the service is a foreground service. With it off there is
     * no notification: the service only runs while the app is open and stops itself afterwards.
     */
    private fun applyNotificationSetting(snapshot: BatterySnapshot?) {
        val on = SettingsRepository.persistentNotification.value
        if (on && !foreground) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(notificationTitle(snapshot), notificationText(snapshot)),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
            )
            foreground = true
            lastNotifyText = null
        } else if (!on && foreground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
            foreground = false
        }
    }

    override fun onDestroy() {
        if (started) {
            unregisterReceiver(wakeReceiver)
            started = false
        }
        scope.cancel()
        super.onDestroy()
    }

    /** Without the notification, stop once the app is no longer open (after a short grace at start). */
    private fun shouldStopQuietly(): Boolean {
        if (SettingsRepository.persistentNotification.value) return false
        if (BatteryRepository.uiVisible.value) {
            sawUi = true
            return false
        }
        return sawUi || System.currentTimeMillis() - createdMs > QUIET_START_GRACE_MS
    }

    private suspend fun sampleOnce(reader: BatteryReader) {
        val snapshot = reader.read() ?: return
        // Picks up a change of the persistent notification setting while the service runs.
        applyNotificationSetting(snapshot)
        lastSnapshot = snapshot
        BatteryRepository.publish(snapshot)
        BatteryRepository.publishEstimate(estimator.update(snapshot))
        BatteryRepository.publishCapacity(estimator.capacityMah)
        SessionRepository.onSnapshot(snapshot)
        LevelHistoryRepository.onSnapshot(snapshot)
        SettingsRepository.recordCurrent(snapshot)
        checkAlerts(snapshot)

        val title = notificationTitle(snapshot)
        val text = notificationText(snapshot)
        val now = System.currentTimeMillis()
        val manager = getSystemService(NotificationManager::class.java)
        val key = title + "|" + text
        if (foreground && key != lastNotifyText && now - lastNotifyMs >= NOTIFY_MIN_INTERVAL_MS) {
            lastNotifyText = key
            lastNotifyMs = now
            manager.notify(NOTIFICATION_ID, buildNotification(title, text))
        }

        val widgetInterval = if (snapshot.isCharging || BatteryRepository.uiVisible.value) {
            WIDGET_FAST_INTERVAL_MS
        } else WIDGET_SLOW_INTERVAL_MS
        if (now - lastWidgetMs >= widgetInterval) {
            lastWidgetMs = now
            sendBroadcast(Intent(ACTION_REFRESH_WIDGETS).setPackage(packageName))
        }
    }

    private fun checkAlerts(s: BatterySnapshot) {
        val events = alerts.update(
            level = s.levelPercent,
            plugged = s.source != PowerSource.BATTERY,
            chargeAt = SettingsRepository.chargeAlertLevel.value.takeIf { SettingsRepository.chargeAlertEnabled.value },
            lowAt = SettingsRepository.lowAlertLevel.value.takeIf { SettingsRepository.lowAlertEnabled.value },
        )
        val manager = getSystemService(NotificationManager::class.java)
        events.forEach { kind ->
            val (id, text) = when (kind) {
                AlertKind.CHARGE_LIMIT -> ALERT_CHARGE_ID to getString(R.string.alert_charge_text)
                AlertKind.LOW_BATTERY -> ALERT_LOW_ID to getString(R.string.alert_low_text)
            }
            val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_bolt)
                .setContentTitle(getString(R.string.alert_title, s.levelPercent))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openAppIntent())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()
            manager.notify(id, notification)
        }
    }

    private fun nextIntervalMs(): Long = when {
        BatteryRepository.uiVisible.value -> VISIBLE_INTERVAL_MS
        lastSnapshot?.let { it.isCharging || it.settling } == true -> CHARGING_INTERVAL_MS
        else -> IDLE_INTERVAL_MS
    }

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.monitor_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.monitor_channel_description) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL_ID,
                getString(R.string.alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = getString(R.string.alert_channel_description) },
        )
    }

    /** The state ("Charging", "Discharging"...) is the title, so the app name is not repeated. */
    private fun notificationTitle(snapshot: BatterySnapshot?): String =
        if (snapshot == null) getString(R.string.app_name) else getString(stateLabel(snapshot.powerState))

    private fun notificationText(snapshot: BatterySnapshot?): String =
        if (snapshot == null) {
            getString(R.string.monitor_starting)
        } else {
            getString(
                R.string.notification_text_short,
                snapshot.signedCurrentMa ?: 0,
                snapshot.levelPercent,
                formatTemperature(snapshot.temperatureC, SettingsRepository.useFahrenheit.value),
            )
        }

    private fun openAppIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun buildNotification(title: String, text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    companion object {
        private const val CHANNEL_ID = "monitor"
        private const val ALERT_CHANNEL_ID = "alerts"
        private const val NOTIFICATION_ID = 1
        private const val ALERT_CHARGE_ID = 2
        private const val ALERT_LOW_ID = 3
        private const val VISIBLE_INTERVAL_MS = 500L
        private const val CHARGING_INTERVAL_MS = 2_000L
        private const val IDLE_INTERVAL_MS = 30_000L
        private const val NOTIFY_MIN_INTERVAL_MS = 1_000L
        private const val QUIET_START_GRACE_MS = 10_000L
        private const val WIDGET_FAST_INTERVAL_MS = 10_000L
        private const val WIDGET_SLOW_INTERVAL_MS = 60_000L

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            if (SettingsRepository.persistentNotification.value) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stateLabel(state: PowerState): Int = when (state) {
            PowerState.CHARGING -> R.string.status_charging
            PowerState.DISCHARGING -> R.string.status_discharging
            PowerState.FULL -> R.string.status_full
            PowerState.PLUGGED_DRAINING -> R.string.status_draining
            PowerState.PLUGGED_PAUSED -> R.string.status_paused
            PowerState.CHECKING -> R.string.status_checking
        }
    }
}
