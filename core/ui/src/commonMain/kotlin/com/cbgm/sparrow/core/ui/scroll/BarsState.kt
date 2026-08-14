package com.cbgm.sparrow.core.ui.scroll

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.cbgm.sparrow.core.ui.theme.Alpha

@Stable
data class BarsState(
    val topBarAlpha: Float,
    val bottomBarAlpha: Float
)

@Composable
fun rememberBarsState(
    state: LazyListState,
    fadedAlpha: Float = Alpha.OpaqueBar
): BarsState =
    rememberBarsState(
        canScrollBackward = {
            state.canScrollBackward
        },
        canScrollForward = {
            state.canScrollForward
        },
        fadedAlpha = fadedAlpha
    )

@Composable
fun rememberBarsState(
    state: ScrollState,
    fadedAlpha: Float = Alpha.OpaqueBar
): BarsState =
    rememberBarsState(
        canScrollBackward = {
            state.canScrollBackward
        },
        canScrollForward = {
            state.canScrollForward
        },
        fadedAlpha = fadedAlpha
    )

@Composable
private fun rememberBarsState(
    canScrollForward: () -> Boolean,
    canScrollBackward: () -> Boolean,
    fadedAlpha: Float
): BarsState {
    val contentIsScrollable by remember {
        derivedStateOf {
            canScrollForward() || canScrollBackward()
        }
    }

    val barAlpha by animateFloatAsState(
        targetValue =
            if (contentIsScrollable) {
                fadedAlpha
            } else {
                1f
            },
        label = "BarsAlpha"
    )

    return BarsState(
        topBarAlpha = barAlpha,
        bottomBarAlpha = barAlpha
    )
}
