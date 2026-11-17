package com.cbgm.sparrow.feature.onboarding.presentation.pages.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis

/**
 * Milliseconds since the page first became the current one; everything that fades or slides in on the page reads its
 * moment from it. Read it in draw or layer blocks, so the frames redraw without recomposing. It starts again when the
 * page is composed anew after it left the screen.
 */
@Composable
fun rememberPageClock(active: Boolean): State<Long> {
    val elapsed = remember { mutableLongStateOf(0L) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        val start = withFrameMillis { frame -> frame } - elapsed.longValue
        while (elapsed.longValue < RUNNING_MILLISECONDS) {
            withFrameMillis { frame -> elapsed.longValue = frame - start }
        }
    }
    return elapsed
}

/** 0 before [startMillis], rising to 1 over [durationMillis], eased out. */
fun State<Long>.progress(
    startMillis: Long,
    durationMillis: Long
): Float {
    val linear = ((value - startMillis).toFloat() / durationMillis).coerceIn(0f, 1f)
    val remaining = 1f - linear
    return 1f - remaining * remaining * remaining
}

/** Long enough for the slowest page; the clock stops there. */
private const val RUNNING_MILLISECONDS = 8_000L
