package com.cbgm.sparrow.feature.transport.websocket

import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
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
}
