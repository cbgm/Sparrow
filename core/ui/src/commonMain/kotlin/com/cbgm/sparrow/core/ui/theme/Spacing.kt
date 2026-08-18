package com.cbgm.sparrow.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val base: Dp = 8.dp
) {
    operator fun times(multiplier: Int): Dp = base * multiplier

    operator fun times(multiplier: Float): Dp = base * multiplier.toInt()

    val micro = 4.dp
    val small = 16.dp
    val medium = 24.dp
    val large = 32.dp
    val screenPadding = 24.dp
}

val LocalSpacing = compositionLocalOf { Spacing() }

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
