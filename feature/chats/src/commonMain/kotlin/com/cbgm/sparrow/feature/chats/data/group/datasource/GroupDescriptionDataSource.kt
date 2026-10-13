package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.data.datastore.group.GroupMetadataPreferenceKeys
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupDescription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class GroupDescriptionDataSource(
    private val dataStore: SparrowDataStore
) {
    fun observe(groupId: String): Flow<GroupDescription> {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return combine(
            dataStore.observeString(GroupMetadataPreferenceKeys.descriptionValue(groupId)),
            dataStore.observeLong(GroupMetadataPreferenceKeys.descriptionChangedAt(groupId))
        ) { description, changedAt ->
            GroupDescription(
                groupId = groupId,
                description = description,
                changedAtEpochMilliseconds = changedAt
            )
        }
    }
}
