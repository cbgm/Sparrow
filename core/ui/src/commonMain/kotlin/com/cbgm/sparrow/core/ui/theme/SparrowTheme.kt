package com.cbgm.sparrow.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme =
    darkColorScheme(
        primary = Colors.Primary,
        onPrimary = Colors.OnPrimary,
        secondary = Colors.Secondary,
        onSecondary = Colors.OnSecondary,
        error = Colors.Error,
        onError = Colors.OnError,
        onBackground = Colors.OnBackground,
        onSurface = Colors.OnSurface,
        onSurfaceVariant = Colors.OnSurfaceVariant,
        surface = Colors.Surface,
        primaryContainer = Colors.PrimaryContainer,
        surfaceVariant = Colors.SurfaceVariant,
        background = Colors.Background
    )

@Composable
fun SparrowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
