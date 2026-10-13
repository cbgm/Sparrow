package com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper

import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

private const val GROUP_CONVERSATION_TYPE = "GROUP"

internal fun toGroupConversationEntity(
    groupId: String,
    title: String,
    createdAtEpochMilliseconds: Long
): ConversationEntity =
    ConversationEntity(
        id = groupId,
        contactId = null,
        type = GROUP_CONVERSATION_TYPE,
        title = title,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        updatedAtEpochMilliseconds = createdAtEpochMilliseconds
    )

internal fun ConversationEntity.toGroupMembershipContext(): GroupMembershipContext =
    GroupMembershipContext(
        title = requireNotNull(title) { "Group title was not found" },
        createdAtEpochMilliseconds = createdAtEpochMilliseconds
    )
