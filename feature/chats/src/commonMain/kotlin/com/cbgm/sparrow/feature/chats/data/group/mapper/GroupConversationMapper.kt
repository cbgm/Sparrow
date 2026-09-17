package com.cbgm.sparrow.feature.chats.data.group.mapper

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.data.database.model.ConversationWithMessagesDto
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.sparrow.feature.chats.data.mapper.toMessagePart
import com.cbgm.sparrow.feature.chats.data.model.MessagePartDto
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageReaction
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.chats.domain.model.group.MessageDeliveryProgress
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.domain.model.GroupConversationState

internal fun ConversationWithMessagesDto.toGroupConversation(
    participantContactIds: List<String>,
    recipientStates: List<MessageRecipientStateEntity>,
    memberships: List<GroupMembershipEntity>,
    verificationRows: List<GroupVerificationPairEntity> = emptyList(),
    partsByMessageId: Map<String, List<MessagePartDto>> = emptyMap(),
    reactionsByMessageId: Map<String, List<MessageReaction>> = emptyMap(),
    localMembershipHistory: List<MessageEntity> = messages
): GroupConversation {
    val timeline =
        buildGroupLocalMembershipTimeline(
            messages = messages,
            memberships = memberships,
            localMembershipHistory = localMembershipHistory
        )
    val visibleMessages = timeline.visibleMessages
    val statesByMessageId = recipientStates.groupBy(MessageRecipientStateEntity::messageId)
    val groupState =
        GroupMembershipStateMachine.conversationState(
            memberships = timeline.currentMemberships,
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
                        attachmentParts = partsByMessageId[message.id].orEmpty(),
                        reactions = reactionsByMessageId[message.id].orEmpty()
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
                timeline.currentMemberships.count { it.status.isPendingMembershipStatus() }
            },
        isReady = groupState == GroupConversationState.READY,
        state = groupState,
        memberProgress = GroupMembershipStateMachine.memberProgress(timeline.currentMemberships)
    )
}

private fun MessageEntity.toGroupMessage(
    recipientStates: List<MessageRecipientStateEntity>,
    attachmentParts: List<MessagePartDto>,
    reactions: List<MessageReaction>
): GroupMessage {
    val deliveryStatus =
        if (recipientStates.isEmpty()) {
            deliveryStatus.toMessageDeliveryStatus()
        } else {
            GroupMessageDeliveryStateMachine.aggregate(
                recipientStates.map { it.deliveryStatus.toMessageDeliveryStatus() }
            )
        }

    return GroupMessage(
        id = id,
        isMine = isMine,
        timestamp = createdAtEpochMilliseconds,
        security = transportMode.toMessageSecurity(),
        contentStatus = contentStatus.toMessageContentStatus(),
        deliveryStatus = if (isMine) deliveryStatus else MessageDeliveryStatus.NOT_APPLICABLE,
        replyToMessageId = replyToMessageId,
        reactions = reactions,
        type = GroupMembershipMessageFactory.typeOf(transportMode),
        senderContactId = senderContactId,
        deliveryProgress = recipientStates.toMessageDeliveryProgress(),
        parts =
            buildList {
                text
                    .takeIf(String::isNotBlank)
                    ?.let { value -> add(MessagePartDto.TextDto(text = value)) }
                addAll(attachmentParts)
            }.map { part -> part.toMessagePart() }
    )
}

private fun List<MessageRecipientStateEntity>.toMessageDeliveryProgress(): MessageDeliveryProgress =
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

internal fun String.toMessageDeliveryStatus(): MessageDeliveryStatus =
    MessageDeliveryStatus.entries.firstOrNull { it.name == this }
        ?: MessageDeliveryStatus.NOT_APPLICABLE

private fun String.isPendingMembershipStatus(): Boolean =
    this == GroupMembershipStatus.STAGED.name ||
        this == GroupMembershipStatus.IDENTITY_READY.name ||
        this == GroupMembershipStatus.JOIN_REQUEST_SENT.name ||
        this == GroupMembershipStatus.WELCOME_SENT.name ||
        this == GroupMembershipStatus.WAITING_FOR_ACTIVATION.name
