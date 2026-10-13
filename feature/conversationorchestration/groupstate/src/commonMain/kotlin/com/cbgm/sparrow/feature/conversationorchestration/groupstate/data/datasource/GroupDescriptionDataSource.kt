package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.data.datastore.group.GroupMetadataPreferenceKeys
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model.GroupDescriptionDto

internal class GroupDescriptionDataSource(
    private val dataStore: SparrowDataStore
) {
    suspend fun get(groupId: String): GroupDescriptionDto =
        GroupDescriptionDto(
            description = dataStore.getString(GroupMetadataPreferenceKeys.descriptionValue(groupId)),
            changedAtEpochMilliseconds = dataStore.getLong(GroupMetadataPreferenceKeys.descriptionChangedAt(groupId))
        )

    suspend fun save(groupId: String, description: GroupDescriptionDto) {
        dataStore.edit {
            val value = description.description
            if (value == null) {
                removeString(GroupMetadataPreferenceKeys.descriptionValue(groupId))
            } else {
                putString(GroupMetadataPreferenceKeys.descriptionValue(groupId), value)
            }
            putLong(GroupMetadataPreferenceKeys.descriptionChangedAt(groupId), description.changedAtEpochMilliseconds)
        }
    }

    suspend fun deleteLocal(groupId: String) {
        dataStore.edit {
            removeString(GroupMetadataPreferenceKeys.descriptionValue(groupId))
            removeLong(GroupMetadataPreferenceKeys.descriptionChangedAt(groupId))
        }
    }
}
