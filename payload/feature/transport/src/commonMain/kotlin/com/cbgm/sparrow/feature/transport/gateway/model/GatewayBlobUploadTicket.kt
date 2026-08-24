package com.cbgm.sparrow.feature.transport.gateway.model

data class GatewayBlobUploadTicketRequest(
    val requestId: String,
    val blobId: String,
    val maximumBytes: Long,
    val readCapabilitySha256: String,
    val deleteCapabilitySha256: String,
    val blobRetentionMilliseconds: Long
) {
    init {
        require(requestId.isNotBlank())
        require(BLOB_ID.matches(blobId)) { "Invalid blob ID" }
        require(maximumBytes > 0L)
        require(SHA_256_HEX.matches(readCapabilitySha256)) { "Invalid read capability hash" }
        require(SHA_256_HEX.matches(deleteCapabilitySha256)) { "Invalid delete capability hash" }
        require(blobRetentionMilliseconds > 0L)
    }

    private companion object {
        val BLOB_ID = Regex("[A-Za-z0-9_-]{16,128}")
        val SHA_256_HEX = Regex("[0-9a-f]{64}")
    }
}

data class GatewayBlobUploadTicket(
    val requestId: String,
    val nodeId: String,
    val uploadToken: String,
    val blobExpiresAtEpochMilliseconds: Long
) {
    init {
        require(requestId.isNotBlank())
        require(nodeId.isNotBlank())
        require(uploadToken.isNotBlank())
        require(blobExpiresAtEpochMilliseconds > 0L)
    }
}
