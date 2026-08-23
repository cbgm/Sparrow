package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.GatewayClientMessage
import com.cbgm.sparrow.server.protocol.GatewayServerMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GatewayBlobUploadTicketIssuerTest {
    private val permits = BlobUploadPermitStore()
    private val issuer =
        GatewayBlobUploadTicketIssuer(
            nodeId = "node-a",
            permitStore = permits,
            maximumBlobBytes = 128L * 1024L * 1024L,
            maximumRetentionMilliseconds = 30L * 24L * 60L * 60L * 1_000L,
            ticketLifetimeMilliseconds = 5L * 60L * 1_000L,
            now = { NOW }
        )

    @Test
    fun validRequestGetsScopedOneTimePermit() {
        val response = issuer.issue(request(maximumBytes = 1_024L))
        val issued = assertIs<GatewayServerMessage.BlobUploadTicketIssued>(response)
        val claims = requireNotNull(permits.consume(issued.uploadToken))

        assertEquals("node-a", issued.nodeId)
        assertEquals("blob-1234567890123456", claims.blobId)
        assertEquals(1_024L, claims.maximumBytes)
        assertEquals(NOW + RETENTION, claims.blobExpiresAtEpochMilliseconds)
        assertNull(permits.consume(issued.uploadToken))
    }

    @Test
    fun oversizedBlobIsRejected() {
        val response = issuer.issue(request(maximumBytes = 129L * 1024L * 1024L))
        val rejected = assertIs<GatewayServerMessage.BlobUploadTicketRejected>(response)

        assertEquals("BLOB_TOO_LARGE", rejected.code)
    }

    private fun request(maximumBytes: Long): GatewayClientMessage.RequestBlobUploadTicket =
        GatewayClientMessage.RequestBlobUploadTicket(
            requestId = "request-1",
            blobId = "blob-1234567890123456",
            maximumBytes = maximumBytes,
            readCapabilitySha256 = "a".repeat(64),
            deleteCapabilitySha256 = "b".repeat(64),
            blobExpiresAtEpochMilliseconds = NOW + RETENTION
        )

    private companion object {
        const val NOW = 1_000_000L
        const val RETENTION = 24L * 60L * 60L * 1_000L
    }
}
