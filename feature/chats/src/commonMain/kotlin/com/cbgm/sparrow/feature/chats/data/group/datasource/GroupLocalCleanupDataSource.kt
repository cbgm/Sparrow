package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipCleanupDataSource

internal class GroupLocalCleanupDataSource(
    private val chatDao: ChatDao,
    private val groupMembershipDao: GroupMembershipDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupAvatarDataSource: GroupAvatarDataSource,
    private val groupTitleDataSource: GroupTitleDataSource,
    private val groupDescriptionDataSource: GroupDescriptionDataSource,
    private val groupPinDataSource: GroupPinDataSource
) : GroupMembershipCleanupDataSource {
    override suspend fun endMembership(message: MessageEntity) {
        chatDao.applyLocalGroupRemoval(message)
        groupSecurityManager
            .retireLocalMembership(
                groupId = message.conversationId,
                retiredAtEpochMilliseconds = message.createdAtEpochMilliseconds
            ).getOrThrow()
        groupVerificationDao.deleteByGroupId(message.conversationId)
        groupMembershipDao.deleteByGroupId(message.conversationId)
    }

    override suspend fun deleteConversationHistory(
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
        groupMembershipDao.deleteByGroupId(groupId)
        groupAvatarDataSource.deleteLocal(groupId)
        groupTitleDataSource.deleteLocal(groupId)
        groupDescriptionDataSource.deleteLocal(groupId)
        groupPinDataSource.delete(groupId)
    }

    override suspend fun delete(
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
        groupMembershipDao.deleteByGroupId(groupId)
        groupAvatarDataSource.deleteLocal(groupId)
        groupTitleDataSource.deleteLocal(groupId)
        groupDescriptionDataSource.deleteLocal(groupId)
        groupPinDataSource.delete(groupId)
    }
}
