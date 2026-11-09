package com.joulearc.app.core

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

/** Theme choice: follow the system, light, dark, or dark with true black surfaces (AMOLED). */
const val THEME_MODE_AMOLED = 3

/** Resolves the theme choice: 0 = follow the system, 1 = light, 2 = dark, 3 = AMOLED (dark). */
@Composable
fun isAppInDarkTheme(themeMode: Int): Boolean = when (themeMode) {
    1 -> false
    2, THEME_MODE_AMOLED -> true
    else -> isSystemInDarkTheme()
}

/** Pure black background and near-black containers, so OLED pixels stay off. */
private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF303030),
)

/** Digits of equal width, so a changing number never changes the width of its line. */
private fun Typography.tabular(): Typography {
    fun TextStyle.t() = copy(fontFeatureSettings = "tnum")
    return copy(
        displayLarge = displayLarge.t(), displayMedium = displayMedium.t(), displaySmall = displaySmall.t(),
        headlineLarge = headlineLarge.t(), headlineMedium = headlineMedium.t(), headlineSmall = headlineSmall.t(),
        titleLarge = titleLarge.t(), titleMedium = titleMedium.t(), titleSmall = titleSmall.t(),
        bodyLarge = bodyLarge.t(), bodyMedium = bodyMedium.t(), bodySmall = bodySmall.t(),
        labelLarge = labelLarge.t(), labelMedium = labelMedium.t(), labelSmall = labelSmall.t(),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JouleArcTheme(themeMode: Int = 0, content: @Composable () -> Unit) {
    val dark = isAppInDarkTheme(themeMode)
    val context = LocalContext.current
    val base = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    val colorScheme = if (themeMode == THEME_MODE_AMOLED) base.amoled() else base
    val typography = remember { Typography().tabular() }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = typography,
        content = content,
    )
}
