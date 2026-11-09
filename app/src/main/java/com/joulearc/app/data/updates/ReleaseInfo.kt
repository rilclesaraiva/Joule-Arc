package com.joulearc.app.data.updates

import org.json.JSONObject

/** The newest public release: its version, the first APK attached, the release page and the notes. */
data class ReleaseInfo(
    val version: String,
    val apkUrl: String?,
    val pageUrl: String,
    val notes: String,
)

/** Reads the body of GitHub's "latest release" answer. Null when it has no usable tag. */
fun parseRelease(json: String): ReleaseInfo? =
    try {
        val o = JSONObject(json)
        val tag = o.optString("tag_name").trim()
        if (parseVersion(tag) == null) {
            null
        } else {
            val assets = o.optJSONArray("assets")
            var apk: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.optJSONObject(i) ?: continue
                    if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                        apk = a.optString("browser_download_url").takeIf { it.isNotBlank() }
                        if (apk != null) break
                    }
                }
            }
            ReleaseInfo(
                version = tag.removePrefix("v").removePrefix("V"),
                apkUrl = apk,
                pageUrl = o.optString("html_url").ifBlank { "https://github.com/rilclesaraiva/Joule-Arc/releases" },
                notes = o.optString("body").trim(),
            )
        }
    } catch (e: Exception) {
        null
    }
