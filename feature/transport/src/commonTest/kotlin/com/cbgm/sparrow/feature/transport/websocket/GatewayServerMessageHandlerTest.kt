package com.cbgm.sparrow.feature.transport.websocket

import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.InternalSerializationApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(InternalSerializationApi::class)
class GatewayServerMessageHandlerTest {
    private val handler =
        GatewayServerMessageHandler(
            json = createGatewayJson(),
            pendingRequestRegistry = GatewayPendingRequestRegistry()
        )

    @Test
    fun gatewayChallengeFrameDecodesWithStrictClientJson() {
        val nonce = (0 until 32).joinToString(separator = ",")
        val challenge =
            handler.decodeChallenge("""{"type":"challenge","nonce":[$nonce],"nodeId":"node-a"}""")

        assertEquals("node-a", challenge.nodeId)
        assertEquals(32, challenge.nonce.size)
        assertEquals(31.toByte(), challenge.nonce.last())
    }

    @Test
    fun firstFrameOtherThanChallengeFailsTheConnection() {
        val error =
            assertFailsWith<IllegalStateException> {
                handler.decodeChallenge("""{"type":"registered","routingId":"scrouting1_test"}""")
            }

        assertEquals("Gateway did not send a session challenge", error.message)
    }

    @Test
    fun rejectedSessionFailsTheConnection() {
        val error =
            assertFailsWith<IllegalStateException> {
                handler.decodeChallenge(
                    """{"type":"error","code":"INVALID_SESSION","message":"Session proof is invalid"}"""
                )
            }

        assertEquals("Gateway rejected the session: INVALID_SESSION", error.message)
    }

    @Test
    fun unreadableEnvelopeIsDroppedAndTheSessionGoesOn() =
        runTest {
            val received = mutableListOf<TransportEnvelope>()
            val dropped = mutableListOf<String>()

            suspend fun handle(frame: String) =
                handler.handle(
                    encodedMessage = frame,
                    expectedRoutingId = "scrouting1_me",
                    onGatewayRegistered = {},
                    onRouteRegistered = {},
                    onRouteRejected = {},
                    onIncomingEnvelope = { envelope -> received += envelope },
                    onUnreadableEnvelope = { envelopeId -> dropped += envelopeId },
                    onIndicatorEvent = {}
                )

            handle(envelopeFrame(id = "poisoned", createdAt = -1))
            handle("""{"type":"some_future_message","value":1}""")
            handle("not json")
            handle(envelopeFrame(id = "fine", createdAt = 1))

            assertEquals(listOf("poisoned"), dropped)
            assertEquals(listOf("fine"), received.map(TransportEnvelope::envelopeId))
        }

    private fun envelopeFrame(id: String, createdAt: Long) =
        """{"type":"incoming_envelope","envelope":{"version":1,"envelopeId":"$id","senderId":"scrouting1_a",""" +
            """"recipientId":"scrouting1_me","payload":"scmsg:1","createdAtEpochMilliseconds":$createdAt,"extra":true}}"""
}
