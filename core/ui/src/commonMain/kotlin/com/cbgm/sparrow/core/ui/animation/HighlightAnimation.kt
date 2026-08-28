package com.cbgm.sparrow.core.ui.animation

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private const val DEFAULT_HIGHLIGHT_DURATION_MILLIS = 1_000

@Composable
fun rememberHighlightColor(
    isHighlighted: Boolean,
    baseColor: Color,
    highlightColor: Color,
    durationMillis: Int = DEFAULT_HIGHLIGHT_DURATION_MILLIS
): Color {
    val animatedColor = remember(baseColor) { Animatable(baseColor) }

    LaunchedEffect(isHighlighted, baseColor, highlightColor, durationMillis) {
        if (isHighlighted) {
            animatedColor.snapTo(highlightColor)
            animatedColor.animateTo(
                targetValue = baseColor,
                animationSpec =
                    tween(
                        durationMillis = durationMillis,
                        easing = FastOutSlowInEasing
                    )
            )
        } else {
            animatedColor.snapTo(baseColor)
        }
    }

    return animatedColor.value
}
