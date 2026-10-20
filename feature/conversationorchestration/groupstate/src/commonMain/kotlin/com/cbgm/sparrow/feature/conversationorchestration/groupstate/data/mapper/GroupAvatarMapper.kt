package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.protocol.avatar.GroupAvatarMetadata
import com.cbgm.sparrow.protocol.avatar.GroupAvatarPayload

internal fun GroupAvatarState.toGroupAvatarMetadata(): GroupAvatarMetadata =
    GroupAvatarMetadata(
        changedAtEpochMilliseconds = changedAtEpochMilliseconds,
        hasAvatar = bytes != null,
        payload = bytes?.let { GroupAvatarPayload(it.copyOf()) }
    )
