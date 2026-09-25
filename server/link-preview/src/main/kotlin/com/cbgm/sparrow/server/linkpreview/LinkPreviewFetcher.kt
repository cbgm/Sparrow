package com.cbgm.sparrow.server.linkpreview

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.time.Duration.Companion.milliseconds

internal class LinkPreviewFetcher(
    private val httpClient: HttpClient = createLinkPreviewHttpClient()
) : AutoCloseable {
    suspend fun fetch(url: String): FetchedLinkPreview {
        val youtubeThumbnailUrl = url.youtubeThumbnailUrlOrNull() ?: return fetchHtmlPreview(url)

        val htmlPreview =
            try {
                fetchHtmlPreview(url)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }

        val youtubeMetadata =
            try {
                fetchYouTubeMetadata(url)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }

        return FetchedLinkPreview(
            url = htmlPreview?.url ?: url,
            title = youtubeMetadata?.title ?: htmlPreview?.title,
            description = htmlPreview?.description,
            siteName = "YouTube",
            imageUrl = youtubeMetadata?.thumbnailUrl ?: htmlPreview?.imageUrl ?: youtubeThumbnailUrl
        )
    }

    private suspend fun fetchHtmlPreview(url: String): FetchedLinkPreview {
        val response = requestFollowingSafeRedirects(url)
        val contentType = response.headers[HttpHeaders.ContentType].orEmpty().lowercase()
        require(contentType.startsWith("text/html") || contentType.startsWith("application/xhtml+xml")) {
            "Link-preview target is not HTML"
        }
        requireContentLengthWithinLimit(response, MAX_HTML_BYTES)

        val html = response.bodyAsText()
        require(html.length <= MAX_HTML_CHARACTERS) {
            "Link-preview HTML is too large"
        }
        return parseLinkPreviewHtml(response.call.request.url.toString(), html)
    }

    private suspend fun fetchYouTubeMetadata(url: String): YouTubeMetadata =
        withTimeout(REQUEST_TIMEOUT_MILLISECONDS.milliseconds) {
            val response =
                httpClient.get(YOUTUBE_OEMBED_URL) {
                    parameter("url", url)
                    parameter("format", "json")
                    header(HttpHeaders.UserAgent, USER_AGENT)
                    header(HttpHeaders.Accept, "application/json")
                }

            require(response.status.value in 200..299) {
                "YouTube oEmbed returned HTTP ${response.status.value}"
            }

            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            YouTubeMetadata(
                title = json["title"]?.jsonPrimitive?.contentOrNull,
                thumbnailUrl = json["thumbnail_url"]?.jsonPrimitive?.contentOrNull
            )
        }

    suspend fun fetchImage(url: String): LinkPreviewImage {
        val response = requestFollowingSafeRedirects(url)
        val contentType = response.headers[HttpHeaders.ContentType].orEmpty().substringBefore(';').trim().lowercase()
        require(contentType.startsWith("image/")) {
            "Link-preview image target is not an image"
        }
        requireContentLengthWithinLimit(response, MAX_SOURCE_IMAGE_BYTES)

        val sourceBytes = response.body<ByteArray>()
        require(sourceBytes.size <= MAX_SOURCE_IMAGE_BYTES) {
            "Link-preview image is too large"
        }

        return LinkPreviewImage(
            bytes = minimizeImage(sourceBytes),
            contentType = "image/jpeg"
        )
    }

    override fun close() {
        httpClient.close()
    }

    private suspend fun requestFollowingSafeRedirects(initialUrl: String): HttpResponse =
        withTimeout(REQUEST_TIMEOUT_MILLISECONDS.milliseconds) {
            var current = LinkPreviewUrlValidator.validate(initialUrl)
            repeat(MAX_REDIRECTS + 1) { redirectIndex ->
                val response =
                    httpClient.get(current.toString()) {
                        header(HttpHeaders.UserAgent, USER_AGENT)
                        header(HttpHeaders.Accept, "text/html,application/xhtml+xml,image/*;q=0.9,*/*;q=0.1")
                    }

                if (response.status.value in 300..399) {
                    require(redirectIndex < MAX_REDIRECTS) {
                        "Link-preview target redirected too many times"
                    }
                    val location = response.headers[HttpHeaders.Location]
                        ?: throw IllegalArgumentException("Link-preview redirect has no location")
                    current = LinkPreviewUrlValidator.validate(current.resolve(location).toString())
                } else {
                    require(response.status.value in 200..299) {
                        "Link-preview target returned HTTP ${response.status.value}"
                    }
                    return@withTimeout response
                }
            }
            error("Link-preview redirect resolution failed")
        }

    private fun requireContentLengthWithinLimit(response: HttpResponse, maximumBytes: Int) {
        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: return
        require(contentLength <= maximumBytes) {
            "Link-preview response is too large"
        }
    }
}

