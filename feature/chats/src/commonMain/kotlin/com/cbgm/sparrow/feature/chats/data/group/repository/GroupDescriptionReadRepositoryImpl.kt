package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupDescriptionDataSource
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupDescription
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupDescriptionReadRepository
import kotlinx.coroutines.flow.Flow

internal class GroupDescriptionReadRepositoryImpl(
    private val dataSource: GroupDescriptionDataSource
) : GroupDescriptionReadRepository {
    override fun observe(groupId: String): Flow<GroupDescription> = dataSource.observe(groupId)
}
