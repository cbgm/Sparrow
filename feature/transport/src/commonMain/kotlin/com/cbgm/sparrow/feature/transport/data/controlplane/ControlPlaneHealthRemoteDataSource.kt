package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

internal class ControlPlaneHealthRemoteDataSource(
    private val httpClient: HttpClient
) {
    suspend fun isAvailable(endpoint: ControlPlaneEndpoint): Boolean =
        runCatching {
            withTimeout(HEALTH_TIMEOUT) {
                httpClient.get("${endpoint.baseUrl}/health/registry").status.value in MIN_SUCCESS_STATUS..MAX_SUCCESS_STATUS
            }
        }.getOrDefault(false)

    private companion object {
        val HEALTH_TIMEOUT = 1.seconds
        const val MIN_SUCCESS_STATUS = 200
        const val MAX_SUCCESS_STATUS = 299
    }
}
