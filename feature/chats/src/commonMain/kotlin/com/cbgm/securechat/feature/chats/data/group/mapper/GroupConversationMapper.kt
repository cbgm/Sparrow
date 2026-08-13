package com.cbgm.securechat.feature.chats.data.group.mapper

import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.entity.MessageRecipientStateEntity
import com.cbgm.securechat.data.database.model.ConversationWithMessages
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipStateMachine
import com.cbgm.securechat.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity
import com.cbgm.securechat.feature.chats.domain.model.group.GroupConversation
import com.cbgm.securechat.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.model.group.GroupMessage
import com.cbgm.securechat.feature.chats.domain.model.group.GroupMessageDeliveryStateMachine
import com.cbgm.securechat.feature.chats.domain.model.group.MessageDeliveryProgress

internal fun ConversationWithMessages.toGroupConversation(
    participantContactIds: List<String>,
    recipientStates: List<MessageRecipientStateEntity>,
    invitations: List<GroupInvitationEntity>
): GroupConversation {
    val statesByMessageId = recipientStates.groupBy(MessageRecipientStateEntity::messageId)
    val groupState =
        GroupMembershipStateMachine.conversationState(
            invitations = invitations,
            hasLocalMembershipRemoval =
                messages.any { message ->
                    GroupMembershipMessageFactory.typeOf(message.transportMode).isLocalMembershipEnd()
                }
        )

    return GroupConversation(
        id = conversation.id,
        title = conversation.title.orEmpty(),
        messages =
            messages
                .sortedBy(MessageEntity::createdAtEpochMilliseconds)
                .map { message ->
                    message.toGroupMessage(statesByMessageId[message.id].orEmpty())
                },
        unreadCount =
            messages.count { message ->
                !message.isMine &&
                    !message.readReceiptSent &&
                    message.contentStatus == MessageContentStatus.READABLE.name
            },
        participantContactIds = participantContactIds,
        pendingParticipantCount = invitations.count { it.status.isPendingMembershipStatus() },
        isReady = groupState == GroupConversationState.READY,
        state = groupState,
        isIncomingInvitation = GroupMembershipStateMachine.isIncoming(invitations),
        memberInvitationStates = GroupMembershipStateMachine.memberStates(invitations)
    )
}

private fun MessageEntity.toGroupMessage(
    recipientStates: List<MessageRecipientStateEntity>
): GroupMessage {
    val deliveryStatus =
        if (recipientStates.isEmpty()) {
            deliveryStatus.toGroupDeliveryStatus()
        } else {
            GroupMessageDeliveryStateMachine.aggregate(
                recipientStates.map { it.deliveryStatus.toGroupDeliveryStatus() }
            )
        }

    return GroupMessage(
        id = id,
        text = text,
        isMine = isMine,
        timestamp = createdAtEpochMilliseconds,
        security = transportMode.toMessageSecurity(),
        contentStatus = contentStatus.toMessageContentStatus(),
        deliveryStatus = if (isMine) deliveryStatus else MessageDeliveryStatus.NOT_APPLICABLE,
        type = GroupMembershipMessageFactory.typeOf(transportMode),
        senderContactId = senderContactId,
        deliveryProgress = recipientStates.toDeliveryProgress()
    )
}

private fun List<MessageRecipientStateEntity>.toDeliveryProgress(): MessageDeliveryProgress =
    MessageDeliveryProgress(
        recipientCount = size,
        deliveredCount = count { it.deliveryStatus == MessageDeliveryStatus.DELIVERED.name || it.deliveryStatus == MessageDeliveryStatus.READ.name },
        readCount = count { it.deliveryStatus == MessageDeliveryStatus.READ.name }
    )

private fun String.toMessageSecurity(): MessageSecurity =
    if (this == TransportEncryptionMode.SEALED_BOX.name || this == GROUP_END_TO_END_ENCRYPTED_MODE) {
        MessageSecurity.END_TO_END_ENCRYPTED
    } else {
        MessageSecurity.INSECURE
    }

private fun String.toMessageContentStatus(): MessageContentStatus =
    MessageContentStatus.entries.firstOrNull { it.name == this }
        ?: MessageContentStatus.INVALID_PACKET

internal fun String.toGroupDeliveryStatus(): MessageDeliveryStatus =
    MessageDeliveryStatus.entries.firstOrNull { it.name == this }
        ?: MessageDeliveryStatus.NOT_APPLICABLE

private fun String.isPendingMembershipStatus(): Boolean =
    this != GroupInvitationStatus.ACTIVE.name &&
        this != GroupInvitationStatus.LEAVE_SENT.name &&
        this != GroupInvitationStatus.DECLINED.name &&
        this != GroupInvitationStatus.EXPIRED.name &&
        this != GroupInvitationStatus.FAILED.name &&
        this != GroupInvitationStatus.REMOVED.name &&
        this != GroupInvitationStatus.GROUP_DELETED.name

private fun com.cbgm.securechat.feature.chats.domain.model.group.ChatMessageType.isLocalMembershipEnd(): Boolean =
    this == com.cbgm.securechat.feature.chats.domain.model.group.ChatMessageType.LOCAL_GROUP_MEMBERSHIP_REMOVED ||
        this == com.cbgm.securechat.feature.chats.domain.model.group.ChatMessageType.LOCAL_GROUP_MEMBERSHIP_LEFT
