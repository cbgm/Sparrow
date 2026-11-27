package com.cbgm.sparrow.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

object DarkColors {
    // Brand accent.
    val Primary = Color(0xFF35E6FF)
    val OnPrimary = Color(0xFF000000)
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

    /** Lines between list items and under fields: only just lighter than the background. */
    val Divider = Color(0xFF10273E)
    val Warning = Color(0xFFE0B050)
    val ErrorBright = Color(0xFFE07070)

    val TintBase = Color(0xFF0B0F16)
    val Scrim = Color(0xFF000000)

    val InverseSurface = Color(0xFFE6EEF4)
    val InverseOnSurface = Color(0xFF10283D)
}

object LightColors {
    // Brand accent.
    val Primary = Color(0xFF0099B8)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFC4F3FF)
    val OnPrimaryContainer = Color(0xFF002028)
    val InversePrimary = Color(0xFF35E6FF)

    // Supporting accent.
    val Secondary = Color(0xFF47606A)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFD3E5ED)
    val OnSecondaryContainer = Color(0xFF091E26)

    // Positive / verified / available state.
    val Tertiary = Color(0xFF107A55)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFBDF5DD)
    val OnTertiaryContainer = Color(0xFF002114)

    // Destructive / unavailable / failure state.
    val Error = Color(0xFFC62828)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD8)
    val OnErrorContainer = Color(0xFF410004)

    // App canvas and neutral surface hierarchy.
    val Background = Color(0xFFEEF2F5)
    val OnBackground = Color(0xFF0B1924)

    val Surface = Color(0xFFEEF2F5)
    val OnSurface = Color(0xFF0B1924)
    val SurfaceVariant = Color(0xFFDCE3E8)
    val OnSurfaceVariant = Color(0xFF43525E)

    val SurfaceDim = Color(0xFFD2DBE1)
    val SurfaceBright = Color(0xFFFFFFFF)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF7F9FB)
    val SurfaceContainer = Color(0xFFFFFFFF)
    val SurfaceContainerHigh = Color(0xFFE5EBEF)
    val SurfaceContainerHighest = Color(0xFFDCE3E8)

    val Outline = Color(0xFF6C7B87)
    val OutlineVariant = Color(0xFFC4CFD6)

    val Divider = Color(0xFFE2E8ED)
    val Warning = Color(0xFFB06D00)
    val ErrorBright = Color(0xFFE03535)

    val TintBase = Color(0xFFE4E9ED)
    val Scrim = Color(0xFF000000)

    val InverseSurface = Color(0xFF132330)
    val InverseOnSurface = Color(0xFFEEF2F5)
}

private val ColorScheme.isLight: Boolean
    get() = background.luminance() > LIGHT_BACKGROUND_LUMINANCE

private const val LIGHT_BACKGROUND_LUMINANCE = 0.5f

@Composable
@ReadOnlyComposable
fun Color.asTint(alpha: Float): Color =
    copy(alpha = alpha).compositeOver(if (MaterialTheme.colorScheme.isLight) LightColors.TintBase else DarkColors.TintBase)

/** The faint line between list items and under fields. */
val ColorScheme.divider: Color
    get() = if (isLight) LightColors.Divider else DarkColors.Divider

/** Amber for things that wait: queued messages, a pending request. */
val ColorScheme.warning: Color
    get() = if (isLight) LightColors.Warning else DarkColors.Warning

val ColorScheme.errorBright: Color
    get() = if (isLight) LightColors.ErrorBright else DarkColors.ErrorBright

/**
 * Fixed colors required by functional rendering.
 *
 * These intentionally do not follow the application theme.
 */
object FunctionalColors {
    val MediaBackground = Color.Black
    val MediaForeground = Color.White

    val QrCodeForeground = Color.Black
    val QrCodeBackground = Color.White
}
