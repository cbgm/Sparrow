package com.cbgm.sparrow.feature.linkpreview.data.datasource

import com.cbgm.sparrow.feature.linkpreview.data.model.LinkPreviewDto
import com.cbgm.sparrow.feature.linkpreview.data.model.LinkPreviewRequestDto
import com.cbgm.sparrow.feature.transport.TransportDiagnosticsProvider
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
        val nodeBaseUrl =
            transportDiagnosticsProvider.diagnostics.value.currentNodeBaseUrl
                ?: error("No connected node is available")
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
}

private const val MAX_LINK_PREVIEW_IMAGE_BYTES = 80 * 1024
