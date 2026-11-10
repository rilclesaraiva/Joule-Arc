package com.joulearc.app.features.widget

import android.app.PendingIntent
import com.joulearc.app.core.formatInt
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.joulearc.app.R
import com.joulearc.app.core.ACTION_REFRESH_WIDGETS
import com.joulearc.app.core.formatPower
import com.joulearc.app.data.BatteryReader
import com.joulearc.app.data.PowerState

/** Home screen widget with the live current, power and level. Redrawn by the monitor service. */
class BatteryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = build(context)
        appWidgetIds.forEach { manager.updateAppWidget(it, views) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGETS) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BatteryWidgetProvider::class.java))
            if (ids.isNotEmpty()) onUpdate(context, manager, ids)
        }
    }

    private fun build(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_battery)
        val s = BatteryReader(context).read()
        if (s == null) {
            views.setTextViewText(R.id.widget_status, context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_current, "—")
            views.setTextViewText(R.id.widget_detail, context.getString(R.string.monitor_starting))
        } else {
            val statusRes = when (s.powerState) {
                PowerState.CHARGING -> R.string.status_charging
                PowerState.DISCHARGING -> R.string.status_discharging
                PowerState.FULL -> R.string.status_full
                PowerState.PLUGGED_DRAINING -> R.string.status_draining
                PowerState.PLUGGED_PAUSED -> R.string.status_paused
                PowerState.CHECKING -> R.string.status_checking
            }
            views.setTextViewText(
                R.id.widget_status,
                context.getString(R.string.tile_subtitle, context.getString(statusRes), s.levelPercent),
            )
            views.setTextViewText(
                R.id.widget_current,
                s.signedCurrentMa?.let { context.getString(R.string.tile_label, formatInt(it)) } ?: "—",
            )
            views.setTextViewText(
                R.id.widget_detail,
                context.getString(
                    R.string.widget_detail,
                    s.signedPowerMw?.let { formatPower(it) } ?: "—",
                    s.temperatureC,
                ),
            )
        }
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            val pending = PendingIntent.getActivity(
                context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
        }
        return views
    }
}
