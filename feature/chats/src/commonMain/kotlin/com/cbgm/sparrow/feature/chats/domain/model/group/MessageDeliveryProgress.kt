package com.cbgm.sparrow.feature.chats.domain.model.group

data class MessageDeliveryProgress(
    val recipientCount: Int = 0,
    val deliveredCount: Int = 0,
    val readCount: Int = 0
)
