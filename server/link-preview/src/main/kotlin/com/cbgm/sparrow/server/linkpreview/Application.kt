package com.cbgm.sparrow.server.linkpreview

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException

private const val DEFAULT_CACHE_TTL_MILLISECONDS = 6L * 60L * 60L * 1_000L

fun Application.installLinkPreviewRoutes(
    cacheTtlMilliseconds: Long = DEFAULT_CACHE_TTL_MILLISECONDS
) {
    val fetcher = LinkPreviewFetcher()
    val service = LinkPreviewService(fetcher, cacheTtlMilliseconds)

    monitor.subscribe(ApplicationStopped) {
        fetcher.close()
    }

    routing {
        post("/v1/link-preview") {
            val request = orNullOnFailure { call.receive<LinkPreviewRequest>() }
            val url = request?.url?.takeIf(String::isNotBlank)
            if (url == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val preview = orNullOnFailure { service.getPreview(url) }
            if (preview == null) {
                call.respond(HttpStatusCode.UnprocessableEntity)
                return@post
            }

            call.response.header(HttpHeaders.CacheControl, "private, max-age=300")
            call.respond(preview)
        }

        get("/v1/link-preview/images/{imageId}") {
            val imageId = call.parameters["imageId"]?.takeIf(String::isNotBlank)
            val image = imageId?.let { id -> orNullOnFailure { service.getImage(id) } }
            if (image == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.respondBytes(
                bytes = image.bytes,
                contentType = ContentType.parse(image.contentType)
            )
        }
    }
}

/** [block]'s result, or null when it failed; a cancellation still cancels. */
private suspend inline fun <T> orNullOnFailure(block: () -> T?): T? =
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        null
    }
