package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.operation

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ParkedMessageOperation
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.repository.ParkedMessageOperationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.ConversationInfo
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupRecipientState
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.MessageReactionRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupConversationMembershipSnapshot
import com.cbgm.sparrow.feature.membership.domain.model.GroupIncomingWelcomeAuthorization
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberPromotionResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.GroupMessageMembershipAccess
import com.cbgm.sparrow.feature.membership.domain.model.GroupMetadataMessageSender
import com.cbgm.sparrow.feature.membership.domain.model.GroupMetadataSendContext
import com.cbgm.sparrow.feature.membership.domain.model.GroupTransportRoutingMember
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.MembershipVerificationSnapshot
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.protocol.packet.GroupReadyAcknowledgementPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private const val NOT_USED = "Not used by the operation workflows"

/** In-memory message tables the operation workflows read and change. */
internal class MessageStore {
    val messages = mutableMapOf<String, StoredMessage>()
    val texts = mutableMapOf<String, String>()
    val reactions = mutableSetOf<Triple<String, String?, String>>()

    fun store(message: StoredMessage, text: String) {
        messages[message.id] = message
        texts[message.id] = text
    }
}

internal class FakeDirectMessageRepository(
    private val store: MessageStore
) : DirectMessageRepository {
    override suspend fun find(messageId: String): Result<StoredMessage?> = Result.success(store.messages[messageId])

    override suspend fun findText(messageId: String): Result<String?> = Result.success(store.texts[messageId])

    override suspend fun replaceText(messageId: String, text: String): Result<Unit> {
        store.texts[messageId] = text
        return Result.success(Unit)
    }

    override suspend fun delete(messages: List<StoredMessage>): Result<Unit> {
        messages.forEach { message -> store.messages.remove(message.id) }
        return Result.success(Unit)
    }

    override suspend fun saveWithText(message: StoredMessage, text: String): Result<Unit> = error(NOT_USED)

    override suspend fun update(message: StoredMessage): Result<Unit> = error(NOT_USED)

    override suspend fun findWaitingForAuthorization(contactId: String): Result<List<StoredMessage>> = error(NOT_USED)

    override fun observeWaitingForAuthorization(): Flow<List<StoredMessage>> = error(NOT_USED)

    override suspend fun findAwaitingReadReceipt(conversationId: String): Result<List<UnreadIncomingMessage>> =
        error(NOT_USED)

    override suspend fun markReadReceiptSent(messageId: String): Result<Boolean> = error(NOT_USED)

    override suspend fun saveIncoming(
        message: StoredMessage,
        text: String,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = error(NOT_USED)
}

internal class FakeGroupMessageRepository(
    private val store: MessageStore
) : GroupMessageRepository {
    override suspend fun find(messageId: String): Result<StoredMessage?> = Result.success(store.messages[messageId])

    override suspend fun findText(messageId: String): Result<String?> = Result.success(store.texts[messageId])

    override suspend fun replaceText(messageId: String, text: String): Result<Unit> {
        store.texts[messageId] = text
        return Result.success(Unit)
    }

    override suspend fun delete(messages: List<StoredMessage>): Result<Unit> {
        messages.forEach { message -> store.messages.remove(message.id) }
        return Result.success(Unit)
    }

    override suspend fun findConversation(conversationId: String): Result<ConversationInfo?> = error(NOT_USED)

    override suspend fun findParticipantIds(groupId: String): Result<List<String>> = error(NOT_USED)

    override suspend fun hasMessageWithTransportMode(groupId: String, transportMode: String): Result<Boolean> =
        error(NOT_USED)

    override suspend fun findQueued(groupId: String): Result<List<StoredMessage>> = error(NOT_USED)

    override suspend fun findAwaitingOutbox(groupId: String): Result<List<StoredMessage>> = error(NOT_USED)

    override suspend fun saveOutgoing(
        message: StoredMessage,
        text: String,
        recipientStates: List<GroupRecipientState>
    ): Result<Unit> = error(NOT_USED)

    override suspend fun saveRecipientStates(states: List<GroupRecipientState>): Result<Unit> = error(NOT_USED)

    override suspend fun findRecipientStates(messageId: String): Result<List<GroupRecipientState>> = error(NOT_USED)

    override suspend fun findRecipientByPacketId(packetId: String): Result<GroupRecipientState?> = error(NOT_USED)

    override suspend fun findAwaitingReadReceipt(groupId: String): Result<List<UnreadIncomingMessage>> =
        error(NOT_USED)

    override suspend fun markReadReceiptSent(messageId: String): Result<Boolean> = error(NOT_USED)

    override suspend fun saveIncoming(
        message: StoredMessage,
        text: String,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = error(NOT_USED)
}

internal class FakeMessageReactionRepository(
    private val store: MessageStore
) : MessageReactionRepository {
    override suspend fun exists(messageId: String, reactorContactId: String?, emoji: String): Result<Boolean> =
        Result.success(Triple(messageId, reactorContactId, emoji) in store.reactions)

    override suspend fun save(
        messageId: String,
        conversationId: String,
        reactorContactId: String?,
        emoji: String
    ): Result<Unit> {
        store.reactions += Triple(messageId, reactorContactId, emoji)
        return Result.success(Unit)
    }

    override suspend fun delete(messageId: String, reactorContactId: String?, emoji: String): Result<Unit> {
        store.reactions -= Triple(messageId, reactorContactId, emoji)
        return Result.success(Unit)
    }
}

/** Messages without attachments. */
internal class FakeAttachmentRepository : MessageAttachmentOperationsRepository {
    override suspend fun messageParts(messageId: String): Result<List<MessagePart>> = Result.success(emptyList())

    override suspend fun deleteForMessages(messageIds: List<String>): Result<Unit> = Result.success(Unit)

    override suspend fun persistOutgoing(
        messageId: String,
        parts: List<MessagePart>,
        context: AttachmentMessageContext
    ): Result<List<MessagePart>> = error(NOT_USED)

    override suspend fun persistIncoming(
        messageId: String,
        parts: List<MessagePart>,
        context: AttachmentMessageContext
    ): Result<Unit> = error(NOT_USED)

    override suspend fun updateConversationDisplayName(
        conversationId: String,
        displayName: String,
        isGroup: Boolean
    ): Result<Unit> = error(NOT_USED)

    override suspend fun updateMessagePart(messageId: String, part: MessagePart): Result<Unit> = error(NOT_USED)

    override suspend fun prepareOutgoing(messageId: String): Result<List<MessagePart>> = error(NOT_USED)

    override suspend fun loadDetachedBytes(part: MessagePart): Result<ByteArray> = error(NOT_USED)

    override fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePart>>> =
        error(NOT_USED)

    override fun observeExpenses(conversationId: String): Flow<List<Expense>> = error(NOT_USED)

    override fun observeExpenseBoardClosures(conversationId: String): Flow<List<ExpenseBoardClosed>> =
        error(NOT_USED)

    override suspend fun findExpenseMessageIds(conversationId: String, boardId: String): Result<List<String>> =
        error(NOT_USED)

    override fun cacheIncoming(messageId: String): Result<Unit> = error(NOT_USED)
}

/** The parked table, ordered like the Room query: by sent time, then by insertion. */
internal class FakeParkedMessageOperationRepository : ParkedMessageOperationRepository {
    private var nextId = 1L
    val parked = mutableListOf<ParkedMessageOperation>()

    override suspend fun park(operation: IncomingCommand.ApplyRemoteMessageOperation): Result<Unit> {
        parked += ParkedMessageOperation(nextId++, operation)
        return Result.success(Unit)
    }

    override suspend fun findFor(messageId: String): Result<List<ParkedMessageOperation>> =
        Result.success(
            parked
                .filter { it.operation.operation.messageId == messageId }
                .sortedWith(compareBy({ it.operation.sentAtEpochMilliseconds }, { it.id }))
        )

    override suspend fun remove(id: Long): Result<Unit> {
        parked.removeAll { it.id == id }
        return Result.success(Unit)
    }
}

internal class FakeGroupMembershipRepository(
    private val administration: GroupAdministrationState = GroupAdministrationState()
) : GroupMembershipRepository {
    override suspend fun getVerificationMembershipContext(
        groupId: String
    ): Result<GroupVerificationMembershipContext> = error(NOT_USED)

    override suspend fun authorizeIncomingWelcome(
        groupId: String,
        senderContactId: String,
        packetId: String,
        epoch: Int
    ): Result<GroupIncomingWelcomeAuthorization?> = error(NOT_USED)

    override suspend fun sendGroupReadyAcknowledgement(
        groupId: String,
        epoch: Int,
        welcomePacketId: String,
        recipientContactId: String
    ): Result<Unit> = error(NOT_USED)

    override suspend fun completeIncomingWelcome(
        groupId: String,
        senderContactId: String,
        isFirstWelcome: Boolean,
        removedContactIds: Set<String>,
        persistedAt: Long
    ): Result<Unit> = error(NOT_USED)

    override suspend fun authorizeIncomingActivation(
        packet: GroupMemberActivatedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray,
        transportMode: String
    ): Result<Boolean> = error(NOT_USED)

    override suspend fun applyIncomingActivation(
        packet: GroupMemberActivatedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray,
        transportMode: String,
        memberContactId: String?,
        receivedAtEpochMilliseconds: Long
    ): Result<Boolean> = error(NOT_USED)

    override suspend fun authorizeIncomingGroupRemoval(
        packet: GroupMemberRemovedPacket,
        senderContactId: String
    ): Result<Boolean> = error(NOT_USED)

    override suspend fun completeIncomingGroupRemoval(
        packet: GroupMemberRemovedPacket
    ): Result<Unit> = error(NOT_USED)

    override suspend fun authorizeIncomingGroupDeletion(
        packet: GroupConversationDeletedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray
    ): Result<Unit> = error(NOT_USED)

    override suspend fun completeIncomingGroupDeletion(
        packet: GroupConversationDeletedPacket,
        ownerContactId: String
    ): Result<Unit> = error(NOT_USED)

    override suspend fun receiveReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = error(NOT_USED)

    override suspend fun receiveMemberActivationAcknowledgement(
        packet: GroupMemberActivationAcknowledgementPacket,
        acknowledgingContactId: String,
        transportMode: String
    ): Result<Unit> = error(NOT_USED)

    override suspend fun receiveLeaveRequest(
        memberContactId: String,
        packet: GroupLeaveRequestPacket,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> = error(NOT_USED)

    override suspend fun resolveMetadataMessageSender(
        groupId: String,
        epoch: Int,
        signingPublicKey: ByteArray
    ): Result<GroupMetadataMessageSender> = error(NOT_USED)

    override suspend fun wasGroupDeleted(groupId: String): Result<Boolean> = error(NOT_USED)

    override suspend fun inspectMessageAccess(
        groupId: String
    ): Result<GroupMessageMembershipAccess> = error(NOT_USED)

    override suspend fun resolvePinnedSenderSigningKey(
        groupId: String,
        isMine: Boolean,
        senderContactId: String?
    ): Result<ByteArray?> = error(NOT_USED)

    override suspend fun resolveTransportEncryptionPublicKey(
        groupId: String,
        contactId: String,
        useLatestMemberKey: Boolean
    ): Result<ByteArray?> = error(NOT_USED)

    override suspend fun getCurrentTransportRoutingMembers(
        groupId: String
    ): Result<List<GroupTransportRoutingMember>?> = error(NOT_USED)

    override suspend fun getAllCurrentTransportRoutingMembers(): Result<List<GroupTransportRoutingMember>> = error(NOT_USED)

    override suspend fun getCurrentEpoch(groupId: String): Result<Int> = error(NOT_USED)

    override suspend fun verifyGroupKeyConfirmation(
        groupId: String,
        epoch: Int,
        confirmation: ByteArray
    ): Result<Unit> = error(NOT_USED)

    override suspend fun authorizeMetadataSend(
        groupId: String,
        localSigningPublicKey: ByteArray,
        action: String
    ): Result<GroupMetadataSendContext> = error(NOT_USED)

    override suspend fun authorizeMetadataReceive(
        groupId: String,
        epoch: Int,
        contactId: String,
        adminSigningPublicKey: ByteArray,
        action: String
    ): Result<Boolean> = error(NOT_USED)

    override fun observeAdministration(groupId: String): Flow<GroupAdministrationState> = flowOf(administration)

    override fun observeConversationMembership(
        groupId: String
    ): Flow<GroupConversationMembershipSnapshot> = error(NOT_USED)

    override fun observeVerificationSnapshot(
        groupId: String
    ): Flow<MembershipVerificationSnapshot> = error(NOT_USED)

    override suspend fun removeMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> = error(NOT_USED)

    override suspend fun promoteMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberPromotionResult> = error(NOT_USED)

    override suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEnd> = error(NOT_USED)

    override suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement> = error(NOT_USED)

    override suspend fun leave(
        groupId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEnd> = error(NOT_USED)

    override suspend fun delete(groupId: String, context: GroupMembershipContext): Result<Long> = error(NOT_USED)
}
