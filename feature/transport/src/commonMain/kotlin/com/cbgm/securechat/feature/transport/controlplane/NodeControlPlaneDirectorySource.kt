package com.cbgm.securechat.feature.transport.controlplane

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

interface NodeControlPlaneDirectorySource {
    suspend fun fetch(websocketUrl: String): Result<List<String>>
}

class HttpNodeControlPlaneDirectorySource(
    private val httpClient: HttpClient
) : NodeControlPlaneDirectorySource {
    override suspend fun fetch(websocketUrl: String): Result<List<String>> =
        runCatching {
            val nodeBaseUrl = websocketUrl.toHttpBaseUrl()
            val response = httpClient.get("$nodeBaseUrl/v1/control-planes")
            check(response.status.isSuccess()) {
                "Node control-plane discovery returned HTTP ${response.status.value}"
            }
            response.body<NodeControlPlaneDirectory>().controlPlanes
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
private data class NodeControlPlaneDirectory(
    val controlPlanes: List<String>
)
