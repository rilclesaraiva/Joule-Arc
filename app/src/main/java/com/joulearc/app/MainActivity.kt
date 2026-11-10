package com.joulearc.app

import android.Manifest
import androidx.compose.ui.unit.DpOffset
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import com.joulearc.app.core.FloatingCircleButton
import kotlin.coroutines.cancellation.CancellationException
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import com.joulearc.app.data.DataExport
import com.joulearc.app.data.ExportInput
import com.joulearc.app.data.BatteryRepository
import com.joulearc.app.data.DeviceCapacities
import com.joulearc.app.data.DeviceInfo
import com.joulearc.app.data.LevelHistoryRepository
import com.joulearc.app.data.SessionRepository
import com.joulearc.app.data.SettingsRepository
import com.joulearc.app.data.computeDrainStats
import com.joulearc.app.data.updates.GitHubUpdatesRepository
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
import com.joulearc.app.features.updates.UpdateScreen
import com.joulearc.app.features.usage.AppUsageScreen
import com.joulearc.app.features.usage.BatteryUsageScreen
import com.joulearc.app.features.usage.UsageAccessCard
import com.joulearc.app.features.usage.UsageReport
import com.joulearc.app.features.usage.isUsageAccessGranted
import com.joulearc.app.features.usage.usageAccessSettingsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val HOUR_MS = 3_600_000L
private const val DAY_MS = 24L * HOUR_MS

// Routes. The back stack is kept as "home|history|session:123" so it survives recreation.
private const val HOME = "home"
private const val HISTORY = "history"
private const val SESSION = "session:"
private const val DRAIN = "drain"
private const val USAGE = "usage"
private const val APP = "app:"
private const val SETTINGS = "settings"
private const val ABOUT = "about"

