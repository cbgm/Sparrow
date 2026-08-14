package com.cbgm.securechat.feature.transport.controlplane

import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneDirectorySynchronizer
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class HttpControlPlaneDirectorySynchronizer(
    private val httpClient: HttpClient,
    private val configuration: ControlPlaneConfiguration,
    private val json: Json
) : ControlPlaneDirectorySynchronizer {
    override suspend fun refresh(): Result<Int> {
        val directoryUrl = configuration.directoryUrl.value ?: return Result.success(0)
        return synchronize(directoryUrl, persistDirectoryUrl = false)
    }

    override suspend fun synchronizeFrom(url: String): Result<Int> =
        synchronize(url, persistDirectoryUrl = true)

    private suspend fun synchronize(
        url: String,
        persistDirectoryUrl: Boolean
    ): Result<Int> =
        runCatching {
            val controlPlanes = fetchDirectory(url)
            configuration.replaceDirectory(controlPlanes).getOrThrow()
            if (persistDirectoryUrl) {
                configuration.setDirectoryUrl(url).getOrThrow()
            }
            configuration.directoryBaseUrls.value.size
        }

    private suspend fun fetchDirectory(url: String): List<String> =
        withTimeout(DIRECTORY_TIMEOUT_MILLISECONDS) {
            val response = httpClient.get(url)
            check(response.status.isSuccess()) {
                "Control-plane directory returned HTTP ${response.status.value}"
            }
            parseControlPlaneDirectory(response.bodyAsText(), json)
        }

    private companion object {
        const val DIRECTORY_TIMEOUT_MILLISECONDS = 5_000L
    }
}

internal fun parseControlPlaneDirectory(
    content: String,
    json: Json
): List<String> {
    val controlPlanes =
        json.decodeFromString<ControlPlaneDirectoryDocument>(content)
            .controlPlanes
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
    require(controlPlanes.isNotEmpty()) {
        "Control-plane directory contains no addresses"
    }
    return controlPlanes
}

@Serializable
private data class ControlPlaneDirectoryDocument(
    val controlPlanes: List<String>
)
