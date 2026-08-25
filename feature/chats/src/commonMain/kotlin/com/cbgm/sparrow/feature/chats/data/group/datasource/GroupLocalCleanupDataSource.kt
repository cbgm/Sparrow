package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager

internal class GroupLocalCleanupDataSource(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupAvatarDataSource: GroupAvatarDataSource,
    private val attachmentTransfer: MessageAttachmentDataSource
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
        attachmentTransfer.deleteLocalFilesForConversation(groupId)
        chatDao.hideGroupConversation(
            GroupMembershipMessageFactory.localConversationDeletedMarker(
                conversationId = groupId,
                createdAtEpochMilliseconds = deletedAtEpochMilliseconds
            )
        )
        groupVerificationDao.deleteByGroupId(groupId)
        groupInvitationDao.deleteByGroupId(groupId)
        groupAvatarDataSource.deleteLocal(groupId)
    }

    suspend fun delete(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ) {
        attachmentTransfer.deleteLocalFilesForConversation(groupId)
        chatDao.hideGroupConversation(
            GroupMembershipMessageFactory.localConversationDeletedMarker(
                conversationId = groupId,
                createdAtEpochMilliseconds = deletedAtEpochMilliseconds
            )
        )
        groupSecurityManager.deleteLocalGroup(groupId).getOrThrow()
        groupVerificationDao.deleteByGroupId(groupId)
        groupInvitationDao.deleteByGroupId(groupId)
        groupAvatarDataSource.deleteLocal(groupId)
    }
}
