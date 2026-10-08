package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.membership.crypto.GroupKeyStore

/** Chats-owned side of membership cleanup; Membership never invokes this datasource directly. */
internal class GroupLocalCleanupDataSource(
    private val chatDao: ChatDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val groupKeyDataSource: GroupKeyStore,
    private val groupAvatarDataSource: GroupAvatarDataSource,
    private val groupTitleDataSource: GroupTitleDataSource,
    private val groupDescriptionDataSource: GroupDescriptionDataSource,
    private val groupPinDataSource: GroupPinDataSource
) {
    suspend fun endMembership(
        groupId: String,
        referenceId: String,
        epoch: Int,
        endedAtEpochMilliseconds: Long
    ) {
        val message =
            GroupMembershipMessageFactory.localMembershipLeft(
                conversationId = groupId,
                invitationId = referenceId,
                epoch = epoch,
                createdAtEpochMilliseconds = endedAtEpochMilliseconds
            )
        chatDao.applyLocalGroupRemoval(message.message, message.text)
        groupKeyDataSource.deleteGroup(groupId)
        groupVerificationDao.deleteByGroupId(groupId)
    }

    suspend fun deleteConversationHistory(groupId: String, deletedAtEpochMilliseconds: Long) {
        val marker =
            GroupMembershipMessageFactory.localConversationDeletedMarker(
                conversationId = groupId,
                createdAtEpochMilliseconds = deletedAtEpochMilliseconds
            )
        chatDao.hideGroupConversation(marker.message, marker.text)
        groupKeyDataSource.deleteGroup(groupId)
        groupVerificationDao.deleteByGroupId(groupId)
        groupAvatarDataSource.deleteLocal(groupId)
        groupTitleDataSource.deleteLocal(groupId)
        groupDescriptionDataSource.deleteLocal(groupId)
        groupPinDataSource.delete(groupId)
    }
}
