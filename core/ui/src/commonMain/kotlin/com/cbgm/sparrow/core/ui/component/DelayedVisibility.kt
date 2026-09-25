package com.cbgm.sparrow.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Avoid flashing transient UI (for example, a loading or empty state) during a fast initial read.
 * The delay is cancelled if [visible] becomes false; actual content should never use this helper.
 */
@Composable
fun rememberDelayedVisibility(
    visible: Boolean,
    delayMillis: Long = 300L
): Boolean {
    // Keying the state prevents a stale `true` from being rendered for one frame
    // when visibility flips off and immediately back on.
    var delayElapsed by remember(visible, delayMillis) { mutableStateOf(false) }

    LaunchedEffect(visible, delayMillis) {
        if (visible) {
            delay(delayMillis)
            delayElapsed = true
        }
    }

    return visible && delayElapsed
}
