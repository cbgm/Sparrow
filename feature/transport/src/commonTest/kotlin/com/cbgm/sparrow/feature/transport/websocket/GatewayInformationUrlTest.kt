package com.cbgm.sparrow.feature.transport.websocket

import com.cbgm.sparrow.feature.transport.presence.gatewayInformationUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GatewayInformationUrlTest {
    @Test
    fun convertsWebSocketGatewayUrlToGatewayInformationUrl() {
        assertEquals(
            "http://10.0.2.2:8094/v1/gateway/info",
            gatewayInformationUrl("ws://10.0.2.2:8094/v1/gateway")
        )
        assertEquals(
            "https://chat.example.com/v1/gateway/info",
            gatewayInformationUrl("wss://chat.example.com/v1/gateway?version=1")
        )
    }

    @Test
    fun rejectsNonWebSocketUrls() {
        assertFailsWith<IllegalStateException> {
            gatewayInformationUrl("https://chat.example.com/v1/gateway")
        }
    }
}
