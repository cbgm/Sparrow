package com.cbgm.sparrow.core.protocol.message

import kotlinx.serialization.json.Json

class MessageEditPayloadCodec(
    private val json: Json
) {
    fun encode(payload: MessageEditPayload): String = json.encodeToString(payload)

    fun decode(plaintext: String): MessageEditPayload = json.decodeFromString(plaintext)
}
