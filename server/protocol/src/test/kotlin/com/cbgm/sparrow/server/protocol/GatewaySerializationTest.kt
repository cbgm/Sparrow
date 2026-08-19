package com.cbgm.sparrow.server.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

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
    fun legacyRegisterFrameStillDecodes() {
        val message =
            serverJson.decodeFromString<GatewayClientMessage>(
                """{"type":"register","routingId":"routing-id"}"""
            )

        assertEquals(GatewayClientMessage.Register("routing-id"), message)
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
    fun signedRegisterFrameDecodesAllRouteProofFields() {
        val encoded =
            "{\"type\":\"register\",\"routingId\":\"scrouting1_test\"," +
                "\"connectionId\":\"connection-a\",\"generation\":123," +
                "\"expiresAtEpochMilliseconds\":456,\"clientSigningPublicKey\":[1,2,3]," +
                "\"clientSignature\":[4,5,6]}"
        val message =
            serverJson.decodeFromString<GatewayClientMessage>(
                encoded
            ) as GatewayClientMessage.Register

        assertEquals("connection-a", message.connectionId)
        assertEquals(123L, message.generation)
        assertNotNull(message.clientSigningPublicKey)
        assertNotNull(message.clientSignature)
    }

    @Test
    fun incompleteSignedRegisterFrameIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            serverJson.decodeFromString<GatewayClientMessage>(
                """{"type":"register","routingId":"scrouting1_test","connectionId":"connection-a","generation":123}"""
            )
        }
    }
}
