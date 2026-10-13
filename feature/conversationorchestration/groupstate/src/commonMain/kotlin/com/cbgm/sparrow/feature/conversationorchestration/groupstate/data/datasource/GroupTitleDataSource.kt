package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.data.datastore.group.GroupMetadataPreferenceKeys
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model.GroupTitleDto

/** The title lives on the conversation row; its change timestamp lives in the preference store. */
internal class GroupTitleDataSource(
    private val chatDao: ChatDao,
    private val dataStore: SparrowDataStore
) {
    suspend fun get(groupId: String): GroupTitleDto {
        val conversation = chatDao.findConversationById(groupId) ?: error("Group conversation was not found")
        return GroupTitleDto(
            title = conversation.title.orEmpty(),
            changedAtEpochMilliseconds = dataStore.getLong(GroupMetadataPreferenceKeys.titleChangedAt(groupId))
        )
    }

    suspend fun save(groupId: String, title: GroupTitleDto) {
        val updated = chatDao.updateConversationTitle(groupId, title.title)
        check(updated > 0) { "Group conversation was not found" }
        dataStore.edit {
            putLong(GroupMetadataPreferenceKeys.titleChangedAt(groupId), title.changedAtEpochMilliseconds)
        }
    }

    suspend fun deleteLocal(groupId: String) {
        dataStore.edit { removeLong(GroupMetadataPreferenceKeys.titleChangedAt(groupId)) }
    }
}
