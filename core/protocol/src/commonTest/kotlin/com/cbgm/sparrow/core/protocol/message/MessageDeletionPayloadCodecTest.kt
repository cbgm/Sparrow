package com.cbgm.sparrow.core.protocol.message

import com.cbgm.sparrow.core.protocol.codec.createProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageDeletionPayloadCodecTest {
    private val codec = MessageDeletionPayloadCodec(createProtocolJson())

    @Test
    fun roundTrip() {
        val original = MessageDeletionPayload(messageId = "message-1")

        val decoded = codec.decode(codec.encode(original))

        assertEquals(original, decoded)
    }
}
