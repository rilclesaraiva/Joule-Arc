package com.joulearc.app.core

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Resolves the theme choice: 0 = follow the system, 1 = light, 2 = dark. */
@Composable
fun isAppInDarkTheme(themeMode: Int): Boolean = when (themeMode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JouleArcTheme(themeMode: Int = 0, content: @Composable () -> Unit) {
    val dark = isAppInDarkTheme(themeMode)
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
