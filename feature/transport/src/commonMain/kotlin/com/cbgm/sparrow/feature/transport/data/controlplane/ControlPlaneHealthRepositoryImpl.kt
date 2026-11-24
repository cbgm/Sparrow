package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneHealthRepository
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneStatusRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

internal class ControlPlaneHealthRepositoryImpl(
    private val httpClient: HttpClient,
    private val configuration: ControlPlaneEndpointStore,
    private val statusStore: ControlPlaneStatusRepository
) : ControlPlaneHealthRepository {
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
                withTimeout(HEALTH_TIMEOUT) {
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
        val HEALTH_TIMEOUT = 1.seconds
        const val MIN_SUCCESS_STATUS = 200
        const val MAX_SUCCESS_STATUS = 299
    }
}
