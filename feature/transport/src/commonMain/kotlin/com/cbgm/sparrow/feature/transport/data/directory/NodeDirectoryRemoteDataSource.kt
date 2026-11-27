package com.cbgm.sparrow.feature.transport.data.directory

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

internal interface NodeDirectoryRemoteDataSource {
    suspend fun fetch(registryBaseUrl: String): Result<String>
}

internal class HttpNodeDirectoryRemoteDataSource(
    private val httpClient: HttpClient
) : NodeDirectoryRemoteDataSource {
    override suspend fun fetch(registryBaseUrl: String): Result<String> =
        runCatching {
            httpClient
                .get("${registryBaseUrl.trimEnd('/')}/v1/nodes")
                .bodyAsText()
        }
}
