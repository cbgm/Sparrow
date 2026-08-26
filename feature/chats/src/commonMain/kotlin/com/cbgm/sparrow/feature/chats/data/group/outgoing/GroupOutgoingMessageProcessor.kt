package com.cbgm.sparrow.feature.chats.data.group.outgoing

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.message.GroupMessageContent
import com.cbgm.sparrow.core.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.ReadReceiptPacket
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.dao.MessageRecipientStateDao
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.model.PreparedMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.data.group.delivery.GroupMessageDeliveryCoordinator
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.mapper.toGroupDeliveryStatus
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessageDeliveryStateMachine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns every outgoing group-message operation.
 *
 * Red line:
 * Group use case -> GroupMessageRepositoryImpl -> this processor -> ProtocolOutbox.
 */
class GroupOutgoingMessageProcessor(
    private val chatDao: ChatDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val messageRecipientStateDao: MessageRecipientStateDao,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val groupSecurityManager: GroupSecurityManager,
    private val deliveryCoordinator: GroupMessageDeliveryCoordinator,
    private val localProfilePictureMetadataProvider: LocalProfilePictureMetadataProvider,
    private val groupMessageContentCodec: GroupMessageContentCodec,
    private val attachmentTransfer: MessageAttachmentDataSource
) {
    private val sendMutex = Mutex()
    private val logger = SparrowLog.withTag("GroupOutgoingMessageProcessor")

    suspend fun send(
        groupId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment> = emptyList(),
        invitations: List<GroupInvitationEntity>
    ): Result<Unit> =
        runCatching {
            sendMutex.withLock {
                val normalizedText = requireMessageContent(text, attachments)
                requireActiveMembership(groupId, invitations)
                val recipients = findCurrentEpochRecipients(groupId)
                check(recipients.isNotEmpty()) { "Group has no active recipients" }

                val message = createQueuedMessage(groupId, normalizedText)
                val prepared = attachmentTransfer.prepareAttachments(attachments).getOrThrow()
                try {
                    encryptAndEnqueue(message, recipients, prepared)
                } catch (error: Throwable) {
                    val stored = chatDao.findMessageById(message.id) != null
                    if (!stored) {
                        attachmentTransfer.cleanupPrepared(prepared)
                    }
                    throw error
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

            val failures = mutableListOf<String>()
            failedRecipients.forEach { state ->
                runCatching {
                    retryRecipient(
                        messageId = messageId,
                        contactId = state.contactId,
                        packetId = requireNotNull(state.packetId) { "Recipient state has no packet ID" }
                    )
                }.onFailure { error ->
                    failures += state.contactId.toFailureDescription(error)
                }
            }
            failures.throwIfNotEmpty("Group message retry")
        }

    suspend fun sendReadReceipts(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            requireGroupConversation(groupId)

            val failures = mutableListOf<String>()
            chatDao.findMessagesAwaitingReadReceipt(groupId).forEach { message ->
                val enqueueError = enqueueReadReceipt(message.messageId, message.contactId).exceptionOrNull()
                if (enqueueError != null) {
                    failures += message.contactId.toFailureDescription(enqueueError)
                    return@forEach
                }

                val markedRead = runCatching { chatDao.markReadReceiptSent(message.messageId) }
                val markError = markedRead.exceptionOrNull()
                when {
                    markError != null -> failures += message.contactId.toFailureDescription(markError)
                    markedRead.getOrNull() != 1 ->
                        failures += "${message.contactId}: incoming group message could not be marked as read"
                    else ->
                        logger.debug {
                            "Group read receipt queued: messageId=${message.messageId}, contactId=${message.contactId}"
                        }
                }
            }
            failures.throwIfNotEmpty("Group read receipt")
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

    private suspend fun findCurrentEpochRecipients(groupId: String): List<GroupMemberKeyEntity> {
        val state = groupSecurityDao.findState(groupId) ?: error("Group security state was not found")
        return groupSecurityDao.findMemberKeys(
            groupId = groupId,
            epoch = state.currentEpoch
        )
    }

    private suspend fun encryptAndEnqueue(
        message: MessageEntity,
        recipients: List<GroupMemberKeyEntity>,
        prepared: List<PreparedMessageAttachment>
    ) {
        val packets = createPackets(message, recipients, prepared)
        val recipientStates = packets.map { (recipient, packet) -> packet.toRecipientState(recipient) }

        chatDao.upsertOutgoingGroupMessage(
            message = message,
            recipientStates = recipientStates,
            timestamp = message.createdAtEpochMilliseconds
        )
        try {
            attachmentTransfer.persistOutgoing(message.id, prepared)
        } catch (error: Throwable) {
            chatDao.deleteMessagesAndRefreshConversations(listOf(message))
            attachmentTransfer.cleanupPrepared(prepared)
            throw error
        }

        val failures = mutableListOf<String>()
        packets.forEach { (recipient, packet) ->
            val error = protocolOutbox.enqueue(recipient.contactId, packet).exceptionOrNull()
            if (error != null) {
                runCatching {
                    deliveryCoordinator.applyPacketEvent(
                        packetId = packet.packetId,
                        event = MessageDeliveryEvent.SEND_FAILED,
                        errorMessage = error.message
                    )
                }.onFailure { stateError ->
                    logger.warn(stateError) {
                        "Could not persist failed group recipient state: packetId=${packet.packetId}"
                    }
                }
                failures += recipient.contactId.toFailureDescription(error)
            }
        }
        failures.throwIfNotEmpty("Group message enqueue")
    }

    private suspend fun createPackets(
        message: MessageEntity,
        recipients: List<GroupMemberKeyEntity>,
        prepared: List<PreparedMessageAttachment>
    ): Map<GroupMemberKeyEntity, GroupChatMessagePacket> {
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val profilePicture =
            localProfilePictureMetadataProvider.forMessage().getOrElse { ProfilePictureMetadata() }
        val plaintext =
            groupMessageContentCodec.encode(
                GroupMessageContent(
                    text = message.text,
                    attachments = prepared.map(PreparedMessageAttachment::attachment)
                )
            )
        val securedMessage =
            groupSecurityManager
                .encryptMessage(
                    groupId = message.conversationId,
                    messageId = message.id,
                    sentAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                    plaintext = plaintext,
                    localSigningKeyPair = localSigningKeyPair,
                    profilePicture = profilePicture
                ).getOrThrow()

        return recipients.associateWith { recipient ->
            GroupChatMessagePacket(
                packetId = packetId(message.id, recipient.contactId),
                groupId = message.conversationId,
                epoch = securedMessage.epoch,
                messageId = message.id,
                sentAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                profilePicture = profilePicture,
                nonce = securedMessage.nonce.copyOf(),
                ciphertext = securedMessage.ciphertext.copyOf(),
                senderSignature = securedMessage.senderSignature.copyOf()
            )
        }
    }

    private fun GroupChatMessagePacket.toRecipientState(
        recipient: GroupMemberKeyEntity
    ): MessageRecipientStateEntity =
        MessageRecipientStateEntity(
            messageId = messageId,
            contactId = recipient.contactId,
            packetId = packetId,
            deliveryStatus = MessageDeliveryStatus.QUEUED.name,
            lastError = null,
            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )

    private suspend fun retryRecipient(
        messageId: String,
        contactId: String,
        packetId: String
    ) {
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

        protocolOutbox.resend(packetId).getOrThrow()
        deliveryCoordinator.applyRetryEvent(messageId, contactId)
    }

    private suspend fun enqueueReadReceipt(
        messageId: String,
        contactId: String
    ): Result<Unit> =
        protocolOutbox
            .enqueue(
                contactId = contactId,
                packet =
                    ReadReceiptPacket(
                        packetId = "read-receipt-$messageId",
                        messageId = messageId,
                        readAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
            ).map { Unit }

    private fun requireMessageContent(
        text: String,
        attachments: List<OutgoingMessageAttachment>
    ): String {
        MessageAttachmentPolicy.requireValid(attachments)
        return text.trim().also { normalizedText ->
            require(normalizedText.isNotEmpty() || attachments.isNotEmpty()) {
                "Message must contain text or attachments"
            }
        }
    }

    private fun String.toFailureDescription(error: Throwable): String =
        "$this: ${error.message ?: error::class.simpleName.orEmpty()}"

    private fun List<String>.throwIfNotEmpty(operation: String) {
        check(isEmpty()) {
            "$operation failed for ${joinToString()}"
        }
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
