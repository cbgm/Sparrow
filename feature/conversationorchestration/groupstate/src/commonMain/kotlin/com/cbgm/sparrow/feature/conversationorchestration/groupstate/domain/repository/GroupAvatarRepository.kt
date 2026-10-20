package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState

interface GroupAvatarRepository {
    suspend fun get(groupId: String): Result<GroupAvatarState>

    suspend fun save(state: GroupAvatarState): Result<Unit>

    suspend fun deleteLocal(groupId: String): Result<Unit>
}
