package com.cbgm.sparrow.server.protocol

import kotlinx.serialization.Serializable

/**
 * What a node keeps about a blob. A blob sent in chunks also has [chunkCount], the hash of the capability that uploads
 * its chunks and until when that is allowed; it can be read once [complete].
 */
@Serializable
data class BlobMetadata(
    val blobId: String,
    val byteSize: Long,
    val readCapabilitySha256: String,
    val deleteCapabilitySha256: String,
    val expiresAtEpochMilliseconds: Long,
    val chunkCount: Int? = null,
    val uploadCapabilitySha256: String? = null,
    val uploadExpiresAtEpochMilliseconds: Long? = null,
    val complete: Boolean = true
)

/**
 * `POST /v1/blobs`: a blob that will arrive in chunks, opened through the app's circuit and paid with [creationWork]
 * (nothing names the sender). The app chooses the capabilities and sends only their hashes.
 */
@Serializable
data class OpenBlobRequest(
    val blobId: String,
    val byteSize: Long,
    val readCapabilitySha256: String,
    val deleteCapabilitySha256: String,
    val uploadCapabilitySha256: String,
    val expiresAtEpochMilliseconds: Long,
    val creationWork: MailboxCreationWork? = null
)

/** The node that holds the blob and how large every chunk but the last is. */
@Serializable
data class OpenBlobResponse(
    val nodeId: String,
    val chunkBytes: Int,
    val expiresAtEpochMilliseconds: Long
)

/** `GET /v1/blobs/{id}/chunks`: the chunks the node already holds, so an interrupted upload goes on from there. */
@Serializable
data class BlobChunksResponse(
    val stored: List<Int>
)

/** `PUT /v1/blobs/{id}/chunks/{n}`: the SHA-256 of what the node stored, for the app to compare with what it sent. */
@Serializable
data class BlobChunkStored(
    val index: Int,
    val sha256: String
)
