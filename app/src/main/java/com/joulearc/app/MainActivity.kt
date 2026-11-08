package com.joulearc.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joulearc.app.core.JouleArcTheme
import com.joulearc.app.core.PillTabRow
import com.joulearc.app.core.SupportFooter
import com.joulearc.app.data.AppUsageReader
import com.joulearc.app.data.BatteryRepository
import com.joulearc.app.data.LevelHistoryRepository
import com.joulearc.app.data.SessionRepository
import com.joulearc.app.data.SettingsRepository
import com.joulearc.app.data.computeInsights
import com.joulearc.app.data.drainedPercent
import com.joulearc.app.data.rankAppUsage
import com.joulearc.app.features.health.HealthScreen
import com.joulearc.app.features.history.HistoryScreen
import com.joulearc.app.features.monitor.MonitorScreen
import com.joulearc.app.features.monitor.MonitorService
import com.joulearc.app.features.usage.BatteryUsageScreen
import com.joulearc.app.features.usage.UsageAccessCard
import com.joulearc.app.features.usage.UsageReport
import com.joulearc.app.features.usage.isUsageAccessGranted
import com.joulearc.app.features.usage.usageAccessSettingsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DAY_MS = 24L * 3_600_000L
private const val USAGE_PAGE = 3

/** Entry point: wires the features together. Features never import each other. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SessionRepository.init(this)
        SettingsRepository.init(this)
        LevelHistoryRepository.init(this)
        MonitorService.start(this)

        setContent {
            JouleArcTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val snapshot by BatteryRepository.snapshot.collectAsStateWithLifecycle()
                val estimate by BatteryRepository.estimate.collectAsStateWithLifecycle()
                val sessions by SessionRepository.sessions.collectAsStateWithLifecycle()
                val activeSession by SessionRepository.active.collectAsStateWithLifecycle()
                val capacityMah by BatteryRepository.capacityMah.collectAsStateWithLifecycle()
                val designCapacity by SettingsRepository.designCapacityMah.collectAsStateWithLifecycle()
                val levelSamples by LevelHistoryRepository.samples.collectAsStateWithLifecycle()
                val insights = remember(sessions) { computeInsights(sessions) }

                val pagerState = rememberPagerState { 4 }
                var windowDays by rememberSaveable { mutableIntStateOf(1) }
                var notificationsGranted by remember { mutableStateOf(hasNotificationPermission()) }
                var usageGranted by remember { mutableStateOf(isUsageAccessGranted(context)) }

                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { notificationsGranted = it }

                LifecycleResumeEffect(Unit) {
                    notificationsGranted = hasNotificationPermission()
                    usageGranted = isUsageAccessGranted(context)
                    onPauseOrDispose { }
                }

                val report by produceState<UsageReport?>(null, usageGranted, windowDays, pagerState.currentPage) {
                    if (pagerState.currentPage != USAGE_PAGE || !usageGranted) return@produceState
                    value = null
                    value = withContext(Dispatchers.IO) { buildUsageReport(context, windowDays) }
                }

                val tabLabels = listOf(
                    stringResource(R.string.tab_live),
                    stringResource(R.string.tab_history),
                    stringResource(R.string.tab_health),
                    stringResource(R.string.tab_apps),
                )

                Scaffold { padding ->
                    Column(Modifier.padding(padding)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                        )
                        PillTabRow(
                            labels = tabLabels,
                            selected = pagerState.currentPage,
                            onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )

                        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                            when (page) {
                                0 -> Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        ElevatedCard(Modifier.fillMaxWidth()) {
                                            Column(
                                                Modifier.padding(24.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.notifications_title),
                                                    style = MaterialTheme.typography.titleMedium,
                                                )
                                                Text(
                                                    text = stringResource(R.string.notifications_body),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                                Button(onClick = {
                                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }) { Text(stringResource(R.string.notifications_action)) }
                                            }
                                        }
                                    }
                                    MonitorScreen(snapshot = snapshot, estimate = estimate)
                                    SupportFooter()
                                }

                                1 -> HistoryScreen(
                                    active = activeSession,
                                    sessions = sessions,
                                    insights = insights,
                                )

                                2 -> HealthScreen(
                                    snapshot = snapshot,
                                    capacityMah = capacityMah,
                                    designCapacityMah = designCapacity,
                                    onDesignCapacityChange = { SettingsRepository.setDesignCapacityMah(it) },
                                )

                                else -> if (!usageGranted) {
                                    Column(Modifier.padding(16.dp)) {
                                        UsageAccessCard(
                                            granted = false,
                                            onOpenSettings = { context.startActivity(usageAccessSettingsIntent()) },
                                        )
                                    }
                                } else {
                                    BatteryUsageScreen(
                                        report = report,
                                        samples = levelSamples,
                                        windowDays = windowDays,
                                        onWindowChange = { windowDays = it },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        BatteryRepository.setUiVisible(true)
    }

    override fun onStop() {
        BatteryRepository.setUiVisible(false)
        super.onStop()
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}

/**
 * Ranks apps over the window and attaches icons. The window is shortened to the start of the
 * battery level history, so the drained percentage and the usage times cover the same period.
 */
private fun buildUsageReport(context: Context, windowDays: Int): UsageReport {
    val now = System.currentTimeMillis()
    val samples = LevelHistoryRepository.samples.value
    val windowStart = now - windowDays * DAY_MS
    val from = samples.firstOrNull()?.let { maxOf(windowStart, it.timeMs) } ?: windowStart
    val reader = AppUsageReader(context)
    val apps = rankAppUsage(reader.read(from, now))
    val icons = apps.mapNotNull { app ->
        val bitmap = reader.icon(app.packageName)?.toBitmap(96, 96)?.asImageBitmap()
        if (bitmap == null) null else app.packageName to bitmap
    }.toMap()
    return UsageReport(
        apps = apps,
        icons = icons,
        drainedPercent = if (samples.isEmpty()) null else drainedPercent(samples, from, now),
        fromMs = from,
        clipped = from > windowStart,
    )
}
