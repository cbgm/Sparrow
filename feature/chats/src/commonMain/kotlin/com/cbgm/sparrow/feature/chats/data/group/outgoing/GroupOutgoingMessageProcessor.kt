package com.cbgm.sparrow.feature.chats.data.group.outgoing

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.data.mapper.toDto
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.core.messagepart.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.PollPolicy
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.core.messagepart.domain.model.Voice
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
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
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.protocol.message.GroupMessageContent
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.message.OperationMessage
import com.cbgm.sparrow.protocol.message.OperationMessageCodec
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.protocol.packet.ReadReceiptPacket
import com.cbgm.sparrow.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.protocol.profile.ProfilePictureMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
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
    private val groupMembershipRepository: GroupMembershipRepository,
    private val deliveryCoordinator: GroupMessageDeliveryCoordinator,
    private val localProfilePictureMetadataProvider: LocalProfilePictureMetadataProvider,
    private val groupMessageContentCodec: GroupMessageContentCodec,
    private val operationMessageCodec: OperationMessageCodec,
    private val attachmentTransfer: MessageAttachmentOperationsRepository
) {
    private val sendMutex = Mutex()
    private val logger = SparrowLog.withTag("GroupOutgoingMessageProcessor")

    suspend fun send(
        groupId: String,
        parts: List<MessagePart>,
        replyToMessageId: String? = null,
        access: GroupMessageMembershipAccess
    ): Result<Unit> =
        safeSuspendCall {
            sendMutex.withLock {
                val normalizedParts = requireMessageContent(parts)
                val text = normalizedParts.filterIsInstance<Text>().singleOrNull()?.text.orEmpty()
                val attachmentParts = normalizedParts.filterNot { part -> part is Text }
                requireActiveMembership(groupId, access)
                val recipients = findCurrentRecipients(groupId)
                if (recipients.isNotEmpty()) {
                    flushQueuedLocked(groupId, recipients)
                }

                val message = createQueuedMessage(groupId, replyToMessageId)
                val messageParts = persistMessage(message, text, attachmentParts)
                if (recipients.isNotEmpty()) {
                    encryptAndEnqueue(message, recipients, messageParts)
                }
            }
        }

    /** Release owner messages only after membership projection grants a recipient.
     * The existing database queue survives process termination and is replayed
     * on ACTIVE membership-result reconciliation after an app restart.
     */
    suspend fun flushQueued(groupId: String): Result<Unit> = safeSuspendCall {
        sendMutex.withLock {
            val recipients = findCurrentRecipients(groupId)
            if (recipients.isNotEmpty()) flushQueuedLocked(groupId, recipients)
        }
    }

    suspend fun findGroupIdForMessage(messageId: String): Result<String?> = safeSuspendCall {
        val message = messageDataSource.findMessage(messageId) ?: return@safeSuspendCall null
        val conversation = messageDataSource.findConversation(message.conversationId)
            ?: return@safeSuspendCall null
        conversation.id.takeIf { conversation.type == GROUP_CONVERSATION_TYPE }
    }

    private suspend fun flushQueuedLocked(groupId: String, recipients: List<String>) {
        val waiting = messageDataSource.findQueuedGroupMessages(groupId)
        val interrupted = messageDataSource.findGroupMessagesAwaitingOutbox(groupId)
        (waiting + interrupted).distinctBy { it.id }
            .sortedWith(compareBy<MessageEntity> { it.createdAtEpochMilliseconds }.thenBy { it.id })
            .forEach { message ->
                try {
                    val previousStates = messageDataSource.findRecipientStates(message.id)
                    val missingRecipients = if (previousStates.isEmpty()) {
                        recipients
                    } else {
                        // Reconcile ONLY original recipients; never backfill a
                        // message to someone who joined at a later epoch.
                        previousStates.filter { state ->
                            val packetId = state.packetId

                            state.contactId in recipients &&
                                state.deliveryStatus == MessageDeliveryStatus.QUEUED.name &&
                                packetId != null &&
                                protocolOutbox.findByPacketId(packetId).getOrThrow() == null
                        }.map { it.contactId }
                    }
                    if (missingRecipients.isEmpty()) return@forEach
                    val packets = createPackets(
                        message = message,
                        recipients = missingRecipients,
                        parts = attachmentTransfer.prepareOutgoing(message.id).getOrThrow()
                    )
                    if (previousStates.isEmpty()) {
                        val states = packets.map { (contactId, packet) ->
                            packet.toMessageRecipientStateEntity(contactId)
                        }
                        // Persist recipient mapping before enqueuing. Restart
                        // reconciliation handles a crash between these writes.
                        messageDataSource.saveOutgoingMessage(
                            message = message,
                            text = messageDataSource.findMessageText(message.id).orEmpty(),
                            recipientStates = states,
                            timestamp = message.createdAtEpochMilliseconds
                        )
                    }
                    enqueuePackets(packets)
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    // A damaged/temporarily undeliverable old message must not
                    // prevent active members from sending subsequent messages.
                    logger.warn(error) {
                        "Group message remains queued for retry: messageId=${message.id}"
                    }
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

            sendOperation(
                groupId = groupId,
                operation = MessageOperation.Reaction(messageId = messageId, emoji = emoji, removed = removed),
                recipients = recipients
            )
        }

    suspend fun votePoll(
        groupId: String,
        messageId: String,
        pollId: String,
        selectedOptionIds: Set<String>,
        access: GroupMessageMembershipAccess
    ): Result<Unit> =
        safeSuspendCall {
            requireActiveMembership(groupId, access)
            val target = requireTargetMessage(groupId, messageId)
            check(target.transportMode == GROUP_END_TO_END_ENCRYPTED_MODE) { "Poll target is not a user message" }
            val recipients = findCurrentRecipients(groupId)
            check(recipients.isNotEmpty()) { "Group has no active recipients" }

            val poll = requirePoll(messageId, pollId)
            val updated =
                PollPolicy.vote(
                    poll = poll,
                    voterId = PollPolicy.LOCAL_VOTER_ID,
                    selectedOptionIds = selectedOptionIds,
                    nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
            attachmentTransfer.updateMessagePart(messageId, updated).getOrThrow()
            sendOperation(
                groupId = groupId,
                operation =
                    MessageOperation.PollVote(
                        messageId = messageId,
                        pollId = pollId,
                        selectedOptionIds = selectedOptionIds
                    ),
                recipients = recipients
            )
        }

    suspend fun closePoll(
        groupId: String,
        messageId: String,
        pollId: String,
        closedAtEpochMilliseconds: Long,
        access: GroupMessageMembershipAccess
    ): Result<Unit> =
        safeSuspendCall {
            requireActiveMembership(groupId, access)
            val target = requireTargetMessage(groupId, messageId)
            check(target.transportMode == GROUP_END_TO_END_ENCRYPTED_MODE) { "Poll target is not a user message" }
            val isLocalAdmin = groupMembershipRepository.observeAdministration(groupId).first().isLocalAdmin
            check(target.isMine || isLocalAdmin) { "Only the poll creator or a group admin can close the poll" }
            val recipients = findCurrentRecipients(groupId)
            check(recipients.isNotEmpty()) { "Group has no active recipients" }

            val updated = PollPolicy.close(requirePoll(messageId, pollId), closedAtEpochMilliseconds)
            attachmentTransfer.updateMessagePart(messageId, updated).getOrThrow()
            sendOperation(
                groupId = groupId,
                operation =
                    MessageOperation.PollClose(
                        messageId = messageId,
                        pollId = pollId,
                        closedAtEpochMilliseconds = closedAtEpochMilliseconds
                    ),
                recipients = recipients
            )
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

            sendOperation(
                groupId = groupId,
                operation =
                    MessageOperation.Delete(
                        messageId = messageId,
                        deletedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    ),
                recipients = recipients
            )

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
            check(!messageDataSource.findMessageText(messageId).isNullOrBlank()) { "Only text messages can be edited" }
            check(attachmentTransfer.messageParts(messageId).getOrThrow().isEmpty()) {
                "Messages with attachments cannot be edited"
            }
            check(
                messageDataSource
                    .findRecipientStates(messageId)
                    .none { state -> state.deliveryStatus == MessageDeliveryStatus.READ.name }
            ) { "Read messages cannot be edited" }

            val recipients = findCurrentRecipients(groupId)
            check(recipients.isNotEmpty()) { "Group has no active recipients" }

            sendOperation(
                groupId = groupId,
                operation =
                    MessageOperation.Edit(
                        messageId = messageId,
                        text = normalizedText,
                        editedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    ),
                recipients = recipients
            )

            messageDataSource.replaceMessageText(messageId, normalizedText)
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
        replyToMessageId: String?
    ): MessageEntity =
        MessageEntity(
            id = IdGenerator.generate(prefix = "group-message"),
            conversationId = groupId,
            packetId = null,
            replyToMessageId = replyToMessageId,
            transportPayload = null,
            transportMode = GROUP_END_TO_END_ENCRYPTED_MODE,
            contentStatus = MessageContentStatus.READABLE.name,
            deliveryStatus = MessageDeliveryStatus.QUEUED.name,
            senderContactId = null,
            isMine = true,
            createdAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )

    private suspend fun requireTargetMessage(groupId: String, messageId: String): MessageEntity {
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        val target = messageDataSource.findMessage(messageId) ?: error("Message was not found")
        check(target.conversationId == groupId) { "Message does not belong to this group" }
        return target
    }

    private suspend fun requirePoll(messageId: String, pollId: String): Poll {
        require(pollId.isNotBlank()) { "Poll ID must not be blank" }
        return attachmentTransfer
            .messageParts(messageId)
            .getOrThrow()
            .filterIsInstance<Poll>()
            .singleOrNull { poll -> poll.id == pollId }
            ?: error("Poll was not found")
    }

    private suspend fun sendOperation(
        groupId: String,
        operation: MessageOperation,
        recipients: List<String>
    ) {
        val eventId = IdGenerator.generate(prefix = "group-operation")
        val timestamp = SystemClock.nowEpochMilliseconds()
        val profilePicture = localProfilePictureMetadataProvider.forMessage().getOrElse { ProfilePictureMetadata() }
        val plaintext = operationMessageCodec.encode(OperationMessage(operation))
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val secured =
            groupSecurityManager.encryptMessage(
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
                    packetId = "group-operation-$eventId-$contactId",
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

    private suspend fun findCurrentRecipients(groupId: String): List<String> =
        messageDataSource
            .findConversationParticipants(groupId)
            .map { participant -> participant.contactId }
            .distinct()

    private suspend fun encryptAndEnqueue(
        message: MessageEntity,
        recipients: List<String>,
        parts: List<MessagePart>
    ) {
        val packets =
            try {
                createPackets(message, recipients, parts)
            } catch (error: Throwable) {
                attachmentTransfer.deleteForMessages(listOf(message.id))
                messageDataSource.deleteMessages(listOf(message))
                throw error
            }
        val recipientStates = packets.map { (contactId, packet) -> packet.toMessageRecipientStateEntity(contactId) }
        messageDataSource.saveRecipientStates(recipientStates)
        enqueuePackets(packets)
    }

    private suspend fun persistMessage(
        message: MessageEntity,
        text: String,
        parts: List<MessagePart>
    ): List<MessagePart> {
        messageDataSource.saveOutgoingMessage(
            message = message,
            text = text,
            recipientStates = emptyList(),
            timestamp = message.createdAtEpochMilliseconds
        )
        try {
            val conversation = messageDataSource.findConversation(message.conversationId)
                ?: error("Group conversation was not found")
            return attachmentTransfer.persistOutgoing(
                messageId = message.id,
                parts = parts,
                context = AttachmentMessageContext(
                    conversationId = message.conversationId,
                    createdAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                    displayName = conversation.title?.takeIf(String::isNotBlank) ?: conversation.id,
                    isGroup = true,
                    isMine = true,
                    senderContactId = null
                )
            ).getOrThrow()
        } catch (error: Throwable) {
            messageDataSource.deleteMessages(listOf(message))
            throw error
        }
    }

    private suspend fun enqueuePackets(packets: Map<String, GroupChatMessagePacket>) {
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
        parts: List<MessagePart>
    ): Map<String, GroupChatMessagePacket> {
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val profilePicture =
            localProfilePictureMetadataProvider.forMessage().getOrElse { ProfilePictureMetadata() }
        val plaintext =
            groupMessageContentCodec.encode(
                GroupMessageContent(
                    parts =
                        buildList {
                            messageDataSource.findMessageText(message.id)
                                ?.takeIf(String::isNotBlank)
                                ?.let { text -> add(TextDto(id = message.id, text = text)) }
                            addAll(parts.map { it.toDto() })
                        },
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

    private fun requireMessageContent(parts: List<MessagePart>): List<MessagePart> {
        require(parts.isNotEmpty()) { "Message must contain message parts" }
        require(parts.map(MessagePart::id).distinct().size == parts.size) {
            "Message part IDs must be unique"
        }

        val textParts = parts.filterIsInstance<Text>()
        require(textParts.size <= 1) { "A message can contain at most one text part" }
        val normalizedText = textParts.singleOrNull()?.text?.trim().orEmpty()
        require(textParts.isEmpty() || normalizedText.isNotEmpty()) {
            "Message text must not be blank"
        }

        val attachments = parts.filterNot { part -> part is Text }
        require(attachments.none { part -> part is Poll } || normalizedText.isEmpty()) {
            "A poll must be sent without a separate text part"
        }
        MessageAttachmentPolicy.requireValid(attachments)
        require(attachments.none { it is Voice } || normalizedText.isEmpty()) {
            "A voice message cannot contain text"
        }

        return buildList {
            textParts.singleOrNull()?.let { textPart ->
                add(textPart.copy(text = normalizedText))
            }
            addAll(attachments)
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
