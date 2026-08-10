package com.cbgm.securechat.feature.transport.controlplane

import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneDirectorySynchronizer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

class HttpControlPlaneDirectorySynchronizer(
    private val httpClient: HttpClient,
    private val configuration: ControlPlaneConfiguration
) : ControlPlaneDirectorySynchronizer {
    override suspend fun refresh(): Result<Int> =
        runCatching {
            val directoryUrl = configuration.directoryUrl.value ?: return@runCatching 0
            val response = httpClient.get(directoryUrl)
            check(response.status.isSuccess()) {
                "Control-plane directory returned HTTP ${response.status.value}"
            }
            val document = response.body<ControlPlaneDirectoryDocument>()
            configuration
                .mergeDirectory(document.controlPlanes)
                .getOrThrow()
            document.controlPlanes.distinct().size
        }
}

@Serializable
private data class ControlPlaneDirectoryDocument(
    val controlPlanes: List<String>
)
