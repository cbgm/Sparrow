package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState

interface GroupDescriptionRepository {
    suspend fun get(groupId: String): Result<GroupDescriptionState>

    suspend fun save(state: GroupDescriptionState): Result<Unit>

    suspend fun deleteLocal(groupId: String): Result<Unit>
}
