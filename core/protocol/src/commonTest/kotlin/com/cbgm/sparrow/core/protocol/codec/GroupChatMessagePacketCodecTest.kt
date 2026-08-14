package com.cbgm.sparrow.core.protocol.codec

import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GroupChatMessagePacketCodecTest {
    private val codec = KotlinxPacketCodec(json = createProtocolJson())

    @Test
    fun secureGroupMessagePacketRoundTrip() {
        val original =
            GroupChatMessagePacket(
                packetId = "group-message-packet-1",
                groupId = "group-1",
                epoch = 2,
                messageId = "group-message-1",
                sentAtEpochMilliseconds = 123_456L,
                nonce = byteArrayOf(1, 2, 3),
                ciphertext = byteArrayOf(4, 5, 6),
                senderSignature = byteArrayOf(7, 8, 9)
            )

        val decoded = codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
        val packet = assertIs<GroupChatMessagePacket>(decoded)

        assertEquals(original.packetId, packet.packetId)
        assertEquals(original.groupId, packet.groupId)
        assertEquals(original.epoch, packet.epoch)
        assertEquals(original.messageId, packet.messageId)
        assertEquals(original.sentAtEpochMilliseconds, packet.sentAtEpochMilliseconds)
        assertContentEquals(original.nonce, packet.nonce)
        assertContentEquals(original.ciphertext, packet.ciphertext)
        assertContentEquals(original.senderSignature, packet.senderSignature)
    }
}
