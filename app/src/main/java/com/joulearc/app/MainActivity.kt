package com.joulearc.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import com.joulearc.app.core.AppMotion
import com.joulearc.app.core.BackButton
import com.joulearc.app.core.PageTitle
import com.joulearc.app.core.JouleArcTheme
import com.joulearc.app.core.appRecomposer
import com.joulearc.app.core.isAppInDarkTheme
import com.joulearc.app.data.AppUsageReader
import com.joulearc.app.data.BatteryRepository
import com.joulearc.app.data.DeviceInfo
import com.joulearc.app.data.LevelHistoryRepository
import com.joulearc.app.data.SessionRepository
import com.joulearc.app.data.SettingsRepository
import com.joulearc.app.data.computeDrainStats
import com.joulearc.app.data.computeInsights
import com.joulearc.app.data.drainedPercent
import com.joulearc.app.data.rankAppUsage
import com.joulearc.app.features.drain.DrainScreen
import com.joulearc.app.features.history.HistoryScreen
import com.joulearc.app.features.history.SessionDetailScreen
import com.joulearc.app.features.home.HomeScreen
import com.joulearc.app.features.home.HomeState
import com.joulearc.app.features.monitor.MonitorService
import com.joulearc.app.features.settings.AboutScreen
import com.joulearc.app.features.settings.SettingsScreen
import com.joulearc.app.features.usage.BatteryUsageScreen
import com.joulearc.app.features.usage.UsageAccessCard
import com.joulearc.app.features.usage.UsageReport
import com.joulearc.app.features.usage.isUsageAccessGranted
import com.joulearc.app.features.usage.usageAccessSettingsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DAY_MS = 24L * 3_600_000L

// Routes. The back stack is kept as "home|history|session:123" so it survives recreation.
private const val HOME = "home"
private const val HISTORY = "history"
private const val SESSION = "session:"
private const val DRAIN = "drain"
private const val USAGE = "usage"
private const val SETTINGS = "settings"
private const val ABOUT = "about"

