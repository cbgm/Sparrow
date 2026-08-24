package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.BlobUploadTicketClaims
import com.cbgm.sparrow.server.protocol.GatewayClientMessage
import com.cbgm.sparrow.server.protocol.GatewayServerMessage

class GatewayBlobUploadTicketIssuer(
    private val nodeId: String,
    private val permitStore: BlobUploadPermitStore,
    private val maximumBlobBytes: Long,
    private val maximumRetentionMilliseconds: Long,
    private val ticketLifetimeMilliseconds: Long,
    private val now: () -> Long = System::currentTimeMillis
) {
    init {
        require(nodeId.isNotBlank())
        require(maximumBlobBytes > 0L)
        require(maximumRetentionMilliseconds > 0L)
        require(ticketLifetimeMilliseconds in 1 until maximumRetentionMilliseconds)
    }

    fun issue(request: GatewayClientMessage.RequestBlobUploadTicket): GatewayServerMessage {
        val currentTime = now()
        if (request.maximumBytes !in 1..maximumBlobBytes) {
            return request.rejected("BLOB_TOO_LARGE", "Requested blob exceeds the node limit")
        }
        if (request.blobRetentionMilliseconds !in 2L..maximumRetentionMilliseconds) {
            return request.rejected("INVALID_BLOB_RETENTION", "Requested blob retention is not allowed")
        }

        val blobExpiresAt = currentTime + request.blobRetentionMilliseconds
        val ticketExpiresAt =
            minOf(
                currentTime + ticketLifetimeMilliseconds,
                blobExpiresAt - 1L
            )
        if (ticketExpiresAt <= currentTime) {
            return request.rejected("INVALID_BLOB_RETENTION", "Blob retention is too short for upload")
        }

        val claims =
            BlobUploadTicketClaims(
                blobId = request.blobId,
                maximumBytes = request.maximumBytes,
                readCapabilitySha256 = request.readCapabilitySha256,
                deleteCapabilitySha256 = request.deleteCapabilitySha256,
                blobExpiresAtEpochMilliseconds = blobExpiresAt,
                ticketExpiresAtEpochMilliseconds = ticketExpiresAt
            )
        return GatewayServerMessage.BlobUploadTicketIssued(
            requestId = request.requestId,
            nodeId = nodeId,
            uploadToken = permitStore.issue(claims),
            blobExpiresAtEpochMilliseconds = blobExpiresAt
        )
    }

    private fun GatewayClientMessage.RequestBlobUploadTicket.rejected(
        code: String,
        message: String
    ): GatewayServerMessage.BlobUploadTicketRejected =
        GatewayServerMessage.BlobUploadTicketRejected(
            requestId = requestId,
            code = code,
            message = message
        )
}
