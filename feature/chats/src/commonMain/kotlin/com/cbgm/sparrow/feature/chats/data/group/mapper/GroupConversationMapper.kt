package com.cbgm.sparrow.feature.chats.data.group.mapper

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.data.database.model.ConversationWithMessages
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.sparrow.feature.chats.data.mapper.toDomain
import com.cbgm.sparrow.feature.chats.data.model.MessagePartDto
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.chats.domain.model.group.MessageDeliveryProgress

internal fun ConversationWithMessages.toGroupConversation(
    participantContactIds: List<String>,
    recipientStates: List<MessageRecipientStateEntity>,
    invitations: List<GroupInvitationEntity>,
    verificationRows: List<GroupVerificationPairEntity> = emptyList(),
    partsByMessageId: Map<String, List<MessagePartDto>> = emptyMap()
): GroupConversation {
    val timeline = buildGroupLocalMembershipTimeline(messages, invitations)
    val visibleMessages = timeline.visibleMessages
    val statesByMessageId = recipientStates.groupBy(MessageRecipientStateEntity::messageId)
    val groupState =
        GroupMembershipStateMachine.conversationState(
            invitations = timeline.currentInvitations,
            isLocallyInactive = timeline.isLocallyInactive
        )

    return GroupConversation(
        id = conversation.id,
        title = conversation.title.orEmpty(),
        messages =
            visibleMessages
                .map { message ->
                    message.toGroupMessage(
                        recipientStates = statesByMessageId[message.id].orEmpty(),
                        attachmentParts = partsByMessageId[message.id].orEmpty()
                    )
                },
        unreadCount =
            visibleMessages.count { message ->
                !message.isMine &&
                    !message.readReceiptSent &&
                    message.contentStatus == MessageContentStatus.READABLE.name
            },
        participantContactIds = participantContactIds,
        pendingParticipantCount =
            if (verificationRows.isNotEmpty()) {
                verificationRows.count { row ->
                    row.membershipStatus == GroupVerificationPairEntity.PENDING_STATUS
                }
            } else {
                timeline.currentInvitations.count { it.status.isPendingMembershipStatus() }
            },
        isReady = groupState == GroupConversationState.READY,
        state = groupState,
        isIncomingInvitation = GroupMembershipStateMachine.isIncoming(timeline.currentInvitations),
        memberInvitationStates = GroupMembershipStateMachine.memberStates(timeline.currentInvitations)
    )
}

private fun MessageEntity.toGroupMessage(
    recipientStates: List<MessageRecipientStateEntity>,
    attachmentParts: List<MessagePartDto>
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
        isMine = isMine,
        timestamp = createdAtEpochMilliseconds,
        security = transportMode.toMessageSecurity(),
        contentStatus = contentStatus.toMessageContentStatus(),
        deliveryStatus = if (isMine) deliveryStatus else MessageDeliveryStatus.NOT_APPLICABLE,
        type = GroupMembershipMessageFactory.typeOf(transportMode),
        senderContactId = senderContactId,
        deliveryProgress = recipientStates.toDeliveryProgress(),
        parts =
            buildList {
                text
                    .takeIf(String::isNotBlank)
                    ?.let { value -> add(MessagePartDto.TextDto(text = value)) }
                addAll(attachmentParts)
            }.map { part -> part.toDomain() }
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
    this == GroupInvitationStatus.INVITE_SENT.name ||
        this == GroupInvitationStatus.INVITE_RECEIVED.name ||
        this == GroupInvitationStatus.WAITING_FOR_IDENTITY.name ||
        this == GroupInvitationStatus.IDENTITY_READY.name ||
        this == GroupInvitationStatus.WELCOME_SENT.name
