package com.cbgm.sparrow.protocol.message

import kotlinx.serialization.Serializable

@Serializable
data class MessageDeletionPayload(
    val messageId: String
) {
    init {
        require(messageId.isNotBlank()) { "Deleted message ID must not be blank" }
    }
}
