package com.cbgm.sparrow.feature.transport.controlplane

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.core.transport.ControlPlaneReachability
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import kotlinx.coroutines.CancellationException

class ControlPlaneRequestRouter(
    private val configuration: ControlPlaneConfiguration,
    private val statusStore: ControlPlaneStatusStore
) {
    private val logger = SparrowLog.withTag("ControlPlaneRequestRouter")

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
            lastError = result.exceptionOrNull()
            if (lastError is CancellationException) throw lastError
            statusStore.markUnreachable(endpoint)
            // Endpoint failover is expected. This is not an app-level error when
            // another configured control plane can complete the request.
            logger.debug {
                "Control-plane endpoint unavailable (${endpoint.baseUrl}); trying fallback: " +
                    (lastError?.message ?: "Unknown cause")
            }
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
            // Keep the failed Result: push registration must retry when connectivity returns.
            // An unavailable control plane is reported by the existing offline/online hint.
            return Result.failure(ControlPlaneUnavailableException("No reachable control plane is configured"))
        }

        val failures = mutableListOf<String>()
        var rejectedRequest: ControlPlaneRequestRejectedException? = null
        var lastUnavailableCause: Throwable? = null
        val successfulEndpoints = mutableListOf<ControlPlaneEndpoint>()
        endpoints.forEach { endpoint ->
            runCatching { block(endpoint) }
                .onSuccess {
                    statusStore.markAvailable(endpoint)
                    successfulEndpoints += endpoint
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    if (error is ControlPlaneRequestRejectedException) {
                        // A response from the server is not a connectivity failure.
                        statusStore.markAvailable(endpoint)
                        if (rejectedRequest == null) rejectedRequest = error
                    } else {
                        // Individual endpoint failures are expected during failover.
                        logger.debug {
                            "Control-plane endpoint unavailable (${endpoint.baseUrl}); " +
                                "continuing with remaining endpoints: " +
                                (error.message ?: error::class.simpleName)
                        }
                        statusStore.markUnreachable(endpoint)
                        lastUnavailableCause = error
                        failures += "${endpoint.baseUrl}: ${error.message ?: error::class.simpleName}"
                    }
                }
        }

        return if (successfulEndpoints.isNotEmpty()) {
            // Do not keep pointing at a failed endpoint once a working alternative
            // has actually completed the operation.
            if (configuration.activeEndpoint.value !in successfulEndpoints) {
                configuration.markActive(successfulEndpoints.first())
            }
            Result.success(Unit)
        } else {
            Result.failure(
                rejectedRequest ?: ControlPlaneUnavailableException(
                    "All control-plane requests failed: ${failures.joinToString()}",
                    lastUnavailableCause
                )
            )
        }
    }
}
