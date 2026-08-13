package com.cbgm.securechat.feature.chats.data.group.outgoing

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.ReadReceiptPacket
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.MessageRecipientStateDao
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.entity.MessageRecipientStateEntity
import com.cbgm.securechat.feature.chats.data.group.delivery.GroupMessageDeliveryCoordinator
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.group.invitation.canSendToActiveGroupMembers
import com.cbgm.securechat.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.securechat.feature.chats.data.group.mapper.toGroupDeliveryStatus
import com.cbgm.securechat.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.securechat.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.group.GroupMessageDeliveryStateMachine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns every outgoing group-message operation.
 *
 * Red line:
 * Group use case -> GroupConversationRepositoryImpl -> this processor -> ProtocolOutbox.
 */
class GroupOutgoingMessageProcessor(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val messageRecipientStateDao: MessageRecipientStateDao,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val groupSecurityManager: GroupSecurityManager,
    private val deliveryCoordinator: GroupMessageDeliveryCoordinator
) {
    private val sendMutex = Mutex()
    private val logger = SecureChatLog.withTag("GroupOutgoingMessageProcessor")

    suspend fun send(
        groupId: String,
        text: String,
        invitations: List<GroupInvitationEntity>
    ): Result<Unit> =
        runCatching {
            sendMutex.withLock {
                val normalizedText = text.trim()
                require(normalizedText.isNotEmpty()) { "Message text must not be blank" }

                requireActiveMembership(groupId, invitations)
                val message = createQueuedMessage(groupId, normalizedText)
                val participants = findCurrentEpochParticipants(groupId)

                if (canSendToActiveGroupMembers(participants.size)) {
                    encryptAndEnqueue(message, participants)
                } else {
                    storeFailedLocalMessage(message)
                }
            }
        }

    suspend fun retry(messageId: String): Result<Unit> =
        runCatching {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }

            val message = chatDao.findMessageById(messageId) ?: error("Message was not found")
            check(message.isMine) { "Only outgoing messages can be retried" }
            requireGroupConversation(message.conversationId)

            val failedRecipients =
                messageRecipientStateDao
                    .findByMessageId(messageId)
                    .filter { state -> state.deliveryStatus == MessageDeliveryStatus.FAILED.name }
            check(failedRecipients.isNotEmpty()) { "Only failed group messages can be retried" }

            failedRecipients.forEach { state ->
                retryRecipient(
                    messageId = messageId,
                    contactId = state.contactId,
                    packetId = requireNotNull(state.packetId) { "Recipient state has no packet ID" }
                )
            }
        }

    suspend fun sendReadReceipts(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            requireGroupConversation(groupId)

            chatDao.findMessagesAwaitingReadReceipt(groupId).forEach { message ->
                enqueueReadReceipt(message.messageId, message.contactId)
                check(chatDao.markReadReceiptSent(message.messageId) == 1) {
                    "Incoming group message could not be marked as read"
                }
                logger.debug {
                    "Group read receipt queued: messageId=${message.messageId}, contactId=${message.contactId}"
                }
            }
        }

    private suspend fun requireActiveMembership(
        groupId: String,
        invitations: List<GroupInvitationEntity>
    ) {
        requireGroupConversation(groupId)
        check(invitations.none { invitation -> invitation.status.isIncomingPendingStatus() }) {
            "Accept the group invitation before sending messages"
        }
        check(invitations.none { invitation -> invitation.status == GroupInvitationStatus.LEAVE_SENT.name }) {
            "Messages are disabled while the group is being left"
        }
        check(invitations.none { invitation -> invitation.status == GroupInvitationStatus.GROUP_DELETED.name }) {
            "This group conversation was deleted"
        }
        check(isStillMember(groupId, invitations)) {
            "You are no longer a member of this group"
        }
    }

    private suspend fun isStillMember(
        groupId: String,
        invitations: List<GroupInvitationEntity>
    ): Boolean {
        val hasCurrentInvitation =
            invitations.any { invitation ->
                invitation.status != GroupInvitationStatus.REMOVED.name &&
                    invitation.status != GroupInvitationStatus.GROUP_DELETED.name
            }
        if (hasCurrentInvitation) return true

        val wasRemoved =
            chatDao.hasMessageWithTransportMode(
                conversationId = groupId,
                transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_REMOVED_TRANSPORT_MODE
            )
        val leftGroup =
            chatDao.hasMessageWithTransportMode(
                conversationId = groupId,
                transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_LEFT_TRANSPORT_MODE
            )
        return !wasRemoved && !leftGroup
    }

    private suspend fun requireGroupConversation(groupId: String) {
        val conversation = chatDao.findConversationById(groupId)
            ?: error("Group conversation was not found")
        check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }
    }

    private fun createQueuedMessage(
        groupId: String,
        text: String
    ): MessageEntity =
        MessageEntity(
            id = IdGenerator.generate(prefix = "group-message"),
            conversationId = groupId,
            packetId = null,
            text = text,
            transportPayload = null,
            transportMode = GROUP_END_TO_END_ENCRYPTED_MODE,
            contentStatus = MessageContentStatus.READABLE.name,
            deliveryStatus = MessageDeliveryStatus.QUEUED.name,
            senderContactId = null,
            isMine = true,
            createdAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )

    private suspend fun findCurrentEpochParticipants(
        groupId: String
    ): List<ConversationParticipantEntity> =
        chatDao
            .findConversationParticipants(groupId)
            .filter { participant -> isCurrentMember(groupId, participant) }

    private suspend fun isCurrentMember(
        groupId: String,
        participant: ConversationParticipantEntity
    ): Boolean {
        val signingPublicKey =
            contactDao
                .findPublicIdentityByContactId(participant.contactId)
                ?.signingPublicKey
                ?: return false

        return groupSecurityManager
            .isRemoteMemberIdentityCurrent(
                groupId = groupId,
                contactId = participant.contactId,
                signingPublicKey = signingPublicKey
            ).getOrDefault(false)
    }

    private suspend fun storeFailedLocalMessage(message: MessageEntity) {
        chatDao.upsertMessage(message.copy(deliveryStatus = MessageDeliveryStatus.FAILED.name))
        chatDao.updateConversationTimestamp(
            conversationId = message.conversationId,
            timestamp = message.createdAtEpochMilliseconds
        )
    }

    private suspend fun encryptAndEnqueue(
        message: MessageEntity,
        participants: List<ConversationParticipantEntity>
    ) {
        check(participants.isNotEmpty()) { "Group has no active participants" }

        val packets = createPackets(message, participants)
        val recipientStates = packets.map { (participant, packet) -> packet.toRecipientState(participant) }

        chatDao.upsertOutgoingGroupMessage(
            message = message,
            recipientStates = recipientStates,
            timestamp = message.createdAtEpochMilliseconds
        )
        packets.forEach { (participant, packet) -> enqueue(participant.contactId, packet) }
    }

    private suspend fun createPackets(
        message: MessageEntity,
        participants: List<ConversationParticipantEntity>
    ): Map<ConversationParticipantEntity, GroupChatMessagePacket> {
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val securedMessage =
            groupSecurityManager
                .encryptMessage(
                    groupId = message.conversationId,
                    messageId = message.id,
                    sentAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                    plaintext = message.text,
                    localSigningKeyPair = localSigningKeyPair
                ).getOrThrow()

        return participants.associateWith { participant ->
            GroupChatMessagePacket(
                packetId = packetId(message.id, participant.contactId),
                groupId = message.conversationId,
                epoch = securedMessage.epoch,
                messageId = message.id,
                sentAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                nonce = securedMessage.nonce.copyOf(),
                ciphertext = securedMessage.ciphertext.copyOf(),
                senderSignature = securedMessage.senderSignature.copyOf()
            )
        }
    }

    private fun GroupChatMessagePacket.toRecipientState(
        participant: ConversationParticipantEntity
    ): MessageRecipientStateEntity =
        MessageRecipientStateEntity(
            messageId = messageId,
            contactId = participant.contactId,
            packetId = packetId,
            deliveryStatus = MessageDeliveryStatus.QUEUED.name,
            lastError = null,
            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )

    private suspend fun enqueue(
        contactId: String,
        packet: GroupChatMessagePacket
    ) {
        protocolOutbox.enqueue(contactId, packet).getOrElse { error ->
            deliveryCoordinator.applyPacketEvent(
                packetId = packet.packetId,
                event = MessageDeliveryEvent.SEND_FAILED,
                errorMessage = error.message
            )
            throw error
        }
    }

    private suspend fun retryRecipient(
        messageId: String,
        contactId: String,
        packetId: String
    ) {
        val outboxItem = protocolOutbox.findByPacketId(packetId).getOrThrow()
            ?: error("Linked outbox item was not found")
        val currentState = messageRecipientStateDao.findByPacketId(packetId)
            ?: error("Recipient delivery state was not found")
        val currentStatus = currentState.deliveryStatus.toGroupDeliveryStatus()

        check(
            GroupMessageDeliveryStateMachine.canTransition(
                current = currentStatus,
                event = MessageDeliveryEvent.RETRY_REQUESTED
            )
        ) {
            "Recipient delivery is not retryable"
        }

        protocolOutbox.retry(outboxItem.id).getOrThrow()
        deliveryCoordinator.applyRetryEvent(messageId, contactId)
    }

    private suspend fun enqueueReadReceipt(
        messageId: String,
        contactId: String
    ) {
        protocolOutbox.enqueue(
            contactId = contactId,
            packet =
                ReadReceiptPacket(
                    packetId = "read-receipt-$messageId",
                    messageId = messageId,
                    readAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
        ).getOrThrow()
    }

    private fun String.isIncomingPendingStatus(): Boolean =
        this == GroupInvitationStatus.AWAITING_ACCEPTANCE.name ||
            this == GroupInvitationStatus.JOIN_SENT.name ||
            this == GroupInvitationStatus.WAITING_FOR_ACTIVATION.name

    private fun packetId(
        messageId: String,
        contactId: String
    ): String = "group-message-$messageId-$contactId"

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
