package com.cbgm.sparrow.feature.polls.presentation.message

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cbgm.sparrow.core.messagepart.ui.model.PollUi
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.polls.util.PollConstants.MILLISECONDS_PER_MINUTE
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun rememberPollNowEpochMilliseconds(part: PollUi): Long {
    var nowEpochMilliseconds by remember(
        part.instanceKey,
        part.expiresAtEpochMilliseconds,
        part.closedAtEpochMilliseconds
    ) {
        mutableLongStateOf(SystemClock.nowEpochMilliseconds())
    }

    LaunchedEffect(
        part.instanceKey,
        part.expiresAtEpochMilliseconds,
        part.closedAtEpochMilliseconds
    ) {
        val expiresAt = part.expiresAtEpochMilliseconds ?: return@LaunchedEffect
        if (part.closedAtEpochMilliseconds != null) return@LaunchedEffect

        while (true) {
            val now = SystemClock.nowEpochMilliseconds()
            nowEpochMilliseconds = now
            val remaining = expiresAt - now
            if (remaining <= 0L) break

            val untilNextMinute = remaining % MILLISECONDS_PER_MINUTE
            delay(
                (
                    if (untilNextMinute == 0L) {
                        MILLISECONDS_PER_MINUTE
                    } else {
                        untilNextMinute
                    }
                ).milliseconds
            )
        }
    }

    return nowEpochMilliseconds
}
