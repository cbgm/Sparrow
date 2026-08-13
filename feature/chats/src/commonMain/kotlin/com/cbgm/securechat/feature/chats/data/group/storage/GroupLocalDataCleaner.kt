package com.cbgm.securechat.feature.chats.data.group.storage

import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupVerificationDao
import com.cbgm.securechat.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.securechat.feature.chats.data.group.security.GroupSecurityManager

internal class GroupLocalDataCleaner(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val groupSecurityManager: GroupSecurityManager
) {
    suspend fun delete(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ) {
        chatDao.hideGroupConversation(
            GroupMembershipMessageFactory.localConversationDeletedMarker(
                conversationId = groupId,
                createdAtEpochMilliseconds = deletedAtEpochMilliseconds
            )
        )
        groupSecurityManager.deleteLocalGroup(groupId).getOrThrow()
        groupVerificationDao.deleteByGroupId(groupId)
        groupInvitationDao.deleteByGroupId(groupId)
    }
}
