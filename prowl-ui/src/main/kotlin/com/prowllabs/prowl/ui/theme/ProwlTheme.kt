package com.prowllabs.prowl.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.prowllabs.prowl.ui.util.ProwlThemeMode
import com.prowllabs.prowl.ui.util.ProwlUiPreferences

private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    secondary = Color(0xFF5856D6),
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFC6C6C8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    secondary = Color(0xFF5E5CE6),
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF38383A),
)

@Composable
fun ProwlTheme(
    themeMode: ProwlThemeMode? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val resolvedMode = themeMode ?: ProwlUiPreferences.themeMode(context)
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (resolvedMode) {
        ProwlThemeMode.SYSTEM -> systemDark
        ProwlThemeMode.LIGHT -> false
        ProwlThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