/** Entry point: wires the features together and owns navigation. Features never import each other. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SettingsRepository.init(this)
        SessionRepository.init(this)
        LevelHistoryRepository.init(this)
        if (savedInstanceState == null) BatteryRepository.resetExtremes()
        MonitorService.start(this)

        // The app's own recomposer: animations ignore the system animation scale.
        setContent(parent = appRecomposer()) {
            val themeMode by SettingsRepository.themeMode.collectAsStateWithLifecycle()
            val dark = isAppInDarkTheme(themeMode)
            LaunchedEffect(dark) {
                val transparent = Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                )
            }
            JouleArcTheme(themeMode) {
                AppContent(hasNotificationPermission = ::hasNotificationPermission)
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

@Composable
private fun AppContent(hasNotificationPermission: () -> Boolean) {
    val context = LocalContext.current
    val snapshot by BatteryRepository.snapshot.collectAsStateWithLifecycle()
    val estimate by BatteryRepository.estimate.collectAsStateWithLifecycle()
    val capacityMah by BatteryRepository.capacityMah.collectAsStateWithLifecycle()
    val extremes by BatteryRepository.extremes.collectAsStateWithLifecycle()
    val sessions by SessionRepository.sessions.collectAsStateWithLifecycle()
    val activeSession by SessionRepository.active.collectAsStateWithLifecycle()
    val levelSamples by LevelHistoryRepository.samples.collectAsStateWithLifecycle()
    val designUser by SettingsRepository.designCapacityMah.collectAsStateWithLifecycle()
    val recorded by SettingsRepository.recorded.collectAsStateWithLifecycle()
    val fahrenheit by SettingsRepository.useFahrenheit.collectAsStateWithLifecycle()
    val systemDesign by produceState<Int?>(null) {
        value = withContext(Dispatchers.IO) { DeviceInfo.systemDesignCapacityMah(context) }
    }
    val insights = remember(sessions) { computeInsights(sessions) }
    val drainStats = remember(levelSamples) {
        val now = System.currentTimeMillis()
        computeDrainStats(levelSamples, now - 14L * DAY_MS, now)
    }

    // Kept here, above the page switch, so coming back from a page returns to the same spot.
    val homeScroll = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    var stack by rememberSaveable { mutableStateOf(HOME) }
    val route = stack.substringAfterLast('|')
    val open: (String) -> Unit = { stack = "$stack|$it" }
    val back: () -> Unit = { stack = stack.substringBeforeLast('|') }
    BackHandler(enabled = stack.contains('|')) { back() }

    var notificationsGranted by remember { mutableStateOf(hasNotificationPermission()) }
    var usageGranted by remember { mutableStateOf(isUsageAccessGranted(context)) }
    var windowDays by rememberSaveable { mutableIntStateOf(1) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notificationsGranted = it }
    LifecycleResumeEffect(Unit) {
        notificationsGranted = hasNotificationPermission()
        usageGranted = isUsageAccessGranted(context)
        onPauseOrDispose { }
    }

    val report by produceState<UsageReport?>(null, usageGranted, windowDays, route) {
        if (route != USAGE || !usageGranted) return@produceState
        value = null
        value = withContext(Dispatchers.IO) { buildUsageReport(context, windowDays) }
    }

    AnimatedContent(
        targetState = stack,
        transitionSpec = {
            val forward = targetState.count { it == '|' } >= initialState.count { it == '|' }
            val direction = if (forward) 1 else -1
            (slideInHorizontally(tween(AppMotion.MEDIUM)) { it / 5 * direction } + fadeIn(tween(AppMotion.MEDIUM)))
                .togetherWith(
                    slideOutHorizontally(tween(AppMotion.MEDIUM)) { -it / 5 * direction } + fadeOut(tween(AppMotion.SHORT)),
                )
        },
        label = "pages",
    ) { target ->
        val page = target.substringAfterLast('|')
        when {
            page == HOME -> {
                val homeState = HomeState(
                    snapshot = snapshot,
                    estimate = estimate,
                    capacityMah = capacityMah,
                    designCapacityMah = designUser ?: systemDesign,
                    designFromSystem = designUser == null && systemDesign != null,
                    recorded = recorded,
                    extremes = extremes,
                    fahrenheit = fahrenheit,
                    sessionCount = sessions.size,
                    insights = insights,
                    drain = drainStats,
                    notificationsGranted = notificationsGranted,
                )
                HomePage { modifier ->
                    HomeScreen(
                        state = homeState,
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onOpenHistory = { open(HISTORY) },
                        onOpenDrain = { open(DRAIN) },
                        onOpenUsage = { open(USAGE) },
                        onResetRecorded = { SettingsRepository.resetRecorded() },
                        onSettings = { open(SETTINGS) },
                        scrollState = homeScroll,
                        modifier = modifier,
                    )
                }
            }

            page == HISTORY -> Page(back) { modifier ->
                HistoryScreen(
                    active = activeSession,
                    sessions = sessions,
                    insights = insights,
                    onSessionClick = { open(SESSION + it.startMs) },
                    modifier = modifier,
                )
            }

            page.startsWith(SESSION) -> Page(back) { modifier ->
                val start = page.removePrefix(SESSION).toLongOrNull()
                val session = activeSession?.takeIf { it.startMs == start }
                    ?: sessions.firstOrNull { it.startMs == start }
                if (session != null) SessionDetailScreen(session, modifier)
            }

            page == DRAIN -> Page(back) { modifier ->
                DrainScreen(
                    stats = drainStats,
                    levelPercent = snapshot?.levelPercent,
                    capacityMah = capacityMah,
                    modifier = modifier,
                )
            }

            page == USAGE -> Page(back) { modifier ->
                if (!usageGranted) {
                    Column(modifier.padding(16.dp)) {
                        PageTitle(stringResource(R.string.usage_per_app))
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
                        modifier = modifier,
                    )
                }
            }

            page == SETTINGS -> Page(back) { modifier ->
                SettingsScreen(systemDesignMah = systemDesign, onOpenAbout = { open(ABOUT) }, modifier = modifier)
            }

            page == ABOUT -> Page(back) { modifier -> AboutScreen(modifier) }
        }
    }
}

/** Home: no fixed bar. The title and the settings button are part of the scrolling content. */
@Composable
private fun HomePage(content: @Composable (Modifier) -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        content(Modifier.fillMaxSize().systemBarsPadding())
    }
}

/** Inner page: the title scrolls with the content; only the round back button stays over the page. */
@Composable
private fun Page(onBack: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        content(Modifier.fillMaxSize().systemBarsPadding())
        BackButton(onBack, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp))
    }
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
