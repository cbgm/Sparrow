package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.datastore.SparrowDataStore

internal class GroupTitleDataSource(
    private val chatDao: ChatDao,
    private val dataStore: SparrowDataStore
) {
    suspend fun get(groupId: String): GroupTitleSnapshot {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        val conversation = chatDao.findConversationById(groupId)
            ?: error("Group conversation was not found")
        return GroupTitleSnapshot(
            title = conversation.title.orEmpty(),
            changedAtEpochMilliseconds = dataStore.getLong(changedAtKey(groupId))
        )
    }

    suspend fun save(
        groupId: String,
        title: String,
        changedAtEpochMilliseconds: Long
    ) {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(title.isNotBlank()) { "Group title must not be blank" }
        require(changedAtEpochMilliseconds > 0L) { "Group-title timestamp must be positive" }

        val updated = chatDao.updateConversationTitle(groupId, title)
        check(updated > 0) { "Group conversation was not found" }
        dataStore.edit {
            putLong(changedAtKey(groupId), changedAtEpochMilliseconds)
        }
    }

    suspend fun deleteLocal(groupId: String) {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        dataStore.edit {
            removeLong(changedAtKey(groupId))
        }
    }

    private fun changedAtKey(groupId: String): String = "$CHANGED_AT_PREFIX$groupId"

    private companion object {
        const val CHANGED_AT_PREFIX = "chats.group_title.changed_at."
    }
}

internal data class GroupTitleSnapshot(
    val title: String,
    val changedAtEpochMilliseconds: Long
)
