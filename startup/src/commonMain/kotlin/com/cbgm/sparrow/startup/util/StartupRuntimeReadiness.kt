package com.cbgm.sparrow.startup.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/** Application-scoped signal: required local initialization has completed (or failed). */
class StartupRuntimeReadiness {
    private sealed interface Status {
        data object Pending : Status

        data object Ready : Status

        data class Failed(
            val cause: Throwable
        ) : Status
    }

    private val status = MutableStateFlow<Status>(Status.Pending)

    fun markReady() {
        status.value = Status.Ready
    }

    fun markFailed(cause: Throwable) {
        status.value = Status.Failed(cause)
    }

    suspend fun awaitReady() {
        when (val current = status.first { it !is Status.Pending }) {
            Status.Ready -> Unit
            is Status.Failed -> throw current.cause
            Status.Pending -> error("The runtime is still initializing")
        }
    }
}
