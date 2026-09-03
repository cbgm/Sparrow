package com.cbgm.sparrow.core.protocol.message

import com.cbgm.sparrow.core.protocol.codec.createProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageEditPayloadCodecTest {
    private val codec = MessageEditPayloadCodec(createProtocolJson())

    @Test
    fun roundTrip() {
        val original =
            MessageEditPayload(
                messageId = "message-1",
                text = "Updated message"
            )

        val decoded = codec.decode(codec.encode(original))

        assertEquals(original, decoded)
    }
}
