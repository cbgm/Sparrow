package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.GroupMetadataDao
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupDescription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class GroupDescriptionDataSource(
    private val groupMetadataDao: GroupMetadataDao
) {
    fun observe(groupId: String): Flow<GroupDescription> {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return groupMetadataDao.observe(groupId)
            .map { metadata ->
                GroupDescription(
                    groupId = groupId,
                    description = metadata?.description,
                    changedAtEpochMilliseconds = metadata?.descriptionChangedAtEpochMilliseconds ?: 0L
                )
            }.distinctUntilChanged()
    }
}
