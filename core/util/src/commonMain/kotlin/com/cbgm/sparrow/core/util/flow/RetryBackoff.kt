package com.cbgm.sparrow.core.util.flow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** The wait before the next try after [failures] failed ones: one second, doubling up to one minute. */
fun retryBackoff(failures: Long): Duration =
    (1_000L shl failures.coerceIn(0L, 6L).toInt()).coerceAtMost(60_000L).milliseconds

/**
 * Subscribes again after any failure except cancellation, waiting [retryBackoff] first. An app-lifetime
 * observer must survive a temporary error (e.g. a Room observation error) instead of stopping for the rest of
 * the process.
 */
fun <T> Flow<T>.retryWithBackoff(): Flow<T> =
    retryWhen { cause, attempt ->
        if (cause is CancellationException) throw cause
        delay(retryBackoff(attempt))
        true
    }

/**
 * Hands each latest value to [handle] until it returns true. A failed value is tried again after
 * [retryBackoff], or right away when a newer value arrives, which replaces it. [handle] itself is never
 * cancelled by a newer value, so a started workflow always runs to its end.
 */
suspend fun <T : Any> Flow<T>.collectUntilHandled(handle: suspend (T) -> Boolean): Unit =
    coroutineScope {
        val values = conflate().produceIn(this)
        var value = values.receiveCatching().getOrNull() ?: return@coroutineScope
        var open = true
        var failures = 0L
        while (true) {
            if (handle(value)) {
                failures = 0L
                value = values.receiveCatching().getOrNull() ?: return@coroutineScope
                continue
            }
            val wait = retryBackoff(failures++)
            if (!open) {
                delay(wait)
                continue
            }
            val next = withTimeoutOrNull(wait) { values.receiveCatching() } ?: continue
            if (next.isClosed) {
                open = false
                delay(wait)
            } else {
                value = next.getOrThrow()
            }
        }
    }
