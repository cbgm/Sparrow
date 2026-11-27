package com.cbgm.sparrow.core.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/*
BACKGROUND
#071A2E
└─ app/screen canvas

SURFACE
#0B2035
└─ subtle element above background

SURFACE CONTAINER
#102A46
└─ cards, fields, sheets, dialogs

SURFACE CONTAINER HIGH
#17324D
└─ stronger/elevated/selected neutral surface


PRIMARY
bright cyan
└─ Sparrow's main action / active accent
   ├─ onPrimary = black content
   └─ primaryContainer = subdued cyan/navy-tinted highlight

SECONDARY
muted blue/cyan
└─ supporting accent; lower priority than primary

TERTIARY
green in Sparrow
└─ positive / verified / available
   └─ tertiaryContainer = dark green-tinted panel/background

ERROR
red
└─ destructive / unavailable / failure
   └─ errorContainer = dark red-tinted warning background
 */

private val DarkColorScheme =
    darkColorScheme(
        primary = DarkColors.Primary,
        onPrimary = DarkColors.OnPrimary,
        primaryContainer = DarkColors.PrimaryContainer,
        onPrimaryContainer = DarkColors.OnPrimaryContainer,
        inversePrimary = DarkColors.InversePrimary,
        secondary = DarkColors.Secondary,
        onSecondary = DarkColors.OnSecondary,
        secondaryContainer = DarkColors.SecondaryContainer,
        onSecondaryContainer = DarkColors.OnSecondaryContainer,
        tertiary = DarkColors.Tertiary,
        onTertiary = DarkColors.OnTertiary,
        tertiaryContainer = DarkColors.TertiaryContainer,
        onTertiaryContainer = DarkColors.OnTertiaryContainer,
        error = DarkColors.Error,
        onError = DarkColors.OnError,
        errorContainer = DarkColors.ErrorContainer,
        onErrorContainer = DarkColors.OnErrorContainer,
        background = DarkColors.Background,
        onBackground = DarkColors.OnBackground,
        surface = DarkColors.Surface,
        onSurface = DarkColors.OnSurface,
        surfaceVariant = DarkColors.SurfaceVariant,
        onSurfaceVariant = DarkColors.OnSurfaceVariant,
        surfaceTint = DarkColors.Primary,
        inverseSurface = DarkColors.InverseSurface,
        inverseOnSurface = DarkColors.InverseOnSurface,
        outline = DarkColors.Outline,
        outlineVariant = DarkColors.OutlineVariant,
        scrim = DarkColors.Scrim,
        surfaceBright = DarkColors.SurfaceBright,
        surfaceDim = DarkColors.SurfaceDim,
        surfaceContainer = DarkColors.SurfaceContainer,
        surfaceContainerHigh = DarkColors.SurfaceContainerHigh,
        surfaceContainerHighest = DarkColors.SurfaceContainerHighest,
        surfaceContainerLow = DarkColors.SurfaceContainerLow,
        surfaceContainerLowest = DarkColors.SurfaceContainerLowest
    )

