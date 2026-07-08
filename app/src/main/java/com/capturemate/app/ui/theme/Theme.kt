package com.capturemate.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = CaptureInk,
    onPrimary = CaptureOnDark,
    secondary = CaptureMuted,
    onSecondary = CaptureInk,
    background = CaptureBackground,
    onBackground = CaptureInk,
    surface = CaptureSurface,
    onSurface = CaptureInk,
    surfaceVariant = CaptureMuted,
    onSurfaceVariant = CaptureMutedForeground,
    error = CaptureDestructive,
    onError = CaptureOnDark,
    outline = CaptureBorder,
)

private val DarkColorScheme = darkColorScheme(
    primary = CaptureOnDark,
    onPrimary = CaptureInk,
    secondary = CaptureDarkMuted,
    onSecondary = CaptureOnDark,
    background = CaptureDarkBackground,
    onBackground = CaptureOnDark,
    surface = CaptureDarkSurface,
    onSurface = CaptureOnDark,
    surfaceVariant = CaptureDarkMuted,
    onSurfaceVariant = CaptureDarkMutedForeground,
    error = CaptureDestructive,
    onError = CaptureOnDark,
)

@Composable
fun CaptureMateTheme(
    // 디자인이 라이트 테마 전용이라 시스템 다크모드와 무관하게 항상 라이트로 고정.
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
