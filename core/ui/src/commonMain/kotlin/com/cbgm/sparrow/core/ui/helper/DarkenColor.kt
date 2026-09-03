package com.cbgm.sparrow.core.ui.helper

import androidx.compose.ui.graphics.Color

fun Color.darker(factor: Float = 0.8f): Color {
    val darkenBy = factor.coerceIn(0f, 1f)
    return this.copy(
        red = this.red * darkenBy,
        green = this.green * darkenBy,
        blue = this.blue * darkenBy,
        alpha = this.alpha
    )
}
