package com.capturemate.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = CaptureBlue,
    secondary = CaptureGreen,
    onSurface = CaptureInk,
)

private val DarkColorScheme = darkColorScheme(
    primary = CaptureBlue,
    secondary = CaptureGreen,
)

@Composable
fun CaptureMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
