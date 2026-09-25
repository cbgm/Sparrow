package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupDescription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class GroupDescriptionDataSource(
    private val dataStore: SparrowDataStore
) {
    fun observe(groupId: String): Flow<GroupDescription> {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return combine(
            dataStore.observeString(descriptionKey(groupId)),
            dataStore.observeLong(changedAtKey(groupId))
        ) { description, changedAt ->
            GroupDescription(
                groupId = groupId,
                description = description,
                changedAtEpochMilliseconds = changedAt
            )
        }
    }

    suspend fun get(groupId: String): GroupDescription {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        return GroupDescription(
            groupId = groupId,
            description = dataStore.getString(descriptionKey(groupId)),
            changedAtEpochMilliseconds = dataStore.getLong(changedAtKey(groupId))
        )
    }

    suspend fun save(
        groupId: String,
        description: String?,
        changedAtEpochMilliseconds: Long
    ) {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        dataStore.edit {
            if (description == null) {
                removeString(descriptionKey(groupId))
            } else {
                putString(descriptionKey(groupId), description)
            }
            putLong(changedAtKey(groupId), changedAtEpochMilliseconds)
        }
    }

    suspend fun deleteLocal(groupId: String) {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        dataStore.edit {
            removeString(descriptionKey(groupId))
            removeLong(changedAtKey(groupId))
        }
    }

    private fun descriptionKey(groupId: String): String = "$DESCRIPTION_PREFIX$groupId"

    private fun changedAtKey(groupId: String): String = "$CHANGED_AT_PREFIX$groupId"

    private companion object {
        const val DESCRIPTION_PREFIX = "chats.group_description.value."
        const val CHANGED_AT_PREFIX = "chats.group_description.changed_at."
    }
}
