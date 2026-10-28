package com.cbgm.sparrow.feature.conversationorchestration.membership.data.datasource

import com.cbgm.sparrow.core.base.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupMetadataDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper.GroupSystemMessageFactory
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupConversationKind

internal class GroupConversationDataSource(
    private val chatDao: ChatDao,
    private val groupMetadataDao: GroupMetadataDao
) {
    suspend fun create(conversation: ConversationEntity, title: String) {
        chatDao.upsertConversation(conversation)
        groupMetadataDao.upsertTitleValue(conversation.id, title)
    }

    suspend fun getTitle(groupId: String): String =
        requireNotNull(groupMetadataDao.get(groupId)) { "Group title was not found" }.title

    suspend fun getGroupConversation(groupId: String): ConversationEntity {
        val conversation =
            requireNotNull(chatDao.findConversationById(groupId)) {
                "Group conversation was not found"
            }
        check(conversation.type == GROUP_CONVERSATION_TYPE) {
            "Conversation is not a group"
        }
        return conversation
    }

    suspend fun findKind(groupId: String): GroupConversationKind =
        when (chatDao.findConversationById(groupId)?.type) {
            null -> GroupConversationKind.NONE
            GROUP_CONVERSATION_TYPE -> GroupConversationKind.GROUP
            else -> GroupConversationKind.OTHER
        }

    suspend fun isParticipant(groupId: String, contactId: String): Boolean =
        chatDao.findConversationParticipants(groupId).any { participant -> participant.contactId == contactId }

    suspend fun stageIncoming(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Boolean {
        val localDeletionTimestamp =
            chatDao.findMessageTimestampByTransportMode(
                conversationId = groupId,
                transportMode = MessageTransportMode.LOCAL_CONVERSATION_DELETED
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
                createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = maxOf(createdAtEpochMilliseconds, updatedAtEpochMilliseconds),
                unseenLocalMessageCount = existing?.unseenLocalMessageCount ?: 0,
                isVisible = existing?.isVisible == true && hasHistory
            )
        )
        // An invitation is unsigned by the group's admin history; it never replaces a versioned title, the
        // welcome sets the title of a re-joined group.
        groupMetadataDao.upsertTitleValueIfUnversioned(groupId, title)
        return true
    }

    /** An accepted invitation owns a visible conversation even before the signed welcome arrives.
     * Membership still controls when the receiver can send group messages.
     */
    suspend fun showAcceptedIncoming(groupId: String) {
        val existing = requireNotNull(chatDao.findConversationById(groupId)) {
            "The accepted group invitation has no staged conversation"
        }
        check(existing.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }
        // Do not resurrect a group that the user explicitly removed.
        val deletedAt = chatDao.findMessageTimestampByTransportMode(
            conversationId = groupId,
            transportMode = MessageTransportMode.LOCAL_CONVERSATION_DELETED
        )
        if (deletedAt != null && existing.createdAtEpochMilliseconds <= deletedAt) return
        if (!existing.isVisible) chatDao.upsertConversation(existing.copy(isVisible = true))
    }

    suspend fun discardPending(
        groupId: String,
        updatedAtEpochMilliseconds: Long
    ) {
        if (chatDao.hasMessages(groupId)) return
        val marker =
            GroupSystemMessageFactory.localConversationDeletedMarker(
                conversationId = groupId,
                createdAtEpochMilliseconds = updatedAtEpochMilliseconds
            )
        chatDao.hideGroupConversation(marker.message, marker.text)
    }

    suspend fun addParticipant(
        groupId: String,
        peerId: String,
        memberDisplayName: String,
        epoch: Int,
        joinedAtEpochMilliseconds: Long,
        eventId: String
    ) {
        // The membership result stream also replays ACTIVE for members who were
        // already in the conversation. Those replays must never produce another
        // member-added event (or refresh an existing participant's join time).
        val wasAlreadyActive = chatDao.findConversationParticipants(groupId)
            .any { participant -> participant.contactId == peerId }
        if (wasAlreadyActive) return

        chatDao.upsertConversationParticipant(
            ConversationParticipantEntity(
                conversationId = groupId,
                contactId = peerId,
                role = MEMBER_ROLE,
                joinedAtEpochMilliseconds = joinedAtEpochMilliseconds
            )
        )
        val message =
            GroupSystemMessageFactory.memberAdded(
                conversationId = groupId,
                epoch = epoch,
                contactId = peerId,
                contactName = memberDisplayName,
                createdAtEpochMilliseconds = joinedAtEpochMilliseconds,
                eventId = eventId
            )
        chatDao.upsertMessageWithText(message.message, message.text)
        chatDao.updateConversationTimestamp(groupId, joinedAtEpochMilliseconds)
    }

    /** A signed DISCOVERY packet announces exactly the member being activated.
     * Re-adds may leave old participant rows on another device; presence in that
     * projection must not hide the new, independently signed activation event.
     * This history event does not grant message-routing access before FINAL. */
    suspend fun recordRemoteMemberAdded(
        groupId: String,
        peerId: String,
        epoch: Int,
        activationId: String,
        memberDisplayName: String,
        joinedAtEpochMilliseconds: Long
    ) {
        val event = GroupSystemMessageFactory.memberAdded(
            conversationId = groupId,
            epoch = epoch,
            contactId = peerId,
            contactName = memberDisplayName,
            createdAtEpochMilliseconds = joinedAtEpochMilliseconds,
            eventId = activationId
        )
        // Deduplicate by the verified activation itself, not by an old participant
        // row that can survive a leave/removal on another emulator.
        if (chatDao.findMessageById(event.message.id) == null) {
            chatDao.upsertMessageWithText(event.message, event.text)
            chatDao.updateConversationTimestamp(groupId, joinedAtEpochMilliseconds)
        }
    }

    /** A rotated welcome contains a future member's key, but only the authenticated
     * FINAL activation makes them an active conversation participant. Record the
     * system event here, not when the welcome is installed. */
    suspend fun activateRemoteParticipant(
        groupId: String,
        peerId: String,
        role: String,
        epoch: Int,
        activationId: String,
        memberDisplayName: String,
        joinedAtEpochMilliseconds: Long
    ) {
        val alreadyPresent = chatDao.findConversationParticipants(groupId)
            .any { participant -> participant.contactId == peerId }
        chatDao.upsertConversationParticipant(
            ConversationParticipantEntity(
                conversationId = groupId,
                contactId = peerId,
                role = role,
                joinedAtEpochMilliseconds = joinedAtEpochMilliseconds
            )
        )
        if (!alreadyPresent) {
            val event = GroupSystemMessageFactory.memberAdded(
                conversationId = groupId,
                epoch = epoch,
                contactId = peerId,
                contactName = memberDisplayName,
                createdAtEpochMilliseconds = joinedAtEpochMilliseconds,
                eventId = activationId
            )
            // DISCOVERY may already have logged this signed activation. FINAL
            // must not rewrite its timestamp or duplicate the system event.
            if (chatDao.findMessageById(event.message.id) == null) {
                chatDao.upsertMessageWithText(event.message, event.text)
                chatDao.updateConversationTimestamp(groupId, joinedAtEpochMilliseconds)
            }
        }
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
        memberDisplayName: String,
        epoch: Int,
        eventId: String,
        updatedAtEpochMilliseconds: Long,
        memberLeft: Boolean
    ) {
        chatDao.deleteConversationParticipant(groupId, peerId)
        val contactName = memberDisplayName
        val message =
            if (memberLeft) {
                GroupSystemMessageFactory.memberLeft(
                    conversationId = groupId,
                    epoch = epoch,
                    contactId = peerId,
                    contactName = contactName,
                    createdAtEpochMilliseconds = updatedAtEpochMilliseconds,
                    eventId = eventId
                )
            } else {
                GroupSystemMessageFactory.memberRemoved(
                    conversationId = groupId,
                    epoch = epoch,
                    contactId = peerId,
                    contactName = contactName,
                    createdAtEpochMilliseconds = updatedAtEpochMilliseconds,
                    eventId = eventId
                )
            }
        chatDao.upsertMessageWithText(message.message, message.text)
        chatDao.updateConversationTimestamp(groupId, updatedAtEpochMilliseconds)
    }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val MEMBER_ROLE = "MEMBER"
        const val ADMIN_ROLE = "ADMIN"
    }
}
