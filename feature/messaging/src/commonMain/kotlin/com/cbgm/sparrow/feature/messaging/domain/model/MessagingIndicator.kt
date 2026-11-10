package com.cbgm.sparrow.feature.messaging.domain.model

/**
 * An ephemeral indicator of [senderContactId], for a direct chat or, with [groupId], a group. The sender is the
 * contact whose mailbox it arrived in; authorization belongs to orchestration.
 */
data class MessagingIndicator(
    val senderContactId: String,
    val groupId: String?,
    val indicatorType: String
)
