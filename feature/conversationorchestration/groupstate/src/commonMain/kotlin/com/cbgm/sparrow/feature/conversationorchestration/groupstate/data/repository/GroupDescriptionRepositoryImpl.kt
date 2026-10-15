package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource.GroupDescriptionDataSource
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper.toGroupDescriptionDto
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper.toGroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository

internal class GroupDescriptionRepositoryImpl(
    private val dataSource: GroupDescriptionDataSource
) : GroupDescriptionRepository {
    override suspend fun get(groupId: String): Result<GroupDescriptionState> =
        safeSuspendCall { dataSource.get(groupId).toGroupDescriptionState(groupId) }

    override suspend fun save(state: GroupDescriptionState): Result<Unit> =
        safeSuspendCall { dataSource.save(state.groupId, state.toGroupDescriptionDto()) }

    override suspend fun deleteLocal(groupId: String): Result<Unit> =
        safeSuspendCall { dataSource.deleteLocal(groupId) }
}
