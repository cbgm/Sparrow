package com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper

import com.cbgm.sparrow.core.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.model.GroupSystemMessageDto

/** Builds the local system rows that record group membership changes in the conversation history. */
internal object GroupSystemMessageFactory {
    fun memberAdded(
        conversationId: String,
        epoch: Int,
        contactId: String,
        contactName: String,
        createdAtEpochMilliseconds: Long,
        eventId: String = "$epoch-$contactId"
    ): GroupSystemMessageDto =
        systemMessage(
            id = "group-member-added-$conversationId-$eventId",
            conversationId = conversationId,
            text = "$contactName was added to the group",
            transportMode = MessageTransportMode.GROUP_MEMBER_ADDED,
            senderContactId = contactId,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    fun memberRemoved(
        conversationId: String,
        epoch: Int,
        contactId: String,
        contactName: String,
        createdAtEpochMilliseconds: Long,
        eventId: String = "$epoch-$contactId"
    ): GroupSystemMessageDto =
        systemMessage(
            id = "group-member-removed-$conversationId-$eventId",
            conversationId = conversationId,
            text = "$contactName was removed from the group",
            transportMode = MessageTransportMode.GROUP_MEMBER_REMOVED,
            senderContactId = contactId,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    fun localMembershipRemoved(
        conversationId: String,
        invitationId: String,
        epoch: Int,
        createdAtEpochMilliseconds: Long
    ): GroupSystemMessageDto =
        systemMessage(
            id = "group-local-membership-removed-$invitationId-$epoch",
            conversationId = conversationId,
            text = "You were removed from this group",
            transportMode = MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_REMOVED,
            senderContactId = null,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    fun localMembershipStarted(
        conversationId: String,
        referenceId: String,
        epoch: Int,
        createdAtEpochMilliseconds: Long
    ): GroupSystemMessageDto =
        systemMessage(
            id = "group-local-membership-started-$referenceId-$epoch",
            conversationId = conversationId,
            text = "",
            transportMode = MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_STARTED,
            senderContactId = null,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    fun memberLeft(
        conversationId: String,
        epoch: Int,
        contactId: String,
        contactName: String,
        createdAtEpochMilliseconds: Long,
        eventId: String = "$epoch-$contactId"
    ): GroupSystemMessageDto =
        systemMessage(
            id = "group-member-left-$conversationId-$eventId",
            conversationId = conversationId,
            text = "$contactName left the group",
            transportMode = MessageTransportMode.GROUP_MEMBER_LEFT,
            senderContactId = contactId,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    fun localMembershipLeft(
        conversationId: String,
        invitationId: String,
        epoch: Int,
        createdAtEpochMilliseconds: Long
    ): GroupSystemMessageDto =
        systemMessage(
            id = "group-local-membership-left-$invitationId-$epoch",
            conversationId = conversationId,
            text = "You left this group",
            transportMode = MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_LEFT,
            senderContactId = null,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    fun localConversationDeletedMarker(
        conversationId: String,
        createdAtEpochMilliseconds: Long
    ): GroupSystemMessageDto =
        systemMessage(
            id = "local-conversation-deleted-$conversationId",
            conversationId = conversationId,
            text = "",
            transportMode = MessageTransportMode.LOCAL_CONVERSATION_DELETED,
            senderContactId = null,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    private fun systemMessage(
        id: String,
        conversationId: String,
        text: String,
        transportMode: String,
        senderContactId: String?,
        createdAtEpochMilliseconds: Long
    ): GroupSystemMessageDto =
        GroupSystemMessageDto(
            message = MessageEntity(
                id = id,
                conversationId = conversationId,
                packetId = null,
                transportPayload = null,
                transportMode = transportMode,
                contentStatus = MessageContentStatus.READABLE.name,
                deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
                senderContactId = senderContactId,
                isMine = true,
                createdAtEpochMilliseconds = createdAtEpochMilliseconds
            ),
            text = text
        )
}
