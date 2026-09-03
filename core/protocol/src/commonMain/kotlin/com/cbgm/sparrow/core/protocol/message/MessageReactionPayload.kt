package com.cbgm.sparrow.core.protocol.message

import kotlinx.serialization.Serializable

@Serializable
data class MessageReactionPayload(
    val messageId: String,
    val emoji: String,
    val removed: Boolean = false
) {
    init {
        require(messageId.isNotBlank()) { "Reaction message ID must not be blank" }
        require(emoji.isNotBlank()) { "Reaction emoji must not be blank" }
    }
}
