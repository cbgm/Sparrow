package com.cbgm.sparrow.feature.transport.data.directory

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

internal interface NodeDirectorySource {
    suspend fun fetch(registryBaseUrl: String): Result<String>
}

internal class HttpNodeDirectorySource(
    private val httpClient: HttpClient
) : NodeDirectorySource {
    override suspend fun fetch(registryBaseUrl: String): Result<String> =
        runCatching {
            httpClient
                .get("${registryBaseUrl.trimEnd('/')}/v1/nodes")
                .bodyAsText()
        }
}
