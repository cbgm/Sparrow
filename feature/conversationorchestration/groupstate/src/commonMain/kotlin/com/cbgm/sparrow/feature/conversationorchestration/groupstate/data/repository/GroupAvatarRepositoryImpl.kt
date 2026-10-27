package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper.toGroupAvatarDto
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper.toGroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository

internal class GroupAvatarRepositoryImpl(
    private val dataSource: GroupAvatarDataSource
) : GroupAvatarRepository {
    override suspend fun get(groupId: String): Result<GroupAvatarState> =
        safeSuspendCall { dataSource.get(groupId).toGroupAvatarState(groupId) }

    override suspend fun save(state: GroupAvatarState): Result<Unit> =
        safeSuspendCall { dataSource.save(state.groupId, state.toGroupAvatarDto()) }

    override suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long): Result<Unit> =
        safeSuspendCall { dataSource.markPublished(groupId, changedAtEpochMilliseconds) }

    override suspend fun findPublishPendingGroupIds(): Result<List<String>> =
        safeSuspendCall { dataSource.findPublishPendingGroupIds() }

    override suspend fun deleteLocal(groupId: String): Result<Unit> =
        safeSuspendCall { dataSource.deleteLocal(groupId) }
}
