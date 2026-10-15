package com.cbgm.sparrow.feature.conversationorchestration.membership.data.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper.GroupSystemMessageFactory
import com.cbgm.sparrow.feature.membership.crypto.GroupKeyStore

/** Local conversation cleanup after the local member left or deleted a group. */
internal class GroupLocalCleanupDataSource(
    private val chatDao: ChatDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val groupKeyDataSource: GroupKeyStore
) {
    suspend fun endMembership(
        groupId: String,
        referenceId: String,
        epoch: Int,
        endedAtEpochMilliseconds: Long
    ) {
        val message =
            GroupSystemMessageFactory.localMembershipLeft(
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
            GroupSystemMessageFactory.localConversationDeletedMarker(
                conversationId = groupId,
                createdAtEpochMilliseconds = deletedAtEpochMilliseconds
            )
        chatDao.hideGroupConversation(marker.message, marker.text)
        groupKeyDataSource.deleteGroup(groupId)
        groupVerificationDao.deleteByGroupId(groupId)
    }
}
