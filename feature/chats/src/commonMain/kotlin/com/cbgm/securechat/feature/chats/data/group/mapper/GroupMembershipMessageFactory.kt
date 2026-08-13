package com.cbgm.securechat.feature.chats.data.group.mapper

import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.group.ChatMessageType

internal object GroupMembershipMessageFactory {
    const val MEMBER_ADDED_TRANSPORT_MODE = "SYSTEM_GROUP_MEMBER_ADDED"
    const val MEMBER_REMOVED_TRANSPORT_MODE = "SYSTEM_GROUP_MEMBER_REMOVED"
    const val LOCAL_MEMBERSHIP_REMOVED_TRANSPORT_MODE = "SYSTEM_LOCAL_GROUP_MEMBERSHIP_REMOVED"
    const val MEMBER_LEFT_TRANSPORT_MODE = "SYSTEM_GROUP_MEMBER_LEFT"
    const val LOCAL_MEMBERSHIP_LEFT_TRANSPORT_MODE = "SYSTEM_LOCAL_GROUP_MEMBERSHIP_LEFT"
    const val LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE = "SYSTEM_LOCAL_CONVERSATION_DELETED"

    fun memberAdded(
        conversationId: String,
        epoch: Int,
        contactId: String,
        contactName: String,
        createdAtEpochMilliseconds: Long,
        eventId: String = "$epoch-$contactId"
    ): MessageEntity =
        systemMessage(
            id = "group-member-added-$conversationId-$eventId",
            conversationId = conversationId,
            text = "$contactName was added to the group",
            transportMode = MEMBER_ADDED_TRANSPORT_MODE,
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
    ): MessageEntity =
        systemMessage(
            id = "group-member-removed-$conversationId-$eventId",
            conversationId = conversationId,
            text = "$contactName was removed from the group",
            transportMode = MEMBER_REMOVED_TRANSPORT_MODE,
            senderContactId = contactId,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    fun localMembershipRemoved(
        conversationId: String,
        invitationId: String,
        epoch: Int,
        createdAtEpochMilliseconds: Long
    ): MessageEntity =
        systemMessage(
            id = "group-local-membership-removed-$invitationId-$epoch",
            conversationId = conversationId,
            text = "You were removed from this group",
            transportMode = LOCAL_MEMBERSHIP_REMOVED_TRANSPORT_MODE,
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
    ): MessageEntity =
        systemMessage(
            id = "group-member-left-$conversationId-$eventId",
            conversationId = conversationId,
            text = "$contactName left the group",
            transportMode = MEMBER_LEFT_TRANSPORT_MODE,
            senderContactId = contactId,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    fun localMembershipLeft(
        conversationId: String,
        invitationId: String,
        epoch: Int,
        createdAtEpochMilliseconds: Long
    ): MessageEntity =
        systemMessage(
            id = "group-local-membership-left-$invitationId-$epoch",
            conversationId = conversationId,
            text = "You left this group",
            transportMode = LOCAL_MEMBERSHIP_LEFT_TRANSPORT_MODE,
            senderContactId = null,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    fun localConversationDeletedMarker(
        conversationId: String,
        createdAtEpochMilliseconds: Long
    ): MessageEntity =
        systemMessage(
            id = "local-conversation-deleted-$conversationId",
            conversationId = conversationId,
            text = "",
            transportMode = LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE,
            senderContactId = null,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

    fun typeOf(transportMode: String): ChatMessageType =
        when (transportMode) {
            MEMBER_ADDED_TRANSPORT_MODE -> ChatMessageType.GROUP_MEMBER_ADDED
            MEMBER_REMOVED_TRANSPORT_MODE -> ChatMessageType.GROUP_MEMBER_REMOVED
            LOCAL_MEMBERSHIP_REMOVED_TRANSPORT_MODE -> ChatMessageType.LOCAL_GROUP_MEMBERSHIP_REMOVED
            MEMBER_LEFT_TRANSPORT_MODE -> ChatMessageType.GROUP_MEMBER_LEFT
            LOCAL_MEMBERSHIP_LEFT_TRANSPORT_MODE -> ChatMessageType.LOCAL_GROUP_MEMBERSHIP_LEFT
            else -> ChatMessageType.USER
        }

    private fun systemMessage(
        id: String,
        conversationId: String,
        text: String,
        transportMode: String,
        senderContactId: String?,
        createdAtEpochMilliseconds: Long
    ): MessageEntity =
        MessageEntity(
            id = id,
            conversationId = conversationId,
            packetId = null,
            text = text,
            transportPayload = null,
            transportMode = transportMode,
            contentStatus = MessageContentStatus.READABLE.name,
            deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
            senderContactId = senderContactId,
            isMine = true,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )
}