private val LightColorScheme =
    lightColorScheme(
        primary = LightColors.Primary,
        onPrimary = LightColors.OnPrimary,
        primaryContainer = LightColors.PrimaryContainer,
        onPrimaryContainer = LightColors.OnPrimaryContainer,
        inversePrimary = LightColors.InversePrimary,
        secondary = LightColors.Secondary,
        onSecondary = LightColors.OnSecondary,
        secondaryContainer = LightColors.SecondaryContainer,
        onSecondaryContainer = LightColors.OnSecondaryContainer,
        tertiary = LightColors.Tertiary,
        onTertiary = LightColors.OnTertiary,
        tertiaryContainer = LightColors.TertiaryContainer,
        onTertiaryContainer = LightColors.OnTertiaryContainer,
        error = LightColors.Error,
        onError = LightColors.OnError,
        errorContainer = LightColors.ErrorContainer,
        onErrorContainer = LightColors.OnErrorContainer,
        background = LightColors.Background,
        onBackground = LightColors.OnBackground,
        surface = LightColors.Surface,
        onSurface = LightColors.OnSurface,
        surfaceVariant = LightColors.SurfaceVariant,
        onSurfaceVariant = LightColors.OnSurfaceVariant,
        surfaceTint = LightColors.Primary,
        inverseSurface = LightColors.InverseSurface,
        inverseOnSurface = LightColors.InverseOnSurface,
        outline = LightColors.Outline,
        outlineVariant = LightColors.OutlineVariant,
        scrim = LightColors.Scrim,
        surfaceBright = LightColors.SurfaceBright,
        surfaceDim = LightColors.SurfaceDim,
        surfaceContainer = LightColors.SurfaceContainer,
        surfaceContainerHigh = LightColors.SurfaceContainerHigh,
        surfaceContainerHighest = LightColors.SurfaceContainerHighest,
        surfaceContainerLow = LightColors.SurfaceContainerLow,
        surfaceContainerLowest = LightColors.SurfaceContainerLowest
    )

@Composable
fun SparrowTheme(content: @Composable () -> Unit) {
    val mode by currentAppThemeMode.collectAsState()
    val dark = (mode ?: AppThemeMode.SYSTEM).isDark(isSystemInDarkTheme())
    SystemBarIcons(darkBackground = dark)
    mode?.let { chosen -> SystemNightMode(chosen) }
    CompositionLocalProvider(
        LocalAttachmentColors provides SparrowAttachmentColors
    ) {
        MaterialTheme(
            colorScheme = (if (dark) DarkColorScheme else LightColorScheme).animated(),
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/** Every colour fades to the other scheme instead of jumping. */
@Composable
private fun ColorScheme.animated(): ColorScheme =
    copy(
        primary = primary.animated(),
        onPrimary = onPrimary.animated(),
        primaryContainer = primaryContainer.animated(),
        onPrimaryContainer = onPrimaryContainer.animated(),
        inversePrimary = inversePrimary.animated(),
        secondary = secondary.animated(),
        onSecondary = onSecondary.animated(),
        secondaryContainer = secondaryContainer.animated(),
        onSecondaryContainer = onSecondaryContainer.animated(),
        tertiary = tertiary.animated(),
        onTertiary = onTertiary.animated(),
        tertiaryContainer = tertiaryContainer.animated(),
        onTertiaryContainer = onTertiaryContainer.animated(),
        background = background.animated(),
        onBackground = onBackground.animated(),
        surface = surface.animated(),
        onSurface = onSurface.animated(),
        surfaceVariant = surfaceVariant.animated(),
        onSurfaceVariant = onSurfaceVariant.animated(),
        surfaceTint = surfaceTint.animated(),
        inverseSurface = inverseSurface.animated(),
        inverseOnSurface = inverseOnSurface.animated(),
        error = error.animated(),
        onError = onError.animated(),
        errorContainer = errorContainer.animated(),
        onErrorContainer = onErrorContainer.animated(),
        outline = outline.animated(),
        outlineVariant = outlineVariant.animated(),
        scrim = scrim.animated(),
        surfaceBright = surfaceBright.animated(),
        surfaceContainer = surfaceContainer.animated(),
        surfaceContainerHigh = surfaceContainerHigh.animated(),
        surfaceContainerHighest = surfaceContainerHighest.animated(),
        surfaceContainerLow = surfaceContainerLow.animated(),
        surfaceContainerLowest = surfaceContainerLowest.animated(),
        surfaceDim = surfaceDim.animated()
    )

@Composable
private fun Color.animated(): Color =
    animateColorAsState(targetValue = this, animationSpec = tween(SCHEME_FADE_MILLISECONDS), label = "schemeColor").value

private const val SCHEME_FADE_MILLISECONDS = 600