private fun minimizeImage(sourceBytes: ByteArray): ByteArray {
    val source =
        ImageIO.read(ByteArrayInputStream(sourceBytes))
            ?: throw IllegalArgumentException("Link-preview image could not be decoded")

    val largestDimension = maxOf(source.width, source.height)
    val scale =
        if (largestDimension > MAX_STORED_IMAGE_DIMENSION) {
            MAX_STORED_IMAGE_DIMENSION.toDouble() / largestDimension.toDouble()
        } else {
            1.0
        }

    var image =
        source.scaleToJpegImage(
            width = maxOf(1, (source.width * scale).toInt()),
            height = maxOf(1, (source.height * scale).toInt())
        )

    JPEG_QUALITIES.forEach { quality ->
        image.encodeJpeg(quality).takeIf { it.size <= MAX_STORED_IMAGE_BYTES }?.let { return it }
    }

    while (image.width > MIN_IMAGE_DIMENSION || image.height > MIN_IMAGE_DIMENSION) {
        val nextWidth = maxOf(MIN_IMAGE_DIMENSION, (image.width * IMAGE_REDUCTION_FACTOR).toInt())
        val nextHeight = maxOf(MIN_IMAGE_DIMENSION, (image.height * IMAGE_REDUCTION_FACTOR).toInt())
        if (nextWidth == image.width && nextHeight == image.height) break

        image = image.scaleToJpegImage(nextWidth, nextHeight)
        image.encodeJpeg(MIN_JPEG_QUALITY).takeIf { it.size <= MAX_STORED_IMAGE_BYTES }?.let { return it }
    }

    return image.encodeJpeg(MIN_JPEG_QUALITY).also { encoded ->
        require(encoded.size <= MAX_STORED_IMAGE_BYTES) {
            "Link-preview image cannot be reduced below the storage limit"
        }
    }
}

private fun BufferedImage.scaleToJpegImage(
    width: Int,
    height: Int
): BufferedImage {
    val target = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = target.createGraphics()
    try {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.drawImage(this, 0, 0, width, height, null)
    } finally {
        graphics.dispose()
    }
    return target
}

private fun BufferedImage.encodeJpeg(quality: Float): ByteArray {
    val writer = ImageIO.getImageWritersByFormatName("jpeg").asSequence().firstOrNull()
        ?: error("JPEG writer is unavailable")
    val output = ByteArrayOutputStream()
    val imageOutput = ImageIO.createImageOutputStream(output)
    try {
        writer.output = imageOutput
        val params = writer.defaultWriteParam
        if (params.canWriteCompressed()) {
            params.compressionMode = ImageWriteParam.MODE_EXPLICIT
            params.compressionQuality = quality
        }
        writer.write(null, IIOImage(this, null, null), params)
        imageOutput.flush()
        return output.toByteArray()
    } finally {
        writer.dispose()
        imageOutput.close()
    }
}

private fun createLinkPreviewHttpClient(): HttpClient =
    HttpClient(CIO) {
        expectSuccess = false
        followRedirects = false
    }

private const val MAX_REDIRECTS = 4
private const val MAX_HTML_BYTES = 1_048_576
private const val MAX_HTML_CHARACTERS = 1_048_576
private const val MAX_SOURCE_IMAGE_BYTES = 5 * 1_048_576
private const val MAX_STORED_IMAGE_BYTES = 80 * 1024
private const val MAX_STORED_IMAGE_DIMENSION = 480
private const val MIN_IMAGE_DIMENSION = 120
private const val IMAGE_REDUCTION_FACTOR = 0.8
private const val MIN_JPEG_QUALITY = 0.35f
private val JPEG_QUALITIES = floatArrayOf(0.72f, 0.62f, 0.52f, 0.42f)
private const val REQUEST_TIMEOUT_MILLISECONDS = 8_000L
private const val USER_AGENT = "Sparrow-LinkPreview/1.0"
private const val YOUTUBE_OEMBED_URL = "https://www.youtube.com/oembed"

private data class YouTubeMetadata(
    val title: String?,
    val thumbnailUrl: String?
)
