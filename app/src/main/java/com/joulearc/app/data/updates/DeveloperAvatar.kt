package com.joulearc.app.data.updates

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The maintainer's GitHub profile picture for the About page. It is downloaded once, kept in the
 * app's private storage and looked for again at most once a week (every 10 minutes while there is
 * none). An unchanged picture (HTTP 304) is not downloaded again.
 */
object DeveloperAvatar {
    private const val URL_AVATAR = "https://github.com/rilclesaraiva.png?size=256"
    private const val WEEK_MS = 7L * 24 * 3_600_000L
    private const val RETRY_MS = 10 * 60_000L
    private const val PREFS = "github"
    private const val KEY_ETAG = "avatar_etag"
    private const val KEY_LAST_TRY = "avatar_last_try"

    private fun file(context: Context): File = File(File(context.filesDir, "github"), "developer_avatar")

    /** The saved picture, or null. Call off the main thread. */
    fun cached(context: Context): Bitmap? {
        val f = file(context)
        if (!f.exists()) return null
        return try {
            BitmapFactory.decodeFile(f.path)
        } catch (e: Exception) {
            null
        }
    }

    /** Asks GitHub when it is time. Returns true when a new picture was saved. Call off the main thread. */
    fun refreshIfDue(context: Context, userAgent: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val f = file(context)
        val has = f.exists()
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_TRY, 0L)
        val due = now < last || now - last >= if (has) WEEK_MS else RETRY_MS
        if (!due) return false
        // Marked before asking, so a failure does not make the app insist every time the page opens.
        prefs.edit().putLong(KEY_LAST_TRY, now).apply()
        return try {
            download(f, prefs.getString(KEY_ETAG, null).takeIf { has }, userAgent, prefs)
        } catch (e: Exception) {
            false
        }
    }

    private fun download(
        target: File,
        etag: String?,
        userAgent: String,
        prefs: android.content.SharedPreferences,
    ): Boolean {
        val conn = URL(URL_AVATAR).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", userAgent)
            if (etag != null) conn.setRequestProperty("If-None-Match", etag)
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return false
            target.parentFile?.mkdirs()
            // Written to a temporary file first, so a half-downloaded picture is never left behind.
            val tmp = File(target.parentFile, target.name + ".tmp")
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (BitmapFactory.decodeFile(tmp.path) == null) {
                tmp.delete()
                return false
            }
            if (!tmp.renameTo(target)) {
                tmp.delete()
                return false
            }
            prefs.edit().putString(KEY_ETAG, conn.getHeaderField("ETag")).apply()
            return true
        } finally {
            conn.disconnect()
        }
    }
}
