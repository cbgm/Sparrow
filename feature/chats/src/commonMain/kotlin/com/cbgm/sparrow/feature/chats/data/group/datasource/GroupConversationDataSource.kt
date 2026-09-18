package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory

internal class GroupConversationDataSource(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupSecurityDao: GroupSecurityDao
) {
    suspend fun getContext(groupId: String): GroupConversationContextDto {
        val conversation =
            requireNotNull(chatDao.findConversationById(groupId)) {
                "Group conversation was not found"
            }
        check(conversation.type == GROUP_CONVERSATION_TYPE) {
            "Conversation is not a group"
        }
        return GroupConversationContextDto(
            title = requireNotNull(conversation.title) { "Group title was not found" },
            createdAtEpochMilliseconds = conversation.createdAtEpochMilliseconds
        )
    }

    suspend fun stageIncoming(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Boolean {
        val localDeletionTimestamp =
            chatDao.findMessageTimestampByTransportMode(
                conversationId = groupId,
                transportMode = GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE
            )
        if (localDeletionTimestamp != null) {
            if (createdAtEpochMilliseconds <= localDeletionTimestamp) {
                return false
            }
            chatDao.deleteConversationMessages(groupId)
        }

        val existing = chatDao.findConversationById(groupId)
        val hasHistory = chatDao.hasMessages(groupId)
        chatDao.upsertConversation(
            ConversationEntity(
                id = groupId,
                contactId = null,
                type = GROUP_CONVERSATION_TYPE,
                title = title,
                createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = maxOf(createdAtEpochMilliseconds, updatedAtEpochMilliseconds),
                unseenLocalMessageCount = existing?.unseenLocalMessageCount ?: 0,
                isVisible = existing?.isVisible == true && hasHistory
            )
        )
        return true
    }

    suspend fun discardPending(
        groupId: String,
        updatedAtEpochMilliseconds: Long
    ) {
        if (chatDao.hasMessages(groupId)) return
        chatDao.hideGroupConversation(
            GroupMembershipMessageFactory.localConversationDeletedMarker(
                conversationId = groupId,
                createdAtEpochMilliseconds = updatedAtEpochMilliseconds
            )
        )
    }

    suspend fun addParticipant(
        groupId: String,
        peerId: String,
        joinedAtEpochMilliseconds: Long,
        eventId: String
    ) {
        val securityState =
            requireNotNull(groupSecurityDao.findState(groupId)) {
                "Group security state was not found"
            }
        val contactName = contactDisplayName(peerId)

        chatDao.upsertConversationParticipant(
            ConversationParticipantEntity(
                conversationId = groupId,
                contactId = peerId,
                role = MEMBER_ROLE,
                joinedAtEpochMilliseconds = joinedAtEpochMilliseconds
            )
        )
        chatDao.upsertMessage(
            GroupMembershipMessageFactory.memberAdded(
                conversationId = groupId,
                epoch = securityState.currentEpoch,
                contactId = peerId,
                contactName = contactName,
                createdAtEpochMilliseconds = joinedAtEpochMilliseconds,
                eventId = eventId
            )
        )
        chatDao.updateConversationTimestamp(groupId, joinedAtEpochMilliseconds)
    }

    suspend fun promoteParticipant(
        groupId: String,
        peerId: String,
        updatedAtEpochMilliseconds: Long
    ) {
        val changed =
            chatDao.updateConversationParticipantRole(
                conversationId = groupId,
                contactId = peerId,
                role = ADMIN_ROLE
            )
        if (changed == 0) {
            chatDao.upsertConversationParticipant(
                ConversationParticipantEntity(
                    conversationId = groupId,
                    contactId = peerId,
                    role = ADMIN_ROLE,
                    joinedAtEpochMilliseconds = updatedAtEpochMilliseconds
                )
            )
        }
        chatDao.updateConversationTimestamp(groupId, updatedAtEpochMilliseconds)
    }

    suspend fun removeParticipant(
        groupId: String,
        peerId: String,
        epoch: Int,
        eventId: String,
        updatedAtEpochMilliseconds: Long,
        memberLeft: Boolean
    ) {
        chatDao.deleteConversationParticipant(groupId, peerId)
        val contactName = contactDisplayName(peerId)
        val message =
            if (memberLeft) {
                GroupMembershipMessageFactory.memberLeft(
                    conversationId = groupId,
                    epoch = epoch,
                    contactId = peerId,
                    contactName = contactName,
                    createdAtEpochMilliseconds = updatedAtEpochMilliseconds,
                    eventId = eventId
                )
            } else {
                GroupMembershipMessageFactory.memberRemoved(
                    conversationId = groupId,
                    epoch = epoch,
                    contactId = peerId,
                    contactName = contactName,
                    createdAtEpochMilliseconds = updatedAtEpochMilliseconds,
                    eventId = eventId
                )
            }
        chatDao.upsertMessage(message)
        chatDao.updateConversationTimestamp(groupId, updatedAtEpochMilliseconds)
    }

    private suspend fun contactDisplayName(peerId: String): String =
        contactDao
            .findById(peerId)
            ?.contact
            ?.displayName
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: "Member"

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val MEMBER_ROLE = "MEMBER"
        const val ADMIN_ROLE = "ADMIN"
    }
}

internal data class GroupConversationContextDto(
    val title: String,
    val createdAtEpochMilliseconds: Long
)
