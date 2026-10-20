package com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper

import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationType
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.ConversationInfo

internal fun ConversationEntity.toConversationInfo(title: String?): ConversationInfo =
    ConversationInfo(
        id = id,
        isGroup = type == ConversationType.GROUP.name,
        title = title
    )
