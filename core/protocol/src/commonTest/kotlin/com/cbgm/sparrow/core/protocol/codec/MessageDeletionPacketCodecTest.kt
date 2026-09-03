package com.cbgm.sparrow.core.protocol.codec

import com.cbgm.sparrow.core.protocol.packet.GroupMessageDeletionPacket
import com.cbgm.sparrow.core.protocol.packet.MessageDeletionPacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessageDeletionPacketCodecTest {
    private val codec = KotlinxPacketCodec(createProtocolJson())

    @Test
    fun directMessageDeletionRoundTrip() {
        val original =
            MessageDeletionPacket(
                packetId = "delete-packet-1",
                messageId = "message-1",
                deletedAtEpochMilliseconds = 123_456L
            )

        val decoded = codec.decode(codec.encode(original).getOrThrow()).getOrThrow()

        assertEquals(original, assertIs<MessageDeletionPacket>(decoded))
    }

    @Test
    fun groupMessageDeletionRoundTrip() {
        val original =
            GroupMessageDeletionPacket(
                packetId = "group-delete-packet-1",
                groupId = "group-1",
                epoch = 3,
                deletionId = "group-delete-1",
                deletedAtEpochMilliseconds = 123_456L,
                nonce = ByteArray(24) { 1 },
                ciphertext = ByteArray(32) { 2 },
                senderSignature = ByteArray(64) { 3 }
            )

        val decoded =
            assertIs<GroupMessageDeletionPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original.packetId, decoded.packetId)
        assertEquals(original.groupId, decoded.groupId)
        assertEquals(original.epoch, decoded.epoch)
        assertEquals(original.deletionId, decoded.deletionId)
        assertEquals(original.deletedAtEpochMilliseconds, decoded.deletedAtEpochMilliseconds)
        assertContentEquals(original.nonce, decoded.nonce)
        assertContentEquals(original.ciphertext, decoded.ciphertext)
        assertContentEquals(original.senderSignature, decoded.senderSignature)
    }
}
