package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource.GroupPinDataSource
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper.toGroupPinEntity
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper.toGroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec

internal class GroupPinRepositoryImpl(
    private val dataSource: GroupPinDataSource,
    private val groupMessageContentCodec: GroupMessageContentCodec
) : GroupPinRepository {
    override suspend fun find(groupId: String): Result<GroupPinState?> =
        safeSuspendCall { dataSource.get(groupId)?.toGroupPinState(groupMessageContentCodec) }

    override suspend fun save(pin: GroupPinState): Result<Unit> =
        safeSuspendCall { dataSource.save(pin.toGroupPinEntity(groupMessageContentCodec)) }

    override suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long): Result<Unit> =
        safeSuspendCall { dataSource.markPublished(groupId, changedAtEpochMilliseconds) }

    override suspend fun findPublishPendingGroupIds(): Result<List<String>> =
        safeSuspendCall { dataSource.findPublishPendingGroupIds() }

    override suspend fun deleteLocal(groupId: String): Result<Unit> =
        safeSuspendCall { dataSource.delete(groupId) }
}
