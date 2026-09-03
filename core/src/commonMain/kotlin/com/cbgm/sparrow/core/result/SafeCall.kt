package com.cbgm.sparrow.core.result

import kotlinx.coroutines.CancellationException

suspend inline fun <T> safeSuspendCall(
    crossinline block: suspend () -> T
): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(exception = error)
    }
