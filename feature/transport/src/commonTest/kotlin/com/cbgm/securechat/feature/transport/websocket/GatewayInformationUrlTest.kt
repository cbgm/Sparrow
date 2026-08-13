package com.cbgm.securechat.feature.transport.websocket

import com.cbgm.securechat.feature.transport.presence.gatewayInformationUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GatewayInformationUrlTest {
    @Test
    fun convertsWebSocketGatewayUrlToGatewayInformationUrl() {
        assertEquals(
            "http://10.0.2.2:8094/v1/gateway",
            gatewayInformationUrl("ws://10.0.2.2:8094/relay")
        )
        assertEquals(
            "https://chat.example.com/v1/gateway",
            gatewayInformationUrl("wss://chat.example.com/relay?version=1")
        )
    }

    @Test
    fun rejectsNonWebSocketUrls() {
        assertFailsWith<IllegalStateException> {
            gatewayInformationUrl("https://chat.example.com/relay")
        }
    }
}
