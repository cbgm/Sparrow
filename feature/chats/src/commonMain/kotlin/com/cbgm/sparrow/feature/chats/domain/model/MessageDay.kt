package com.cbgm.sparrow.feature.chats.domain.model

/** A day of a conversation that has messages, with the first message written that day. */
data class MessageDay(
    val epochDay: Long,
    val firstMessage: MessageHistoryCursor,
    val messageCount: Int
)
