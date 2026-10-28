package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
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

    override suspend fun saveIfVersion(
        state: GroupDescriptionState,
        expectedChangedAtEpochMilliseconds: Long
    ): Result<Boolean> =
        safeSuspendCall {
            dataSource.saveIfVersion(state.groupId, state.toGroupDescriptionDto(), expectedChangedAtEpochMilliseconds)
        }

    override suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long): Result<Unit> =
        safeSuspendCall { dataSource.markPublished(groupId, changedAtEpochMilliseconds) }

    override suspend fun findPublishPendingGroupIds(): Result<List<String>> =
        safeSuspendCall { dataSource.findPublishPendingGroupIds() }

    override suspend fun deleteLocal(groupId: String): Result<Unit> =
        safeSuspendCall { dataSource.deleteLocal(groupId) }
}
