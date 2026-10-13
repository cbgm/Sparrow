package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState

interface GroupTitleRepository {
    suspend fun get(groupId: String): Result<GroupTitleState>

    suspend fun save(state: GroupTitleState): Result<Unit>

    suspend fun deleteLocal(groupId: String): Result<Unit>
}
