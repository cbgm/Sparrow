package com.cbgm.sparrow.feature.transport.data.controlplane

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal interface NodeControlPlaneDirectoryRemoteDataSource {
    suspend fun fetch(websocketUrl: String): Result<List<String>>
}

internal class HttpNodeControlPlaneDirectoryRemoteDataSource(
    private val httpClient: HttpClient
) : NodeControlPlaneDirectoryRemoteDataSource {
    override suspend fun fetch(websocketUrl: String): Result<List<String>> =
        runCatching {
            val nodeBaseUrl = websocketUrl.toHttpBaseUrl()
            val response = httpClient.get("$nodeBaseUrl/v1/control-planes")
            check(response.status.isSuccess()) {
                "Node control-plane discovery returned HTTP ${response.status.value}"
            }
            response.body<NodeControlPlaneDirectoryDto>().controlPlanes
        }

    private fun String.toHttpBaseUrl(): String {
        val scheme =
            when {
                startsWith("wss://") -> "https://"
                startsWith("ws://") -> "http://"
                else -> error("Node endpoint must use ws:// or wss://")
            }
        val authority = substringAfter("://").substringBefore('/')
        require(authority.isNotBlank()) {
            "Node endpoint does not contain an authority"
        }
        return "$scheme$authority"
    }
}

@Serializable
private data class NodeControlPlaneDirectoryDto(
    val controlPlanes: List<String>
)
