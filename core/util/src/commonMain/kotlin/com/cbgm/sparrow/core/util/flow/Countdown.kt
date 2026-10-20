package com.cbgm.sparrow.core.util.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Emits [from], [from] - 1, ... down to 0, one value per [step]. */
fun countdown(
    from: Int,
    step: Duration = 1.seconds
): Flow<Int> =
    flow {
        for (value in from downTo 0) {
            emit(value)
            if (value > 0) delay(step)
        }
    }
