package com.cbgm.sparrow.core.ui.theme

import androidx.compose.ui.graphics.Color

object Colors {
    // Brand accent.
    val Primary = Color(0xFF35E6FF)
    val OnPrimary = Color(0xFF071A2E)
    val PrimaryContainer = Color(0xFF0C4450)
    val OnPrimaryContainer = Color(0xFFC8F9FF)
    val InversePrimary = Color(0xFF006A78)

    // Supporting accent.
    val Secondary = Color(0xFF8AB9C7)
    val OnSecondary = Color(0xFF071A2E)
    val SecondaryContainer = Color(0xFF173848)
    val OnSecondaryContainer = Color(0xFFD7EDF3)

    // Positive / verified / available state.
    val Tertiary = Color(0xFF4FD3A3)
    val OnTertiary = Color(0xFF071A2E)
    val TertiaryContainer = Color(0xFF123D32)
    val OnTertiaryContainer = Color(0xFFC8F5E2)

    // Destructive / unavailable / failure state.
    val Error = Color(0xFFB65353)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFF4A2025)
    val OnErrorContainer = Color(0xFFFFDAD8)

    // App canvas and neutral surface hierarchy.
    val Background = Color(0xFF071A2E)
    val OnBackground = Color(0xFFF6FAFE)

    val Surface = Color(0xFF0B2035)
    val OnSurface = Color(0xFFF6FAFE)
    val SurfaceVariant = Color(0xFF17324D)
    val OnSurfaceVariant = Color(0xFFB7C8D6)

    val SurfaceDim = Color(0xFF071A2E)
    val SurfaceBright = Color(0xFF24435F)
    val SurfaceContainerLowest = Color(0xFF071A2E)
    val SurfaceContainerLow = Color(0xFF0B2035)
    val SurfaceContainer = Color(0xFF102A46)
    val SurfaceContainerHigh = Color(0xFF17324D)
    val SurfaceContainerHighest = Color(0xFF1D3A56)

    val Outline = Color(0xFF668095)
    val OutlineVariant = Color(0xFF2D4A62)
    val Scrim = Color(0xFF000000)

    val InverseSurface = Color(0xFFE6EEF4)
    val InverseOnSurface = Color(0xFF10283D)
}

/** Theme-independent colors required by functional content rather than UI styling. */
object FunctionalColors {
    val MediaBackground = Color(0xFF000000)
    val MediaForeground = Color(0xFFFFFFFF)
    val QrCodeForeground = Color(0xFF000000)
    val QrCodeBackground = Color(0xFFFFFFFF)
}
