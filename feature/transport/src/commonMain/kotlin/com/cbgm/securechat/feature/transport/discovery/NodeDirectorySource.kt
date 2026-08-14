package com.cbgm.securechat.feature.transport.discovery

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

interface NodeDirectorySource {
    suspend fun fetch(registryBaseUrl: String): Result<String>
}

class HttpNodeDirectorySource(
    private val httpClient: HttpClient
) : NodeDirectorySource {
    override suspend fun fetch(registryBaseUrl: String): Result<String> =
        runCatching {
            httpClient
                .get("${registryBaseUrl.trimEnd('/')}/v1/nodes")
                .bodyAsText()
        }
}
