package com.cbgm.sparrow.feature.transport.gateway.model

import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import kotlinx.serialization.InternalSerializationApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(InternalSerializationApi::class)
class GatewayClientMessageTest {
    @Test
    fun unsignedRegistrationKeepsTheLegacyWireShape() {
        val encoded =
            createGatewayJson().encodeToString<GatewayClientMessage>(
                GatewayClientMessage.Register(routingId = "scrouting1_test")
            )

        assertEquals(
            """{"type":"register","routingId":"scrouting1_test"}""",
            encoded
        )
    }
}
