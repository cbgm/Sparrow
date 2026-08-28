package com.cbgm.sparrow.server.protocol

import kotlinx.serialization.Serializable

@Serializable
data class BlobUploadTicketClaims(
    val blobId: String,
    val maximumBytes: Long,
    val readCapabilitySha256: String,
    val deleteCapabilitySha256: String,
    val blobExpiresAtEpochMilliseconds: Long,
    val ticketExpiresAtEpochMilliseconds: Long
) {
    init {
        require(BLOB_ID.matches(blobId)) { "Invalid blob ID" }
        require(maximumBytes > 0L)
        require(SHA_256_HEX.matches(readCapabilitySha256)) { "Invalid read capability hash" }
        require(SHA_256_HEX.matches(deleteCapabilitySha256)) { "Invalid delete capability hash" }
        require(blobExpiresAtEpochMilliseconds > 0L)
        require(ticketExpiresAtEpochMilliseconds > 0L)
        require(ticketExpiresAtEpochMilliseconds < blobExpiresAtEpochMilliseconds) {
            "Upload ticket must expire before the blob"
        }
    }

    private companion object {
        val BLOB_ID = Regex("[A-Za-z0-9_-]{16,128}")
        val SHA_256_HEX = Regex("[0-9a-f]{64}")
    }
}

@Serializable
data class BlobMetadata(
    val blobId: String,
    val byteSize: Long,
    val readCapabilitySha256: String,
    val deleteCapabilitySha256: String,
    val expiresAtEpochMilliseconds: Long
)
