package com.cbgm.sparrow.protocol.message

import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.protocol.codec.createProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GroupMessageContentCodecTest {
    private val codec = GroupMessageContentCodec(createProtocolJson())

    @Test
    fun replyRoundTrip() {
        val encoded =
            codec.encode(
                GroupMessageContent(
                    parts = listOf(TextDto(id = "message-1", text = "Reply")),
                    replyToMessageId = "message-original-1"
                )
            )

        val decoded = codec.decode(encoded)
        val textPart = assertIs<TextDto>(decoded.parts.single())

        assertEquals("Reply", textPart.text)
        assertEquals("message-original-1", decoded.replyToMessageId)
        assertTrue("\"replyToMessageId\":\"message-original-1\"" in encoded)
    }

    @Test
    fun replyFieldIsNotAddedWithoutReply() {
        val encoded =
            codec.encode(
                GroupMessageContent(
                    parts = listOf(TextDto(id = "message-1", text = "Hello"))
                )
            )

        assertFalse("\"replyToMessageId\"" in encoded)
    }

    @Test
    fun legacyPlaintextDecodesAsTextPart() {
        val decoded = codec.decode("Hello")
        val textPart = assertIs<TextDto>(decoded.parts.single())

        assertEquals("Hello", textPart.text)
    }
}
