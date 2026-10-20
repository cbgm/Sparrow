package com.cbgm.sparrow.server.gateway

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

internal suspend fun <T> withRealTimeout(block: suspend () -> T): T =
    withContext(Dispatchers.Default) {
        withTimeout(5.seconds) { block() }
    }
