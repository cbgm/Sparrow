package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource.GroupTitleDataSource
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper.toGroupTitleDto
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper.toGroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository

internal class GroupTitleRepositoryImpl(
    private val dataSource: GroupTitleDataSource
) : GroupTitleRepository {
    override suspend fun get(groupId: String): Result<GroupTitleState> =
        safeSuspendCall { dataSource.get(groupId).toGroupTitleState(groupId) }

    override suspend fun save(state: GroupTitleState): Result<Unit> =
        safeSuspendCall { dataSource.save(state.groupId, state.toGroupTitleDto()) }

    override suspend fun deleteLocal(groupId: String): Result<Unit> =
        safeSuspendCall { dataSource.deleteLocal(groupId) }
}
