package com.cbgm.sparrow.feature.transport.gateway.model

import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import kotlinx.serialization.InternalSerializationApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(InternalSerializationApi::class)
class GatewayClientMessageTest {
    @Test
    fun registrationCarriesTheSessionProof() {
        val encoded =
            createGatewayJson().encodeToString<GatewayClientMessage>(
                GatewayClientMessage.Register(
                    routingId = "scrouting1_test",
                    connectionId = "connection-a",
                    clientSigningPublicKey = byteArrayOf(1, 2),
                    clientSignature = byteArrayOf(3, 4)
                )
            )

        assertEquals(
            """{"type":"register","routingId":"scrouting1_test","connectionId":"connection-a",""" +
                """"clientSigningPublicKey":[1,2],"clientSignature":[3,4]}""",
            encoded
        )
    }
}
