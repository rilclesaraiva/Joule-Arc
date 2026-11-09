package com.joulearc.app.data

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/** Per-app language (Android 13+). An empty tag means "follow the system". */
object AppLanguage {
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun current(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
        } else ""

    fun set(context: Context, tags: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(tags)
        }
    }
}
