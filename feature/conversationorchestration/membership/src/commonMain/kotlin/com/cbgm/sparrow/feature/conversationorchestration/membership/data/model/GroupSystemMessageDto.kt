package com.cbgm.sparrow.feature.conversationorchestration.membership.data.model

import com.cbgm.sparrow.data.database.entity.MessageEntity

/** A local system message row together with its text part. */
internal data class GroupSystemMessageDto(
    val message: MessageEntity,
    val text: String
)