/** Entry point: wires the features together and owns navigation. Features never import each other. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SettingsRepository.init(this)
        GitHubUpdatesRepository.init(this)
        coldStart = savedInstanceState == null
        SessionRepository.init(this)
        LevelHistoryRepository.init(this)
        if (savedInstanceState == null) BatteryRepository.resetExtremes()

        // The app's own recomposer: animations ignore the system animation scale.
        setContent(parent = appRecomposer()) {
            val themeMode by SettingsRepository.themeMode.collectAsStateWithLifecycle()
            // Starts the monitor, and again whenever the persistent notification is switched.
            val persistentNotification by SettingsRepository.persistentNotification.collectAsStateWithLifecycle()
            LaunchedEffect(persistentNotification) { MonitorService.start(this@MainActivity) }
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

    private var coldStart = false

    override fun onStart() {
        super.onStart()
        GitHubUpdatesRepository.checkIfDue(coldStart)
        coldStart = false
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
    val recorded by SettingsRepository.recorded.collectAsStateWithLifecycle()
    val fahrenheit by SettingsRepository.useFahrenheit.collectAsStateWithLifecycle()
    val capacities by produceState<DeviceCapacities?>(null) {
        value = withContext(Dispatchers.IO) { DeviceInfo.readCapacities(context) }
    }
    val insights = remember(sessions) { computeInsights(sessions) }
    val drainStats = remember(levelSamples) {
        val now = System.currentTimeMillis()
        computeDrainStats(levelSamples, now - 14L * DAY_MS, now)
    }

    // Kept here, above the page switch, so coming back from a page returns to the same spot.
    val homeScroll = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val usageList = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var stack by rememberSaveable { mutableStateOf(HOME) }
    val route = stack.substringAfterLast('|')
    val open: (String) -> Unit = { stack = "$stack|$it" }
    val back: () -> Unit = { stack = stack.substringBeforeLast('|') }
    // Back gesture: with the preview on, the page slides with the finger and the page below shows through.
    val predictive by SettingsRepository.predictiveBack.collectAsStateWithLifecycle()
    val backProgress = remember { Animatable(0f) }
    // Set just before a committed back gesture, so the page change does not animate twice.
    val instant = remember { booleanArrayOf(false) }
    LaunchedEffect(stack) { instant[0] = false }
    val scope = rememberCoroutineScope()
    // The on-screen back button plays the same slide-out as the gesture, then swaps pages at once.
    val goBack: () -> Unit = {
        if (predictive && stack.contains('|')) {
            if (!backProgress.isRunning && backProgress.value == 0f) {
                scope.launch {
                    backProgress.animateTo(1f, tween(AppMotion.SHORT))
                    instant[0] = true
                    back()
                    backProgress.snapTo(0f)
                }
            }
        } else {
            back()
        }
    }
    var showSystemApps by rememberSaveable { mutableStateOf(false) }
    var usageMenu by remember { mutableStateOf(false) }
    if (predictive) {
        PredictiveBackHandler(enabled = stack.contains('|')) { events ->
            try {
                events.collect { backProgress.snapTo(it.progress) }
                // Finished: slide the rest of the way out, then swap pages without a second animation.
                backProgress.animateTo(1f, tween(AppMotion.SHORT))
                instant[0] = true
                back()
                backProgress.snapTo(0f)
            } catch (e: CancellationException) {
                backProgress.animateTo(0f, tween(AppMotion.SHORT))
            }
        }
    } else {
        BackHandler(enabled = stack.contains('|')) { back() }
    }

    var notificationsGranted by remember { mutableStateOf(hasNotificationPermission()) }
    var usageGranted by remember { mutableStateOf(isUsageAccessGranted(context)) }
    var windowDays by rememberSaveable { mutableIntStateOf(1) }
    // What the usage list is limited to: a day (14-day view) and/or an hour; null = everything.
    var selectedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedHour by rememberSaveable { mutableStateOf<Long?>(null) }
    val changeWindow: (Int) -> Unit = {
        windowDays = it
        selectedDay = null
        selectedHour = null
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notificationsGranted = it }
    LifecycleResumeEffect(Unit) {
        notificationsGranted = hasNotificationPermission()
        usageGranted = isUsageAccessGranted(context)
        onPauseOrDispose { }
    }

    val onUsagePages = route == USAGE || route.startsWith(APP)
    val report by produceState<UsageReport?>(null, usageGranted, windowDays, selectedDay, selectedHour, onUsagePages) {
        if (!onUsagePages || !usageGranted) return@produceState
        value = withContext(Dispatchers.IO) { buildUsageReport(context, windowDays, selectedDay, selectedHour) }
    }

    // Export: the user picks where the file goes (no storage permission needed); it is built on a background thread.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val input = ExportInput(snapshot, estimate, capacityMah, capacities?.referenceMah, usageGranted)
            Toast.makeText(context, R.string.export_started, Toast.LENGTH_SHORT).show()
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { DataExport.write(context, it, input) } != null
                    } catch (e: Exception) {
                        false
                    }
                }
                Toast.makeText(context, if (ok) R.string.export_done else R.string.export_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    val prompt by GitHubUpdatesRepository.prompt.collectAsStateWithLifecycle()
    val renderPage: @Composable (String) -> Unit = { target ->
        val page = target.substringAfterLast('|')
        when {
            page == HOME -> {
                val homeState = HomeState(
                    snapshot = snapshot,
                    estimate = estimate,
                    capacityMah = capacityMah,
                    designCapacityMah = capacities?.referenceMah,
                    manufacturerCapacityMah = capacities?.manufacturerMah,
                    recorded = recorded,
                    extremes = extremes,
                    fahrenheit = fahrenheit,
                    sessionCount = sessions.size,
                    insights = insights,
                    drain = drainStats,
                    notificationsGranted = notificationsGranted,
                )
                HomePage(onSettings = { open(SETTINGS) }) { modifier ->
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
                                        scrollState = homeScroll,
                        modifier = modifier,
                    )
                }
            }

            page == HISTORY -> Page(goBack) { modifier ->
                HistoryScreen(
                    active = activeSession,
                    sessions = sessions,
                    insights = insights,
                    onSessionClick = { open(SESSION + it.startMs) },
                    modifier = modifier,
                )
            }

            page.startsWith(SESSION) -> Page(goBack) { modifier ->
                val start = page.removePrefix(SESSION).toLongOrNull()
                val session = activeSession?.takeIf { it.startMs == start }
                    ?: sessions.firstOrNull { it.startMs == start }
                if (session != null) SessionDetailScreen(session, modifier)
            }

            page == DRAIN -> Page(goBack) { modifier ->
                DrainScreen(
                    stats = drainStats,
                    levelPercent = snapshot?.levelPercent,
                    capacityMah = capacityMah,
                    modifier = modifier,
                )
            }

            page == USAGE -> Page(
                goBack,
                trailing = {
                    Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp)) {
                        FloatingCircleButton(
                            icon = R.drawable.ic_more_vert,
                            description = stringResource(R.string.usage_more_options),
                            onClick = { usageMenu = true },
                        )
                        DropdownMenu(
                            expanded = usageMenu,
                            onDismissRequest = { usageMenu = false },
                            shape = RoundedCornerShape(20.dp),
                            offset = DpOffset(0.dp, 8.dp),
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (showSystemApps) R.string.usage_hide_system else R.string.usage_show_system,
                                        ),
                                    )
                                },
                                onClick = {
                                    showSystemApps = !showSystemApps
                                    usageMenu = false
                                },
                            )
                        }
                    }
                },
            ) { modifier ->
                if (!usageGranted) {
                    Column(modifier.windowInsetsPadding(WindowInsets.systemBars).padding(16.dp)) {
                        PageTitle(stringResource(R.string.usage_per_app))
                        UsageAccessCard(
                            granted = false,
                            onOpenSettings = { context.startActivity(usageAccessSettingsIntent()) },
                        )
                    }
                } else {
                    BatteryUsageScreen(
                        report = report?.let { r ->
                            r.copy(apps = r.apps.filter { showSystemApps || !it.isSystem }.take(30))
                        },
                        samples = levelSamples,
                        windowDays = windowDays,
                        onWindowChange = changeWindow,
                        selectedDay = selectedDay,
                        selectedHour = selectedHour,
                        onSelectDay = {
                            selectedDay = it
                            selectedHour = null
                        },
                        onSelectHour = { selectedHour = it },
                        onAppClick = { open(APP + it) },
                        listState = usageList,
                        modifier = modifier,
                    )
                }
            }

            page.startsWith(APP) -> Page(goBack) { modifier ->
                AppUsageScreen(
                    packageName = page.removePrefix(APP),
                    report = report,
                    windowDays = windowDays,
                    onWindowChange = changeWindow,
                    modifier = modifier,
                )
            }

            page == SETTINGS -> Page(goBack) { modifier ->
                SettingsScreen(
                    onOpenAbout = { open(ABOUT) },
                    onExport = { exportLauncher.launch(DataExport.fileName(System.currentTimeMillis())) },
                    modifier = modifier,
                )
            }

            page == ABOUT -> Page(goBack) { modifier -> AboutScreen(modifier) }
        }
    }
    val gesturing by remember { derivedStateOf { backProgress.value > 0f } }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceDim)) {
    if (gesturing && stack.contains('|')) {
        // The page below, revealed as the current one slides away (slight parallax, fading scrim).
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationX = -size.width * 0.2f * (1f - backProgress.value)
            },
        ) {
            renderPage(stack.substringBeforeLast('|'))
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.32f * (1f - backProgress.value))))
        }
    }
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            val p = backProgress.value
            translationX = size.width * p
            if (p > 0f) {
                shadowElevation = 24.dp.toPx()
                shape = RoundedCornerShape((28f * minOf(1f, p * 6f)).dp)
                clip = true
            }
        },
    ) {
    AnimatedContent(
        targetState = stack,
        transitionSpec = {
            if (instant[0]) {
                androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
            } else {
                val forward = targetState.count { it == '|' } >= initialState.count { it == '|' }
                val direction = if (forward) 1 else -1
                (slideInHorizontally(tween(AppMotion.MEDIUM)) { it / 5 * direction } + fadeIn(tween(AppMotion.MEDIUM)))
                    .togetherWith(
                        slideOutHorizontally(tween(AppMotion.MEDIUM)) { -it / 5 * direction } + fadeOut(tween(AppMotion.SHORT)),
                    )
            }
        },
        label = "pages",
    ) { target -> renderPage(target) }
    prompt?.let { release ->
        BackHandler { GitHubUpdatesRepository.dismissForNow() }
        UpdateScreen(
            release = release,
            installedVersion = GitHubUpdatesRepository.installed,
            onDownload = {
                val url = release.apkUrl ?: release.pageUrl
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            },
            onNotNow = GitHubUpdatesRepository::dismissForNow,
            onSnoozeWeek = { GitHubUpdatesRepository.snooze(7) },
            onSnoozeMonth = { GitHubUpdatesRepository.snooze(30) },
            onSkip = { GitHubUpdatesRepository.skip(release.version) },
        )
    }
    }
    }
}

/** Home: no fixed bar. The title and the settings button are part of the scrolling content. */
@Composable
private fun HomePage(onSettings: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            content(Modifier.fillMaxSize())
            FloatingCircleButton(
                icon = R.drawable.ic_settings,
                description = stringResource(R.string.settings_title),
                onClick = onSettings,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
            )
        }
    }
}

