package com.cbgm.securechat.feature.chats.data.group.storage

import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupVerificationDao
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.securechat.feature.chats.data.group.security.GroupSecurityManager

internal class GroupLocalDataCleaner(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val groupSecurityManager: GroupSecurityManager
) {
    suspend fun endMembership(message: MessageEntity) {
        chatDao.applyLocalGroupRemoval(message)
        groupSecurityManager
            .retireLocalMembership(
                groupId = message.conversationId,
                retiredAtEpochMilliseconds = message.createdAtEpochMilliseconds
            ).getOrThrow()
        groupVerificationDao.deleteByGroupId(message.conversationId)
        groupInvitationDao.deleteByGroupId(message.conversationId)
    }

    suspend fun deleteConversationHistory(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ) {
        chatDao.hideGroupConversation(
            GroupMembershipMessageFactory.localConversationDeletedMarker(
                conversationId = groupId,
                createdAtEpochMilliseconds = deletedAtEpochMilliseconds
            )
        )
        groupVerificationDao.deleteByGroupId(groupId)
        groupInvitationDao.deleteByGroupId(groupId)
    }

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
