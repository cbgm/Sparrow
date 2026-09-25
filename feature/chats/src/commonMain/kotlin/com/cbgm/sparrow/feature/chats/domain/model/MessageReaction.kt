package com.cbgm.sparrow.feature.chats.domain.model

data class MessageReaction(
    val emoji: String,
    val isMine: Boolean,
    val reactorContactId: String? = null
)
