package com.cbgm.sparrow.core.result

import com.cbgm.sparrow.core.logging.SparrowLog
import kotlinx.coroutines.CancellationException

suspend inline fun <T> safeSuspendCall(
    crossinline expectedFailure: (Exception) -> Boolean = { false },
    logFailure: Boolean = true,
    crossinline block: suspend () -> T
): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        if (logFailure && !expectedFailure(error)) {
            val logger = SparrowLog.withTag("safeSuspendCall")
            logger.error(throwable = error, message = { error.message ?: "" })
        }
        Result.failure(exception = error)
    }

inline fun <T> safeCall(
    crossinline expectedFailure: (Exception) -> Boolean = { false },
    logFailure: Boolean = true,
    block: () -> T
): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        if (logFailure && !expectedFailure(error)) {
            val logger = SparrowLog.withTag("safeCall")
            logger.error(throwable = error, message = { error.message ?: "" })
        }
        Result.failure(exception = error)
    }

inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    fold(onSuccess = transform, onFailure = { error -> Result.failure(error) })
