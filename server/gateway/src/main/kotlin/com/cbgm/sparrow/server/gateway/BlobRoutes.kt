package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.BlobChunkStored
import com.cbgm.sparrow.server.protocol.BlobChunksResponse
import com.cbgm.sparrow.server.protocol.BlobUploadTicketClaims
import com.cbgm.sparrow.server.protocol.ErrorResponse
import com.cbgm.sparrow.server.protocol.OpenBlobRequest
import com.cbgm.sparrow.server.protocol.OpenBlobResponse
import com.cbgm.sparrow.server.security.CreationWorkCheck
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray

internal fun Route.installBlobRoutes(
    store: BlobStore,
    permitStore: BlobUploadPermitStore,
    now: () -> Long = System::currentTimeMillis
) {
    installBlobUploadRoute(store, permitStore, now)
    installBlobDownloadRoute(store, now)
    installBlobDeleteRoute(store, now)
}

/**
 * Blobs that travel in chunks through the app's circuits: opened with a proof of work instead of a ticket from a
 * signed-in session, then uploaded and read chunk by chunk ([BlobStore.CHUNK_BYTES]). The node learns nothing about
 * who sends or reads them.
 */
internal fun Route.installChunkedBlobRoutes(
    store: BlobStore,
    nodeId: String,
    creationWork: CreationWorkCheck,
    maximumRetentionMilliseconds: Long,
    now: () -> Long = System::currentTimeMillis
) {
    post("/v1/blobs") {
        val request = runCatching { call.receive<OpenBlobRequest>() }.getOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
        val openedAt = now()
        val uploadExpiresAt = minOf(openedAt + CHUNKED_UPLOAD_WINDOW_MILLISECONDS, request.expiresAtEpochMilliseconds - 1L)
        when {
            !creationWork.accepts(request.creationWork) ->
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("BLOB_WORK_REQUIRED", "A valid proof of work is required"))
            request.expiresAtEpochMilliseconds !in (openedAt + 1L)..(openedAt + maximumRetentionMilliseconds) || uploadExpiresAt <= openedAt ->
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_BLOB_EXPIRY", "Requested blob retention is not allowed"))
            else ->
                runCatching { store.open(request, uploadExpiresAt) }.fold(
                    onSuccess = { call.respond(HttpStatusCode.Created, OpenBlobResponse(nodeId, BlobStore.CHUNK_BYTES, request.expiresAtEpochMilliseconds)) },
                    onFailure = { error -> call.respond(error.blobUploadStatus()) }
                )
        }
    }
    put("/v1/blobs/{blobId}/chunks/{index}") {
        val blobId = call.parameters["blobId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        val index = call.parameters["index"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
        val capability = call.bearerToken() ?: return@put call.respond(HttpStatusCode.Unauthorized)
        val bytes = call.receiveChannel().readBuffer(BlobStore.CHUNK_BYTES + 1L).readByteArray()
        runCatching { store.storeChunk(blobId, capability, index, bytes, now()) }.fold(
            onSuccess = { sha256 -> sha256?.let { call.respond(HttpStatusCode.OK, BlobChunkStored(index, it)) } ?: call.respond(HttpStatusCode.NotFound) },
            onFailure = { error -> call.respond(error.blobUploadStatus()) }
        )
    }
    get("/v1/blobs/{blobId}/chunks") {
        val blobId = call.parameters["blobId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val capability = call.bearerToken() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val stored = runCatching { store.storedChunks(blobId, capability, now()) }.getOrNull() ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respond(BlobChunksResponse(stored))
    }
    get("/v1/blobs/{blobId}/chunks/{index}") {
        val blobId = call.parameters["blobId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val index = call.parameters["index"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
        val capability = call.bearerToken() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val chunk = runCatching { store.readableChunk(blobId, capability, index, now()) }.getOrNull() ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respondFile(chunk.toFile())
    }
}

/** How long an opened blob takes chunks; a transfer that waits for a protecting circuit can resume within it. */
private const val CHUNKED_UPLOAD_WINDOW_MILLISECONDS = 6L * 60L * 60L * 1_000L

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
