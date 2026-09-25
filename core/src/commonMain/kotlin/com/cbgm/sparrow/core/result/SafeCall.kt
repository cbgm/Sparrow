package com.cbgm.sparrow.core.result

import com.cbgm.sparrow.core.logging.SparrowLog
import kotlinx.coroutines.CancellationException

suspend inline fun <T> safeSuspendCall(
    // Only use for an explicitly handled, expected protocol/application state.
    // The failure remains in Result so callers must still handle it.
    crossinline expectedFailure: (Exception) -> Boolean = { false },
    crossinline block: suspend () -> T
): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        if (!expectedFailure(error)) {
            val logger = SparrowLog.withTag("safeSuspendCall")
            logger.error(throwable = error, message = { error.message ?: "" })
        }
        Result.failure(exception = error)
    }
