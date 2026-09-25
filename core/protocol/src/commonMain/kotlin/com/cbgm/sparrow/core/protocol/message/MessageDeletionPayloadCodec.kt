package com.cbgm.sparrow.core.protocol.message

import kotlinx.serialization.json.Json

class MessageDeletionPayloadCodec(
    private val json: Json
) {
    fun encode(payload: MessageDeletionPayload): String =
        json.encodeToString(payload)

    fun decode(plaintext: String): MessageDeletionPayload =
        json.decodeFromString(plaintext)
}
