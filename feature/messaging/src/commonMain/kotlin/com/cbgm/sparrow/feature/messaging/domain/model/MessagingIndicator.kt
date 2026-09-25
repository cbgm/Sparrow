package com.cbgm.sparrow.feature.messaging.domain.model

/** Generic ephemeral indicator from the transport stream; authentication/authorization belongs to orchestration. */
data class MessagingIndicator(
    val senderRoutingId: String,
    val indicatorType: String
)
