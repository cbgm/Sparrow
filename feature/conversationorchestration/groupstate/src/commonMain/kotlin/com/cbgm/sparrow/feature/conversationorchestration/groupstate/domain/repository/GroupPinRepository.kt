package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState

interface GroupPinRepository {
    suspend fun find(groupId: String): Result<GroupPinState?>

    suspend fun save(pin: GroupPinState): Result<Unit>

    suspend fun deleteLocal(groupId: String): Result<Unit>
}
