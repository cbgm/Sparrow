package com.cbgm.sparrow.protocol.message

import com.cbgm.sparrow.core.blob.data.model.EncryptedBlobReferenceDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.PollDto
import com.cbgm.sparrow.core.messagepart.data.model.PollOptionDto
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.protocol.codec.createProtocolJson
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class GroupPollContentTest {
    private val codec = GroupMessageContentCodec(createProtocolJson())
    private val poll = PollDto(
        id = "poll",
        question = "Where?",
        options = listOf(PollOptionDto("a", "Here"), PollOptionDto("b", "There")),
        allowMultipleSelection = true,
        allowVoteChange = false,
        isAnonymous = true,
        expiresAtEpochMilliseconds = 1000L
    )

    @Test
    fun pollRoundTripsWithEncryptedImagesAndReply() {
        val image = ImageDto(
            id = "image",
            mimeType = "image/jpeg",
            byteSize = 10,
            width = 20,
            height = 30,
            blob = EncryptedBlobReferenceDto(
                nodeId = "node",
                blobId = "blob",
                readCapability = "read",
                ciphertextByteSize = 26,
                expiresAtEpochMilliseconds = 10000,
                encryptionKey = ByteArray(32),
                nonce = ByteArray(12),
                ciphertextSha256 = ByteArray(32)
            )
        )
        val expected = poll.copy(images = listOf(image))
        val decoded = codec.decode(codec.encode(GroupMessageContent(parts = listOf(expected), replyToMessageId = "reply")))
        assertEquals(expected, assertIs<PollDto>(decoded.parts.single()))
        assertEquals("reply", decoded.replyToMessageId)
    }

    @Test
    fun rejectsDirectPollAndMixedContent() {
        assertFailsWith<IllegalArgumentException> {
            ChatMessagePacket(packetId = "packet", messageId = "message", sentAtEpochMilliseconds = 1, parts = listOf(poll))
        }
        assertFailsWith<IllegalArgumentException> {
            GroupMessageContent(parts = listOf(poll, TextDto("text", "extra")))
        }
    }

    @Test
    fun rejectsInvalidOptionsAndImageReferences() {
        assertFailsWith<IllegalArgumentException> {
            GroupMessageContent(parts = listOf(poll.copy(options = List(7) { PollOptionDto("$it", "Option") })))
        }
        assertFailsWith<IllegalArgumentException> {
            GroupMessageContent(parts = listOf(poll.copy(options = listOf(PollOptionDto("a", "One"), PollOptionDto("a", "Two")))))
        }
        assertFailsWith<IllegalArgumentException> {
            GroupMessageContent(
                parts = listOf(
                    poll.copy(
                        images = listOf(
                            ImageDto(id = "image", mimeType = "image/jpeg", byteSize = 10, width = 20, height = 30)
                        )
                    )
                )
            )
        }
    }
}
