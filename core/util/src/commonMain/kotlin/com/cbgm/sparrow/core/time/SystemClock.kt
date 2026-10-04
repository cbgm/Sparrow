package com.cbgm.sparrow.core.time

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object SystemClock {
    @OptIn(ExperimentalTime::class)
    fun nowEpochMilliseconds(): Long = Clock.System.now().toEpochMilliseconds()
}
