package com.cbgm.securechat.feature.transport.controlplane

import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneEndpoint
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
}
