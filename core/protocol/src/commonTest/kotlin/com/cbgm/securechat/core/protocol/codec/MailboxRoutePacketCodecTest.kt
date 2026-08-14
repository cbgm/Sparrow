package com.cbgm.securechat.core.protocol.codec

import com.cbgm.securechat.core.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.securechat.core.protocol.packet.MailboxRoutePacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MailboxRoutePacketCodecTest {
    @Test
    fun signedMailboxRouteRoundTrips() {
        val packet =
            MailboxRoutePacket(
                packetId = "packet-1",
                deliveryRoute =
                    MailboxDeliveryRoute(
                        routeId = "route-1",
                        nodeId = "node-b",
                        nodeEndpoint = "https://node-b.example",
                        mailboxId = "mailbox-1",
                        sendCapability = "send-capability",
                        sequence = 7L,
                        expiresAtEpochMilliseconds = 9_000L,
                        identitySignature = byteArrayOf(4, 5, 6)
                    )
            )
        val codec = KotlinxPacketCodec(createProtocolJson())

        val decoded = codec.decode(codec.encode(packet).getOrThrow()).getOrThrow() as MailboxRoutePacket

        assertEquals(packet.packetId, decoded.packetId)
        assertEquals(packet.deliveryRoute.routeId, decoded.deliveryRoute.routeId)
        assertEquals(packet.deliveryRoute.sendCapability, decoded.deliveryRoute.sendCapability)
        assertContentEquals(
            packet.deliveryRoute.identitySignature,
            decoded.deliveryRoute.identitySignature
        )
    }
}
