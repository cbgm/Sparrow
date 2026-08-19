package com.cbgm.sparrow.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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
   ├─ onPrimary = dark navy content
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
        primary = Colors.Primary,
        onPrimary = Colors.OnPrimary,
        primaryContainer = Colors.PrimaryContainer,
        onPrimaryContainer = Colors.OnPrimaryContainer,
        inversePrimary = Colors.InversePrimary,
        secondary = Colors.Secondary,
        onSecondary = Colors.OnSecondary,
        secondaryContainer = Colors.SecondaryContainer,
        onSecondaryContainer = Colors.OnSecondaryContainer,
        tertiary = Colors.Tertiary,
        onTertiary = Colors.OnTertiary,
        tertiaryContainer = Colors.TertiaryContainer,
        onTertiaryContainer = Colors.OnTertiaryContainer,
        error = Colors.Error,
        onError = Colors.OnError,
        errorContainer = Colors.ErrorContainer,
        onErrorContainer = Colors.OnErrorContainer,
        background = Colors.Background,
        onBackground = Colors.OnBackground,
        surface = Colors.Surface,
        onSurface = Colors.OnSurface,
        surfaceVariant = Colors.SurfaceVariant,
        onSurfaceVariant = Colors.OnSurfaceVariant,
        surfaceTint = Colors.Primary,
        inverseSurface = Colors.InverseSurface,
        inverseOnSurface = Colors.InverseOnSurface,
        outline = Colors.Outline,
        outlineVariant = Colors.OutlineVariant,
        scrim = Colors.Scrim,
        surfaceBright = Colors.SurfaceBright,
        surfaceDim = Colors.SurfaceDim,
        surfaceContainer = Colors.SurfaceContainer,
        surfaceContainerHigh = Colors.SurfaceContainerHigh,
        surfaceContainerHighest = Colors.SurfaceContainerHighest,
        surfaceContainerLow = Colors.SurfaceContainerLow,
        surfaceContainerLowest = Colors.SurfaceContainerLowest
    )

@Composable
fun SparrowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
