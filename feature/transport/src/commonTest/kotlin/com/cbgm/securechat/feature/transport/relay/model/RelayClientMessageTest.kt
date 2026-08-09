package com.cbgm.securechat.feature.transport.relay.model

import com.cbgm.securechat.feature.transport.relay.codec.createRelayJson
import kotlin.test.Test
import kotlin.test.assertEquals

class RelayClientMessageTest {
    @Test
    fun unsignedRegistrationKeepsTheLegacyWireShape() {
        val encoded =
            createRelayJson().encodeToString<RelayClientMessage>(
                RelayClientMessage.Register(relayId = "scrouting1_test")
            )

        assertEquals(
            """{"type":"register","relayId":"scrouting1_test"}""",
            encoded
        )
    }
}
