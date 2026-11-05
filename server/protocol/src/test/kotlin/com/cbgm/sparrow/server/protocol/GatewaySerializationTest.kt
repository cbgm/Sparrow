package com.cbgm.sparrow.server.protocol

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GatewaySerializationTest {
    @Test
    fun registeredFrameContainsOnlyStableTransportIdentity() {
        val encoded =
            serverJson.encodeToString<GatewayServerMessage>(
                GatewayServerMessage.Registered(routingId = "routing-id")
            )

        assertFalse("nodeId" in encoded)
        assertFalse("routeLifetimeMilliseconds" in encoded)
        assertFalse("routeRefreshIntervalMilliseconds" in encoded)
        assertFalse("serverTimeEpochMilliseconds" in encoded)

        val decoded =
            serverJson.decodeFromString<GatewayServerMessage>(encoded)
                as GatewayServerMessage.Registered

        assertEquals("routing-id", decoded.routingId)
    }

    @Test
    fun registerWithoutSessionProofDoesNotDecode() {
        assertFailsWith<IllegalArgumentException> {
            serverJson.decodeFromString<GatewayClientMessage>(
                """{"type":"register","routingId":"routing-id","connectionId":"connection-a"}"""
            )
        }
    }

    @Test
    fun challengeRoundTrips() {
        val challenge = GatewayServerMessage.Challenge(nonce = ByteArray(32) { it.toByte() }, nodeId = "node-a")
        val encoded = serverJson.encodeToString<GatewayServerMessage>(challenge)

        assertTrue(encoded.startsWith("""{"type":"challenge","nonce":[0,1,2"""))
        assertEquals(challenge, serverJson.decodeFromString<GatewayServerMessage>(encoded))
    }

    @Test
    fun legacyIncomingEnvelopeDoesNotGainFederationFields() {
        val encoded =
            serverJson.encodeToString<GatewayServerMessage>(
                GatewayServerMessage.IncomingEnvelope(
                    TransportEnvelope(
                        envelopeId = "envelope-id",
                        senderId = "sender",
                        recipientId = "recipient",
                        payload = "ciphertext",
                        createdAtEpochMilliseconds = 1L
                    )
                )
            )

        assertFalse("mailboxRoute" in encoded)
        assertFalse("expiresAtEpochMilliseconds" in encoded)
    }

    @Test
    fun envelopeAcceptanceCarriesServerDeliveryDeadline() {
        val encoded =
            serverJson.encodeToString<GatewayServerMessage>(
                GatewayServerMessage.EnvelopeAccepted(
                    envelopeId = "envelope-id",
                    expiresAtEpochMilliseconds = 123_456L
                )
            )

        val decoded =
            serverJson.decodeFromString<GatewayServerMessage>(encoded)
                as GatewayServerMessage.EnvelopeAccepted

        assertEquals("envelope-id", decoded.envelopeId)
        assertEquals(123_456L, decoded.expiresAtEpochMilliseconds)
    }

    @Test
    fun signedRegisterFrameDecodesSessionProof() {
        val encoded =
            "{\"type\":\"register\",\"routingId\":\"scrouting1_test\"," +
                "\"connectionId\":\"connection-a\",\"clientSigningPublicKey\":[1,2,3]," +
                "\"clientSignature\":[4,5,6]}"
        val message =
            serverJson.decodeFromString<GatewayClientMessage>(
                encoded
            ) as GatewayClientMessage.Register

        assertEquals("connection-a", message.connectionId)
        assertContentEquals(byteArrayOf(1, 2, 3), message.clientSigningPublicKey)
        assertContentEquals(byteArrayOf(4, 5, 6), message.clientSignature)
    }

    @Test
    fun blobUploadTicketMessagesRoundTrip() {
        val request =
            GatewayClientMessage.RequestBlobUploadTicket(
                requestId = "request-1",
                blobId = "blob-1234567890123456",
                maximumBytes = 1_024L,
                readCapabilitySha256 = "a".repeat(64),
                deleteCapabilitySha256 = "b".repeat(64),
                blobExpiresAtEpochMilliseconds = 123_456L
            )
        val decodedRequest =
            serverJson.decodeFromString<GatewayClientMessage>(
                serverJson.encodeToString<GatewayClientMessage>(request)
            )

        assertEquals(request, decodedRequest)

        val response =
            GatewayServerMessage.BlobUploadTicketIssued(
                requestId = "request-1",
                nodeId = "node-a",
                uploadToken = "upload-token",
                blobExpiresAtEpochMilliseconds = 123_456L
            )
        val decodedResponse =
            serverJson.decodeFromString<GatewayServerMessage>(
                serverJson.encodeToString<GatewayServerMessage>(response)
            )

        assertEquals(response, decodedResponse)
    }
}
