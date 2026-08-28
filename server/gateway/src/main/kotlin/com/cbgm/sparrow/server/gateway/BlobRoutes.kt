package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.BlobUploadTicketClaims
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
    installBlobUploadRoute(store, permitStore, now)
    installBlobDownloadRoute(store, now)
    installBlobDeleteRoute(store, now)
}

private fun Route.installBlobUploadRoute(
    store: BlobStore,
    permitStore: BlobUploadPermitStore,
    now: () -> Long
) {
    put("/v1/blobs/{blobId}") {
        val blobId = call.parameters["blobId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        val token = call.bearerToken() ?: return@put call.respond(HttpStatusCode.Unauthorized)
        val claims = permitStore.consume(token) ?: return@put call.respond(HttpStatusCode.Unauthorized)
        if (!claims.isValidFor(blobId, now())) {
            return@put call.respond(HttpStatusCode.Unauthorized)
        }

        runCatching {
            store.store(claims, call.receiveChannel())
        }.fold(
            onSuccess = { metadata -> call.respond(HttpStatusCode.Created, metadata) },
            onFailure = { error -> call.respond(error.blobUploadStatus()) }
        )
    }
}

private fun Route.installBlobDownloadRoute(
    store: BlobStore,
    now: () -> Long
) {
    get("/v1/blobs/{blobId}") {
        val blobId = call.parameters["blobId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val capability = call.bearerToken() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val file =
            store.readableBlob(blobId, capability, now())
                ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respondFile(file.toFile())
    }
}

private fun Route.installBlobDeleteRoute(
    store: BlobStore,
    now: () -> Long
) {
    delete("/v1/blobs/{blobId}") {
        val blobId = call.parameters["blobId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
        val capability = call.bearerToken() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
        val status =
            if (store.delete(blobId, capability, now())) {
                HttpStatusCode.NoContent
            } else {
                HttpStatusCode.NotFound
            }
        call.respond(status)
    }
}

private fun BlobUploadTicketClaims.isValidFor(
    blobId: String,
    nowEpochMilliseconds: Long
): Boolean =
    this.blobId == blobId &&
        ticketExpiresAtEpochMilliseconds > nowEpochMilliseconds &&
        blobExpiresAtEpochMilliseconds > nowEpochMilliseconds

private fun Throwable.blobUploadStatus(): HttpStatusCode =
    when (this) {
        is BlobTooLargeException -> HttpStatusCode.PayloadTooLarge
        is BlobAlreadyExistsException -> HttpStatusCode.Conflict
        is BlobStorageCapacityExceededException -> HttpStatusCode.InsufficientStorage
        is IllegalArgumentException -> HttpStatusCode.BadRequest
        else -> throw this
    }

private fun ApplicationCall.bearerToken(): String? =
    request.headers[HttpHeaders.Authorization]
        ?.takeIf { header -> header.startsWith("Bearer ", ignoreCase = true) }
        ?.substringAfter(' ')
        ?.takeIf(String::isNotBlank)
