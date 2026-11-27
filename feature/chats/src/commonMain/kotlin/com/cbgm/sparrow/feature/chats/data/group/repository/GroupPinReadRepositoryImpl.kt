package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupPinDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.toDomain
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupPin
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinReadRepository
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class GroupPinReadRepositoryImpl(
    private val dataSource: GroupPinDataSource,
    private val groupMessageContentCodec: GroupMessageContentCodec
) : GroupPinReadRepository {
    override fun observe(groupId: String): Flow<GroupPin?> =
        dataSource
            .observe(groupId)
            .map { entity -> entity?.toDomain(groupMessageContentCodec) }
}
