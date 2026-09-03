package com.cbgm.sparrow.core.protocol.message

import kotlinx.serialization.Serializable

@Serializable
data class MessageEditPayload(
    val messageId: String,
    val text: String
) {
    init {
        require(messageId.isNotBlank()) { "Edited message ID must not be blank" }
        require(text.isNotBlank()) { "Edited message text must not be blank" }
    }
}
