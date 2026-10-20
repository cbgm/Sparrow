package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model.GroupAvatarDto
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model.GroupDescriptionDto
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model.GroupTitleDto
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState

internal fun GroupTitleDto.toGroupTitleState(groupId: String): GroupTitleState =
    GroupTitleState(groupId = groupId, title = title, changedAtEpochMilliseconds = changedAtEpochMilliseconds)

internal fun GroupTitleState.toGroupTitleDto(): GroupTitleDto =
    GroupTitleDto(title = title, changedAtEpochMilliseconds = changedAtEpochMilliseconds)

internal fun GroupDescriptionDto.toGroupDescriptionState(groupId: String): GroupDescriptionState =
    GroupDescriptionState(
        groupId = groupId,
        description = description,
        changedAtEpochMilliseconds = changedAtEpochMilliseconds
    )

internal fun GroupDescriptionState.toGroupDescriptionDto(): GroupDescriptionDto =
    GroupDescriptionDto(description = description, changedAtEpochMilliseconds = changedAtEpochMilliseconds)

internal fun GroupAvatarDto.toGroupAvatarState(groupId: String): GroupAvatarState =
    GroupAvatarState(groupId = groupId, bytes = bytes, changedAtEpochMilliseconds = changedAtEpochMilliseconds)

internal fun GroupAvatarState.toGroupAvatarDto(): GroupAvatarDto =
    GroupAvatarDto(bytes = bytes, changedAtEpochMilliseconds = changedAtEpochMilliseconds)
