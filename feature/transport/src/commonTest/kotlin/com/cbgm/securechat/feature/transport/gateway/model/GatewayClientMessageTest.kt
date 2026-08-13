package com.cbgm.securechat.feature.transport.gateway.model

import com.cbgm.securechat.feature.transport.gateway.codec.createGatewayJson
import kotlin.test.Test
import kotlin.test.assertEquals

class GatewayClientMessageTest {
    @Test
    fun unsignedRegistrationKeepsTheLegacyWireShape() {
        val encoded =
            createGatewayJson().encodeToString<GatewayClientMessage>(
                GatewayClientMessage.Register(routingId = "scrouting1_test")
            )

        assertEquals(
            """{"type":"register","relayId":"scrouting1_test"}""",
            encoded
        )
    }
}
