package com.cbgm.sparrow.feature.linkpreview.data.datasource

import com.cbgm.sparrow.core.transport.TransportDiagnosticsProvider
import com.cbgm.sparrow.feature.linkpreview.data.model.LinkPreviewDto
import com.cbgm.sparrow.feature.linkpreview.data.model.LinkPreviewRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class RemoteLinkPreviewDataSource(
    private val httpClient: HttpClient,
    private val transportDiagnosticsProvider: TransportDiagnosticsProvider
) {
    suspend fun getPreview(url: String): LinkPreviewDto {
        val nodeBaseUrl = currentNodeBaseUrl()
        val preview =
            httpClient
                .post("$nodeBaseUrl/v1/link-preview") {
                    contentType(ContentType.Application.Json)
                    setBody(LinkPreviewRequestDto(url))
                }.body<LinkPreviewDto>()

        val imageBytes =
            preview.imagePath
                ?.let { path -> "$nodeBaseUrl/${path.trimStart('/')}" }
                ?.let { imageUrl ->
                    val response = httpClient.get(imageUrl)
                    if (response.status.isSuccess()) {
                        response.body<ByteArray>()
                    } else {
                        null
                    }
                }
                ?.takeIf { bytes -> bytes.size <= MAX_LINK_PREVIEW_IMAGE_BYTES }

        return preview.copy(imageBytes = imageBytes)
    }

    private fun currentNodeBaseUrl(): String {
        val websocketUrl =
            transportDiagnosticsProvider.diagnostics.value.currentWebSocketUrl
                ?: error("No connected node is available")

        val httpUrl =
            when {
                websocketUrl.startsWith("wss://") -> "https://${websocketUrl.removePrefix("wss://")}"
                websocketUrl.startsWith("ws://") -> "http://${websocketUrl.removePrefix("ws://")}"
                else -> error("Unsupported gateway WebSocket URL: $websocketUrl")
            }

        return httpUrl.substringBefore("/v1/gateway").trimEnd('/')
    }
}

private const val MAX_LINK_PREVIEW_IMAGE_BYTES = 80 * 1024
