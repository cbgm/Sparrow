package com.cbgm.sparrow.core.protocol.codec

import com.cbgm.sparrow.core.protocol.packet.GroupMessageEditPacket
import com.cbgm.sparrow.core.protocol.packet.MessageEditPacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessageEditPacketCodecTest {
    private val codec = KotlinxPacketCodec(createProtocolJson())

    @Test
    fun directMessageEditRoundTrip() {
        val original =
            MessageEditPacket(
                packetId = "edit-packet-1",
                messageId = "message-1",
                editedAtEpochMilliseconds = 123_456L,
                text = "Updated message"
            )

        val decoded = codec.decode(codec.encode(original).getOrThrow()).getOrThrow()

        assertEquals(original, assertIs<MessageEditPacket>(decoded))
    }

    @Test
    fun groupMessageEditRoundTrip() {
        val original =
            GroupMessageEditPacket(
                packetId = "group-edit-packet-1",
                groupId = "group-1",
                epoch = 3,
                editId = "group-edit-1",
                editedAtEpochMilliseconds = 123_456L,
                nonce = ByteArray(24) { 1 },
                ciphertext = ByteArray(32) { 2 },
                senderSignature = ByteArray(64) { 3 }
            )

        val decoded =
            assertIs<GroupMessageEditPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original.packetId, decoded.packetId)
        assertEquals(original.groupId, decoded.groupId)
        assertEquals(original.epoch, decoded.epoch)
        assertEquals(original.editId, decoded.editId)
        assertEquals(original.editedAtEpochMilliseconds, decoded.editedAtEpochMilliseconds)
        assertContentEquals(original.nonce, decoded.nonce)
        assertContentEquals(original.ciphertext, decoded.ciphertext)
        assertContentEquals(original.senderSignature, decoded.senderSignature)
    }
}
