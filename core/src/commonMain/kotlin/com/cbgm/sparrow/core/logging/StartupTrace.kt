package com.cbgm.sparrow.core.logging

import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Temporary startup diagnostics. All timestamps share one monotonic origin. */
object StartupTrace {
    private val logger = SparrowLog.withTag("StartupTrace")

    @Volatile
    private var origin: TimeMark? = null

    fun begin() {
        origin = TimeSource.Monotonic.markNow()
        event("AppViewModel created; startup trace begins")
    }

    fun event(message: String) {
        val elapsed = origin?.elapsedNow()?.inWholeMilliseconds
        logger.info { "[+${elapsed ?: 0}ms] $message" }
    }

    suspend fun <T> measure(stage: String, block: suspend () -> T): T {
        event("$stage START")
        val started = TimeSource.Monotonic.markNow()
        return try {
            block().also {
                event("$stage END duration=${started.elapsedNow().inWholeMilliseconds}ms")
            }
        } catch (error: Throwable) {
            event("$stage FAILED after=${started.elapsedNow().inWholeMilliseconds}ms: ${error.message}")
            throw error
        }
    }
}