/** Inner page: the title scrolls with the content; only the round back button stays over the page. */
@Composable
private fun Page(
    onBack: () -> Unit,
    trailing: @Composable BoxScope.() -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            content(Modifier.fillMaxSize())
            BackButton(onBack, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp))
            trailing()
        }
    }
}

/**
 * Ranks apps over the window and attaches icons. The window is shortened to the start of the
 * battery level history, so the drained percentage and the usage times cover the same period.
 */
private fun buildUsageReport(context: Context, windowDays: Int, selectedDay: Long?, selectedHour: Long?): UsageReport {
    val now = System.currentTimeMillis()
    val samples = LevelHistoryRepository.samples.value
    val windowStart = now - windowDays * DAY_MS
    // An hour or a day picked in the chart narrows the window to just that slot.
    val slotStart = selectedHour ?: selectedDay
    val slotEnd = selectedHour?.let { it + HOUR_MS } ?: selectedDay?.let { it + DAY_MS }
    val base = slotStart ?: windowStart
    val to = minOf(slotEnd ?: now, now)
    val from = samples.firstOrNull()?.let { maxOf(base, it.timeMs) } ?: base
    if (from >= to) {
        return UsageReport(emptyList(), emptyMap(), if (samples.isEmpty()) null else 0f, from, false)
    }
    val reader = AppUsageReader(context)
    val apps = rankAppUsage(reader.read(from, to), limit = 100)
    val icons = apps.mapNotNull { app ->
        val bitmap = reader.icon(app.packageName)?.toBitmap(96, 96)?.asImageBitmap()
        if (bitmap == null) null else app.packageName to bitmap
    }.toMap()
    return UsageReport(
        apps = apps,
        icons = icons,
        drainedPercent = if (samples.isEmpty()) null else drainedPercent(samples, from, to),
        fromMs = from,
        clipped = slotStart == null && from > windowStart,
    )
}
