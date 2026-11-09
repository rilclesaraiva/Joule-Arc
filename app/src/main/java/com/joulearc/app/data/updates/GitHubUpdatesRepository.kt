package com.joulearc.app.data.updates

import android.content.Context
import android.content.SharedPreferences
import com.joulearc.app.data.DeviceInfo
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Outcome of a check the user asked for in Settings. */
sealed interface ManualCheckResult {
    data class UpToDate(val version: String) : ManualCheckResult
    data object Failed : ManualCheckResult
    data object Available : ManualCheckResult
}

/**
 * Looks for a newer public release on GitHub. It only asks GitHub for information and sends nothing
 * about the user. The last answer and its ETag are kept, so an unchanged answer (HTTP 304) costs
 * nothing against the API limit. Updates are never installed by the app: "Download" opens the browser.
 */
object GitHubUpdatesRepository {
    private const val LATEST_URL = "https://api.github.com/repos/rilclesaraiva/Joule-Arc/releases/latest"
    private const val MIN_INTERVAL_MS = 10 * 60_000L
    private const val PREFS = "updates"
    private const val KEY_ETAG = "etag"
    private const val KEY_BODY = "body"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_SNOOZE_UNTIL = "snooze_until"
    private const val KEY_SKIPPED = "skipped_version"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var prefs: SharedPreferences
    private lateinit var userAgent: String
    private var installedVersion = ""

    /** "Not now": hides the prompt until the app is opened again. */
    private var dismissedThisSession = false

    private val _latest = MutableStateFlow<ReleaseInfo?>(null)
    /** The newest release, only when it is newer than the installed version. */
    val latest: StateFlow<ReleaseInfo?> = _latest.asStateFlow()

    private val _prompt = MutableStateFlow<ReleaseInfo?>(null)
    /** The release to offer on screen right now, after "not now", snooze and skip are applied. */
    val prompt: StateFlow<ReleaseInfo?> = _prompt.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    private val _manualResult = MutableStateFlow<ManualCheckResult?>(null)
    val manualResult: StateFlow<ManualCheckResult?> = _manualResult.asStateFlow()

    private val _snoozeUntilMs = MutableStateFlow(0L)
    val snoozeUntilMs: StateFlow<Long> = _snoozeUntilMs.asStateFlow()

    val installed: String get() = installedVersion

    fun init(context: Context) {
        if (this::prefs.isInitialized) return
        val app = context.applicationContext
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        installedVersion = DeviceInfo.appVersion(app)
        userAgent = "JouleArc/$installedVersion"
        _snoozeUntilMs.value = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
        // A version found earlier is offered at once, before any new check finishes.
        _latest.value = cachedBody()?.let(::newerThanInstalled)
        refreshPrompt()
    }

    /** Called when the app opens. [coldStart] always asks; coming back to the app asks at most every 10 minutes. */
    fun checkIfDue(coldStart: Boolean) {
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        val now = System.currentTimeMillis()
        val due = coldStart || now < last || now - last >= MIN_INTERVAL_MS
        if (due) launchCheck(manual = false)
    }

    /** The check button in Settings. */
    fun checkNow() {
        if (_checking.value) return
        _manualResult.value = null
        launchCheck(manual = true)
    }

    fun clearManualResult() {
        _manualResult.value = null
    }

    fun dismissForNow() {
        dismissedThisSession = true
        refreshPrompt()
    }

    fun snooze(days: Int) {
        val until = if (days <= 0) 0L else System.currentTimeMillis() + days * 24L * 3_600_000L
        prefs.edit().putLong(KEY_SNOOZE_UNTIL, until).apply()
        _snoozeUntilMs.value = until
        refreshPrompt()
    }

    fun skip(version: String) {
        prefs.edit().putString(KEY_SKIPPED, version).apply()
        refreshPrompt()
    }

    private fun launchCheck(manual: Boolean) {
        if (_checking.value) return
        _checking.value = true
        scope.launch {
            val ok = try {
                fetch()
            } catch (e: Exception) {
                false
            }
            val newer = cachedBody()?.let(::newerThanInstalled)
            _latest.value = newer
            if (manual) {
                _manualResult.value = when {
                    !ok -> ManualCheckResult.Failed
                    newer != null -> ManualCheckResult.Available
                    else -> ManualCheckResult.UpToDate(installedVersion)
                }
                // A check the user asked for always shows what it found.
                _prompt.value = newer
            } else {
                refreshPrompt()
            }
            _checking.value = false
        }
    }

    /** True when GitHub really answered (so the time of the check is kept). */
    private fun fetch(): Boolean {
        val conn = URL(LATEST_URL).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            val etag = prefs.getString(KEY_ETAG, null)
            if (etag != null && cachedBody() != null) conn.setRequestProperty("If-None-Match", etag)
            val now = System.currentTimeMillis()
            return when (conn.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    prefs.edit()
                        .putString(KEY_BODY, body)
                        .putString(KEY_ETAG, conn.getHeaderField("ETag"))
                        .putLong(KEY_LAST_CHECK, now)
                        .apply()
                    true
                }
                HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
                    true
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    // No public release yet.
                    prefs.edit().remove(KEY_BODY).remove(KEY_ETAG).putLong(KEY_LAST_CHECK, now).apply()
                    true
                }
                else -> false
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun cachedBody(): String? = prefs.getString(KEY_BODY, null)

    private fun newerThanInstalled(body: String): ReleaseInfo? =
        parseRelease(body)?.takeIf { isNewerVersion(it.version, installedVersion) }

    private fun refreshPrompt() {
        val release = _latest.value
        val hidden = release == null ||
            dismissedThisSession ||
            release.version == prefs.getString(KEY_SKIPPED, null) ||
            System.currentTimeMillis() < _snoozeUntilMs.value
        _prompt.value = if (hidden) null else release
    }
}
