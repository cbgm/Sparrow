package com.cbgm.securechat.feature.transport.controlplane

import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneEndpoint
import com.cbgm.securechat.core.transport.ControlPlaneReachability
import com.cbgm.securechat.core.transport.ControlPlaneStatusStore

class ControlPlaneRequestRouter(
    private val configuration: ControlPlaneConfiguration,
    private val statusStore: ControlPlaneStatusStore
) {
    suspend fun <T> execute(
        block: suspend (ControlPlaneEndpoint) -> T
    ): Result<T> {
        var lastError: Throwable? = null

        for (endpoint in configuration.orderedEndpoints()) {
            val result = runCatching { block(endpoint) }
            if (result.isSuccess) {
                statusStore.markAvailable(endpoint)
                configuration.markActive(endpoint)
                return result
            }
            statusStore.markUnreachable(endpoint)
            lastError = result.exceptionOrNull()
        }

        return Result.failure(
            lastError ?: IllegalStateException("No control plane is configured")
        )
    }

    suspend fun executeAll(
        block: suspend (ControlPlaneEndpoint) -> Unit
    ): Result<Unit> {
        val unavailable =
            statusStore.statuses.value
                .filter { status -> status.reachability == ControlPlaneReachability.UNREACHABLE }
                .mapTo(mutableSetOf()) { status -> status.endpoint }
        val endpoints =
            configuration
                .orderedEndpoints()
                .distinct()
                .filterNot(unavailable::contains)
        if (endpoints.isEmpty()) {
            return Result.failure(IllegalStateException("No reachable control plane is configured"))
        }

        val failures = mutableListOf<String>()
        endpoints.forEach { endpoint ->
            runCatching { block(endpoint) }
                .onSuccess {
                    statusStore.markAvailable(endpoint)
                }.onFailure { error ->
                    statusStore.markUnreachable(endpoint)
                    failures += "${endpoint.baseUrl}: ${error.message ?: error::class.simpleName}"
                }
        }

        return if (failures.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(
                IllegalStateException(
                    "Control-plane synchronization failed: ${failures.joinToString()}"
                )
            )
        }
    }
}
