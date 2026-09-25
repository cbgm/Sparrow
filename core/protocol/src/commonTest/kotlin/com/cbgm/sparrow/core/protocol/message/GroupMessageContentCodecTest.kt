package com.cbgm.sparrow.core.protocol.message

import com.cbgm.sparrow.core.protocol.codec.createProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupMessageContentCodecTest {
    private val codec = GroupMessageContentCodec(createProtocolJson())

    @Test
    fun replyRoundTrip() {
        val encoded =
            codec.encode(
                GroupMessageContent(
                    text = "Reply",
                    replyToMessageId = "message-original-1"
                )
            )

        val decoded = codec.decode(encoded)

        assertEquals("Reply", decoded.text)
        assertEquals("message-original-1", decoded.replyToMessageId)
        assertTrue("\"replyToMessageId\":\"message-original-1\"" in encoded)
    }

    @Test
    fun replyFieldIsNotAddedWithoutReply() {
        val encoded = codec.encode(GroupMessageContent(text = "Hello"))

        assertFalse("\"replyToMessageId\"" in encoded)
    }
}
