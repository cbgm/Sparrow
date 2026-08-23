package com.cbgm.sparrow.server.gateway

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put

internal fun Route.installBlobRoutes(
    store: BlobStore,
    permitStore: BlobUploadPermitStore,
    now: () -> Long = System::currentTimeMillis
) {
    put("/v1/blobs/{blobId}") {
        val blobId = call.parameters["blobId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        val token = call.bearerToken() ?: return@put call.respond(HttpStatusCode.Unauthorized)
        val claims = permitStore.consume(token) ?: return@put call.respond(HttpStatusCode.Unauthorized)
        val currentTime = now()
        if (
            claims.blobId != blobId ||
            claims.ticketExpiresAtEpochMilliseconds <= currentTime ||
            claims.blobExpiresAtEpochMilliseconds <= currentTime
        ) {
            return@put call.respond(HttpStatusCode.Unauthorized)
        }

        try {
            val metadata = store.store(claims, call.receiveChannel())
            call.respond(HttpStatusCode.Created, metadata)
        } catch (_: BlobTooLargeException) {
            call.respond(HttpStatusCode.PayloadTooLarge)
        } catch (_: BlobAlreadyExistsException) {
            call.respond(HttpStatusCode.Conflict)
        } catch (_: BlobStorageCapacityExceededException) {
            call.respond(HttpStatusCode.InsufficientStorage)
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest)
        }
    }

    get("/v1/blobs/{blobId}") {
        val blobId = call.parameters["blobId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val capability = call.bearerToken() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val file = store.readableBlob(blobId, capability, now()) ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respondFile(file.toFile())
    }

    delete("/v1/blobs/{blobId}") {
        val blobId = call.parameters["blobId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
        val capability = call.bearerToken() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
        if (store.delete(blobId, capability, now())) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}

private fun ApplicationCall.bearerToken(): String? =
    request.headers[HttpHeaders.Authorization]
        ?.takeIf { header -> header.startsWith("Bearer ", ignoreCase = true) }
        ?.substringAfter(' ')
        ?.takeIf(String::isNotBlank)
