package com.cbgm.securechat.server.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class RelayCompatibilityTest {
    @Test
    fun legacyRegisterFrameStillDecodes() {
        val message =
            serverJson.decodeFromString<GatewayClientMessage>(
                """{"type":"register","relayId":"routing-id"}"""
            )

        assertEquals(GatewayClientMessage.Register("routing-id"), message)
    }

    @Test
    fun legacyIncomingEnvelopeDoesNotGainFederationFields() {
        val encoded =
            serverJson.encodeToString<GatewayServerMessage>(
                GatewayServerMessage.IncomingEnvelope(
                    RelayEnvelope(
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
    fun signedRegisterFrameDecodesAllRouteProofFields() {
        val encoded =
            "{\"type\":\"register\",\"relayId\":\"scrouting1_test\"," +
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
                """{"type":"register","relayId":"scrouting1_test","connectionId":"connection-a","generation":123}"""
            )
        }
    }
}
