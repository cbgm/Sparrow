package com.cbgm.securechat.feature.transport.controlplane

import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneEndpoint
import com.cbgm.securechat.core.transport.ControlPlaneHealthMonitor
import com.cbgm.securechat.core.transport.ControlPlaneStatusStore
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

class HttpControlPlaneHealthMonitor(
    private val httpClient: HttpClient,
    private val configuration: ControlPlaneConfiguration,
    private val statusStore: ControlPlaneStatusStore
) : ControlPlaneHealthMonitor {
    override suspend fun refresh() {
        coroutineScope {
            configuration.endpoints.value
                .map { endpoint ->
                    async { probe(endpoint) }
                }.awaitAll()
        }
    }

    private suspend fun probe(endpoint: ControlPlaneEndpoint) {
        val isAvailable =
            runCatching {
                withTimeout(HEALTH_TIMEOUT_MILLISECONDS) {
                    httpClient
                        .get("${endpoint.baseUrl}/health/registry")
                        .status.value in MIN_SUCCESS_STATUS..MAX_SUCCESS_STATUS
                }
            }.getOrDefault(false)

        if (isAvailable) {
            statusStore.markAvailable(endpoint)
        } else {
            statusStore.markUnreachable(endpoint)
        }
    }

    private companion object {
        const val HEALTH_TIMEOUT_MILLISECONDS = 3_000L
        const val MIN_SUCCESS_STATUS = 200
        const val MAX_SUCCESS_STATUS = 299
    }
}
