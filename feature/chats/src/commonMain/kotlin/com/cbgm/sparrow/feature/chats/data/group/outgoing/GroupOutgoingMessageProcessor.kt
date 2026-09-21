package com.cbgm.sparrow.feature.chats.data.group.outgoing

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.message.GroupMessageContent
import com.cbgm.sparrow.core.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.core.protocol.message.MessageDeletionPayload
import com.cbgm.sparrow.core.protocol.message.MessageDeletionPayloadCodec
import com.cbgm.sparrow.core.protocol.message.MessageEditPayload
import com.cbgm.sparrow.core.protocol.message.MessageEditPayloadCodec
import com.cbgm.sparrow.core.protocol.message.MessageReactionPayload
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.GroupMessageDeletionPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMessageEditPacket
import com.cbgm.sparrow.core.protocol.packet.ReadReceiptPacket
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.PreparedMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupOutgoingMessageDataSource
import com.cbgm.sparrow.feature.chats.data.group.delivery.GroupMessageDeliveryCoordinator
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.mapper.toMessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.membership.domain.model.GroupMessageMembershipAccess
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns every outgoing group-message operation.
 *
 * Red line:
 * Group use case -> GroupMessageRepositoryImpl -> this processor -> ProtocolOutbox.
 */
class GroupOutgoingMessageProcessor(
    private val messageDataSource: GroupOutgoingMessageDataSource,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val groupSecurityManager: GroupSecurityRepository,
    private val deliveryCoordinator: GroupMessageDeliveryCoordinator,
    private val localProfilePictureMetadataProvider: LocalProfilePictureMetadataProvider,
    private val groupMessageContentCodec: GroupMessageContentCodec,
    private val messageDeletionPayloadCodec: MessageDeletionPayloadCodec,
    private val messageEditPayloadCodec: MessageEditPayloadCodec,
    private val attachmentTransfer: MessageAttachmentOperationsRepository
) {
    private val sendMutex = Mutex()
    private val logger = SparrowLog.withTag("GroupOutgoingMessageProcessor")

    suspend fun send(
        groupId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment> = emptyList(),
        replyToMessageId: String? = null,
        access: GroupMessageMembershipAccess
    ): Result<Unit> =
        safeSuspendCall {
            sendMutex.withLock {
                val normalizedText = requireMessageContent(text, attachments)
                requireActiveMembership(groupId, access)
                val recipients = findCurrentRecipients(groupId)
                check(recipients.isNotEmpty()) { "Group has no active recipients" }

                val message = createQueuedMessage(groupId, normalizedText, replyToMessageId)
                val prepared = attachmentTransfer.prepareAttachments(attachments)
                try {
                    encryptAndEnqueue(message, recipients, prepared)
                } catch (error: Throwable) {
                    val stored = messageDataSource.findMessage(message.id) != null
                    if (!stored) {
                        attachmentTransfer.cleanupPrepared(prepared)
                    }
                    throw error
                }
            }
        }

    suspend fun toggleReaction(
        groupId: String,
        messageId: String,
        emoji: String,
        access: GroupMessageMembershipAccess
    ): Result<Unit> =
        safeSuspendCall {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }
            require(emoji.isNotBlank()) { "Reaction emoji must not be blank" }
            requireActiveMembership(groupId, access)
            val target = messageDataSource.findMessage(messageId) ?: error("Message was not found")
            check(target.conversationId == groupId) { "Message does not belong to this group" }
            val recipients = findCurrentRecipients(groupId)
            check(recipients.isNotEmpty()) { "Group has no active recipients" }

            val existing = messageDataSource.findReaction(messageId, MessageReactionEntity.LOCAL_REACTOR_ID, emoji)
            val removed = existing != null
            if (removed) {
                messageDataSource.deleteReaction(messageId, MessageReactionEntity.LOCAL_REACTOR_ID, emoji)
            } else {
                messageDataSource.saveReaction(
                    MessageReactionEntity(messageId, groupId, MessageReactionEntity.LOCAL_REACTOR_ID, emoji)
                )
            }

            val eventId = IdGenerator.generate(prefix = "group-reaction")
            val timestamp = SystemClock.nowEpochMilliseconds()
            val profilePicture = localProfilePictureMetadataProvider.forMessage().getOrElse { ProfilePictureMetadata() }
            val plaintext = groupMessageContentCodec.encode(
                GroupMessageContent(reaction = MessageReactionPayload(messageId, emoji, removed))
            )
            val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val secured = groupSecurityManager.encryptMessage(
                groupId = groupId,
                messageId = eventId,
                sentAtEpochMilliseconds = timestamp,
                plaintext = plaintext,
                localSigningKeyPair = localSigningKeyPair,
                profilePicture = profilePicture
            ).getOrThrow()
            recipients.forEach { contactId ->
                protocolOutbox.enqueue(
                    contactId,
                    GroupChatMessagePacket(
                        packetId = "group-reaction-$eventId-$contactId",
                        groupId = groupId,
                        epoch = secured.epoch,
                        messageId = eventId,
                        sentAtEpochMilliseconds = timestamp,
                        profilePicture = profilePicture,
                        nonce = secured.nonce.copyOf(),
                        ciphertext = secured.ciphertext.copyOf(),
                        senderSignature = secured.senderSignature.copyOf()
                    )
                ).getOrThrow()
            }
        }

    suspend fun deleteMessage(
        groupId: String,
        messageId: String,
        access: GroupMessageMembershipAccess
    ): Result<Unit> =
        safeSuspendCall {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }
            requireActiveMembership(groupId, access)
            val target = messageDataSource.findMessage(messageId) ?: error("Message was not found")
            check(target.conversationId == groupId) { "Message does not belong to this group" }
            check(target.transportMode == GROUP_END_TO_END_ENCRYPTED_MODE) { "Only user messages can be deleted" }
            check(target.isMine) { "Only your own messages can be deleted for everyone" }
            val recipients = findCurrentRecipients(groupId)
            check(recipients.isNotEmpty()) { "Group has no active recipients" }

            val eventId = IdGenerator.generate(prefix = "group-delete")
            val timestamp = SystemClock.nowEpochMilliseconds()
            val plaintext =
                messageDeletionPayloadCodec.encode(
                    MessageDeletionPayload(messageId)
                )
            val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val secured =
                groupSecurityManager.encryptMessageDeletion(
                    groupId = groupId,
                    deletionId = eventId,
                    deletedAtEpochMilliseconds = timestamp,
                    plaintext = plaintext,
                    localSigningKeyPair = localSigningKeyPair
                ).getOrThrow()

            recipients.forEach { contactId ->
                protocolOutbox.enqueue(
                    contactId,
                    GroupMessageDeletionPacket(
                        packetId = "group-delete-$eventId-$contactId",
                        groupId = groupId,
                        epoch = secured.epoch,
                        deletionId = eventId,
                        deletedAtEpochMilliseconds = timestamp,
                        nonce = secured.nonce.copyOf(),
                        ciphertext = secured.ciphertext.copyOf(),
                        senderSignature = secured.senderSignature.copyOf()
                    )
                ).getOrThrow()
            }

            attachmentTransfer.deleteForMessages(listOf(messageId))
            messageDataSource.deleteMessages(listOf(target))
        }

    suspend fun editMessage(
        groupId: String,
        messageId: String,
        text: String,
        access: GroupMessageMembershipAccess
    ): Result<Unit> =
        safeSuspendCall {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }
            val normalizedText = text.trim()
            require(normalizedText.isNotBlank()) { "Edited message text must not be blank" }
            requireActiveMembership(groupId, access)

            val target = messageDataSource.findMessage(messageId) ?: error("Message was not found")
            check(target.conversationId == groupId) { "Message does not belong to this group" }
            check(target.transportMode == GROUP_END_TO_END_ENCRYPTED_MODE) { "Only user messages can be edited" }
            check(target.isMine) { "Only your own messages can be edited" }
            check(target.text.isNotBlank()) { "Only text messages can be edited" }
            check(attachmentTransfer.protocolAttachments(messageId).isEmpty()) {
                "Messages with attachments cannot be edited"
            }
            check(
                messageDataSource
                    .findRecipientStates(messageId)
                    .none { state -> state.deliveryStatus == MessageDeliveryStatus.READ.name }
            ) { "Read messages cannot be edited" }

            val recipients = findCurrentRecipients(groupId)
            check(recipients.isNotEmpty()) { "Group has no active recipients" }

            val editId = IdGenerator.generate(prefix = "group-edit")
            val timestamp = SystemClock.nowEpochMilliseconds()
            val plaintext = messageEditPayloadCodec.encode(MessageEditPayload(messageId, normalizedText))
            val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val secured =
                groupSecurityManager
                    .encryptMessageEdit(
                        groupId = groupId,
                        editId = editId,
                        editedAtEpochMilliseconds = timestamp,
                        plaintext = plaintext,
                        localSigningKeyPair = localSigningKeyPair
                    ).getOrThrow()

            recipients.forEach { contactId ->
                protocolOutbox.enqueue(
                    contactId,
                    GroupMessageEditPacket(
                        packetId = "group-edit-$editId-$contactId",
                        groupId = groupId,
                        epoch = secured.epoch,
                        editId = editId,
                        editedAtEpochMilliseconds = timestamp,
                        nonce = secured.nonce.copyOf(),
                        ciphertext = secured.ciphertext.copyOf(),
                        senderSignature = secured.senderSignature.copyOf()
                    )
                ).getOrThrow()
            }

            messageDataSource.saveMessage(target.copy(text = normalizedText))
        }

    suspend fun retry(messageId: String): Result<Unit> =
        safeSuspendCall {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }

            val message = messageDataSource.findMessage(messageId) ?: error("Message was not found")
            check(message.isMine) { "Only outgoing messages can be retried" }
            requireGroupConversation(message.conversationId)

            val failedRecipients =
                messageDataSource
                    .findRecipientStates(messageId)
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
        safeSuspendCall {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            requireGroupConversation(groupId)

            val failures = mutableListOf<String>()
            messageDataSource.findAwaitingReadReceipt(groupId).forEach { message ->
                val enqueueError = enqueueReadReceipt(message.messageId, message.contactId).exceptionOrNull()
                if (enqueueError != null) {
                    failures += message.contactId.toFailureDescription(enqueueError)
                    return@forEach
                }

                val markedRead = runCatching { messageDataSource.markReadReceiptSent(message.messageId) }
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
        access: GroupMessageMembershipAccess
    ) {
        requireGroupConversation(groupId)
        check(!access.isJoinPending) {
            "Complete the group join before sending messages"
        }
        check(!access.isLeavePending) {
            "Messages are disabled while the group is being left"
        }
        check(!access.isDeleted) {
            "This group conversation was deleted"
        }
        check(isStillMember(groupId, access)) {
            "You are no longer a member of this group"
        }
    }

    private suspend fun isStillMember(
        groupId: String,
        access: GroupMessageMembershipAccess
    ): Boolean {
        if (access.hasCurrentMembership) return true

        val wasRemoved =
            messageDataSource.hasMessageWithTransportMode(
                conversationId = groupId,
                transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_REMOVED_TRANSPORT_MODE
            )
        val leftGroup =
            messageDataSource.hasMessageWithTransportMode(
                conversationId = groupId,
                transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_LEFT_TRANSPORT_MODE
            )
        return !wasRemoved && !leftGroup
    }

    private suspend fun requireGroupConversation(groupId: String) {
        val conversation = messageDataSource.findConversation(groupId)
            ?: error("Group conversation was not found")
        check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }
    }

    private fun createQueuedMessage(
        groupId: String,
        text: String,
        replyToMessageId: String?
    ): MessageEntity =
        MessageEntity(
            id = IdGenerator.generate(prefix = "group-message"),
            conversationId = groupId,
            packetId = null,
            text = text,
            replyToMessageId = replyToMessageId,
            transportPayload = null,
            transportMode = GROUP_END_TO_END_ENCRYPTED_MODE,
            contentStatus = MessageContentStatus.READABLE.name,
            deliveryStatus = MessageDeliveryStatus.QUEUED.name,
            senderContactId = null,
            isMine = true,
            createdAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )

    private suspend fun findCurrentRecipients(groupId: String): List<String> =
        messageDataSource
            .findConversationParticipants(groupId)
            .map { participant -> participant.contactId }
            .distinct()

    private suspend fun encryptAndEnqueue(
        message: MessageEntity,
        recipients: List<String>,
        prepared: List<PreparedMessageAttachment>
    ) {
        val packets = createPackets(message, recipients, prepared)
        val recipientStates = packets.map { (contactId, packet) -> packet.toMessageRecipientStateEntity(contactId) }

        messageDataSource.saveOutgoingMessage(
            message = message,
            recipientStates = recipientStates,
            timestamp = message.createdAtEpochMilliseconds
        )
        try {
            val conversation = messageDataSource.findConversation(message.conversationId)
                ?: error("Group conversation was not found")
            attachmentTransfer.persistOutgoing(
                messageId = message.id,
                prepared = prepared,
                context = AttachmentMessageContext(
                    conversationId = message.conversationId,
                    createdAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                    displayName = conversation.title?.takeIf(String::isNotBlank) ?: conversation.id,
                    isGroup = true,
                    isMine = true,
                    senderContactId = null
                )
            )
        } catch (error: Throwable) {
            messageDataSource.deleteMessages(listOf(message))
            attachmentTransfer.cleanupPrepared(prepared)
            throw error
        }

        val failures = mutableListOf<String>()
        packets.forEach { (contactId, packet) ->
            val error = protocolOutbox.enqueue(contactId, packet).exceptionOrNull()
            if (error != null) {
                runCatching {
                    deliveryCoordinator.applyPacketEvent(
                        packetId = packet.packetId,
                        event = MessageDeliveryEvent.SEND_FAILED,
                        errorMessage = error.message
                    )
                }.onFailure { stateError ->
                    logger.error(stateError) {
                        "Could not persist failed group recipient state: packetId=${packet.packetId}"
                    }
                }
                failures += contactId.toFailureDescription(error)
            }
        }
        failures.throwIfNotEmpty("Group message enqueue")
    }

    private suspend fun createPackets(
        message: MessageEntity,
        recipients: List<String>,
        prepared: List<PreparedMessageAttachment>
    ): Map<String, GroupChatMessagePacket> {
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val profilePicture =
            localProfilePictureMetadataProvider.forMessage().getOrElse { ProfilePictureMetadata() }
        val plaintext =
            groupMessageContentCodec.encode(
                GroupMessageContent(
                    text = message.text,
                    attachments = prepared.map(PreparedMessageAttachment::attachment),
                    replyToMessageId = message.replyToMessageId
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

        return recipients.associateWith { contactId ->
            GroupChatMessagePacket(
                packetId = packetId(message.id, contactId),
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

    private fun GroupChatMessagePacket.toMessageRecipientStateEntity(
        contactId: String
    ): MessageRecipientStateEntity =
        MessageRecipientStateEntity(
            messageId = messageId,
            contactId = contactId,
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
        val currentState = messageDataSource.findRecipientByPacketId(packetId)
            ?: error("Recipient delivery state was not found")
        val currentStatus = currentState.deliveryStatus.toMessageDeliveryStatus()

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
            ).map { }

    private fun requireMessageContent(
        text: String,
        attachments: List<OutgoingMessageAttachment>
    ): String {
        MessageAttachmentPolicy.requireValid(attachments)
        return text.trim().also { normalizedText ->
            require(normalizedText.isNotEmpty() || attachments.isNotEmpty()) {
                "Message must contain text or attachments"
            }
            require(attachments.none { it.type == MessageAttachmentType.VOICE } || normalizedText.isEmpty()) {
                "A voice message cannot contain text"
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

    private fun packetId(
        messageId: String,
        contactId: String
    ): String = "group-message-$messageId-$contactId"

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
