package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTranscript
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupPinPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupTitlePacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupCommittedMemberRemoval
import com.cbgm.sparrow.feature.membership.domain.model.GroupConversationMembershipSnapshot
import com.cbgm.sparrow.feature.membership.domain.model.GroupIncomingWelcomeAuthorization
import com.cbgm.sparrow.feature.membership.domain.model.GroupInvitationTarget
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
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.outbox.ProtocolOutboxFailureEvent
import com.cbgm.sparrow.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.GroupTitleUpdatedPacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

private const val NOT_USED = "Not used by the group state workflows"

const val GROUP_ID = "group-1"
val ADMIN_KEYS = LocalSigningKeyPair(publicKey = ByteArray(32) { 7 }, privateKey = ByteArray(64) { 8 })

/** The outbox; while [failures] is positive, every enqueue fails and counts it down. */
internal class FakeOutbox : ProtocolOutbox {
    var failures = 0
    val enqueued = mutableListOf<Pair<String, SparrowPacket>>()

    override suspend fun enqueue(contactId: String, packet: SparrowPacket): Result<ProtocolOutboxItem> {
        if (failures > 0) {
            failures--
            return Result.failure(IllegalStateException("Outbox is not available"))
        }
        enqueued += contactId to packet
        return Result.success(
            ProtocolOutboxItem(
                id = "item-${enqueued.size}",
                contactId = contactId,
                packetId = packet.packetId,
                encodedPacket = byteArrayOf(1),
                status = OutboxStatus.PENDING,
                attemptCount = 0,
                lastError = null,
                createdAtEpochMilliseconds = 1L,
                updatedAtEpochMilliseconds = 1L
            )
        )
    }

    override fun observePending(): Flow<List<ProtocolOutboxItem>> = error(NOT_USED)

    override fun observeTransportStates(): Flow<List<ProtocolOutboxItem>> = error(NOT_USED)

    override fun observeUnacknowledgedFailures(): Flow<List<ProtocolOutboxFailureEvent>> = error(NOT_USED)

    override suspend fun acknowledgeFailure(eventId: String): Result<Unit> = error(NOT_USED)

    override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> = error(NOT_USED)

    override suspend fun markProcessing(itemId: String): Result<Unit> = error(NOT_USED)

    override suspend fun markSent(itemId: String): Result<Unit> = error(NOT_USED)

    override suspend fun markFailed(itemId: String, errorMessage: String): Result<Unit> = error(NOT_USED)

    override suspend fun retry(itemId: String): Result<Unit> = error(NOT_USED)

    override suspend fun resend(packetId: String): Result<Unit> = error(NOT_USED)

    override suspend fun requeueInterrupted(): Result<Unit> = error(NOT_USED)

    override suspend fun retryFailed(): Result<Unit> = error(NOT_USED)

    override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> = error(NOT_USED)
}

internal object FakeSigningKeys : LocalSigningKeyPairProvider {
    override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> = Result.success(ADMIN_KEYS)
}

/** The title of [GROUP_ID]; marking it published honours the version like the database does. */
internal class FakeTitleRepository(
    var stored: GroupTitleState
) : GroupTitleRepository {
    override suspend fun get(groupId: String): Result<GroupTitleState> = Result.success(stored)

    override suspend fun save(state: GroupTitleState): Result<Unit> {
        stored = state
        return Result.success(Unit)
    }

    override suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long): Result<Unit> {
        if (stored.changedAtEpochMilliseconds == changedAtEpochMilliseconds) stored = stored.copy(publishPending = false)
        return Result.success(Unit)
    }

    override suspend fun findPublishPendingGroupIds(): Result<List<String>> =
        Result.success(listOfNotNull(stored.groupId.takeIf { stored.publishPending }))

    override suspend fun deleteLocal(groupId: String): Result<Unit> = error(NOT_USED)
}

/** The pin slot of [GROUP_ID]; marking it published honours the version like the database does. */
internal class FakePinRepository(
    var stored: GroupPinState?
) : GroupPinRepository {
    override suspend fun find(groupId: String): Result<GroupPinState?> = Result.success(stored)

    override suspend fun save(pin: GroupPinState): Result<Unit> {
        stored = pin
        return Result.success(Unit)
    }

    override suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long): Result<Unit> {
        stored = stored?.let { pin ->
            if (pin.changedAtEpochMilliseconds == changedAtEpochMilliseconds) pin.copy(publishPending = false) else pin
        }
        return Result.success(Unit)
    }

    override suspend fun findPublishPendingGroupIds(): Result<List<String>> =
        Result.success(listOfNotNull(stored?.takeIf { pin -> pin.publishPending }?.groupId))

    override suspend fun deleteLocal(groupId: String): Result<Unit> = error(NOT_USED)
}

/** A group without description changes. */
internal object FakeDescriptionRepository : GroupDescriptionRepository {
    override suspend fun get(groupId: String): Result<GroupDescriptionState> =
        Result.success(GroupDescriptionState(groupId, description = null, changedAtEpochMilliseconds = 0L))

    override suspend fun save(state: GroupDescriptionState): Result<Unit> = error(NOT_USED)

    override suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long): Result<Unit> =
        error(NOT_USED)

    override suspend fun findPublishPendingGroupIds(): Result<List<String>> = Result.success(emptyList())

    override suspend fun deleteLocal(groupId: String): Result<Unit> = error(NOT_USED)
}

/** A group without avatar changes. */
internal object FakeAvatarRepository : GroupAvatarRepository {
    override suspend fun get(groupId: String): Result<GroupAvatarState> =
        Result.success(GroupAvatarState(groupId, bytes = null, changedAtEpochMilliseconds = 0L))

    override suspend fun save(state: GroupAvatarState): Result<Unit> = error(NOT_USED)

    override suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long): Result<Unit> =
        error(NOT_USED)

    override suspend fun findPublishPendingGroupIds(): Result<List<String>> = Result.success(emptyList())

    override suspend fun deleteLocal(groupId: String): Result<Unit> = error(NOT_USED)
}

/** Unsigned packets: the workflows only hand them to the outbox. */
internal object FakeTitleCodec : GroupTitlePacketCodec {
    private var next = 0

    override suspend fun create(state: GroupTitleState, context: GroupAdminContext): Result<GroupTitleUpdatedPacket> =
        Result.success(
            GroupTitleUpdatedPacket(
                packetId = "title-${next++}",
                groupId = state.groupId,
                epoch = context.epoch,
                title = state.title,
                changedAtEpochMilliseconds = state.changedAtEpochMilliseconds,
                adminSigningPublicKey = context.signingKeyPair.publicKey,
                adminSignature = byteArrayOf(1)
            )
        )

    override suspend fun verify(packet: GroupTitleUpdatedPacket): Result<Unit> = error(NOT_USED)
}

internal object FakePinCodec : GroupPinPacketCodec {
    private var next = 0

    override suspend fun create(pin: GroupPinState, context: GroupAdminContext): Result<GroupPinUpdatedPacket> =
        Result.success(
            GroupPinUpdatedPacket(
                packetId = "pin-${next++}",
                groupId = pin.groupId,
                epoch = context.epoch,
                messageId = pin.messageId,
                changedAtEpochMilliseconds = pin.changedAtEpochMilliseconds,
                adminSigningPublicKey = context.signingKeyPair.publicKey,
                adminSignature = byteArrayOf(1)
            )
        )

    override suspend fun verify(packet: GroupPinUpdatedPacket): Result<Unit> = error(NOT_USED)

    override fun toGroupPinState(packet: GroupPinUpdatedPacket, senderContactId: String?, isMine: Boolean): GroupPinState =
        error(NOT_USED)
}

/** Message parts of the group: only board closures are stored, and there are no expenses with receipts. */
internal class FakeAttachmentOperations : MessageAttachmentOperationsRepository {
    val closures = MutableStateFlow<List<ExpenseBoardClosed>>(emptyList())

    override fun observeExpenseBoardClosures(conversationId: String): Flow<List<ExpenseBoardClosed>> = closures

    override fun observeExpenses(conversationId: String): Flow<List<Expense>> = flowOf(emptyList())

    override suspend fun messageParts(messageId: String): Result<List<MessagePart>> = error(NOT_USED)

    override suspend fun deleteForMessages(messageIds: List<String>): Result<Unit> = error(NOT_USED)

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

    override suspend fun findExpenseMessageIds(conversationId: String, boardId: String): Result<List<String>> =
        error(NOT_USED)

    override fun cacheIncoming(messageId: String): Result<Unit> = error(NOT_USED)
}

internal object FakeAttachmentFiles : MessageAttachmentRepository {
    override suspend fun loadLocalFile(partId: String, groupId: String?): Result<String> = error(NOT_USED)

    override suspend fun loadLocation(partId: String, groupId: String?): Result<CurrentLocation> = error(NOT_USED)

    override suspend fun loadContact(partId: String, groupId: String?): Result<SharedContact> = error(NOT_USED)

    override suspend fun loadBytes(partId: String, groupId: String?): Result<ByteArray> = error(NOT_USED)

    override suspend fun saveTranscript(attachmentId: String, transcription: AttachmentTranscript): Result<Unit> =
        error(NOT_USED)

    override fun observeTranscript(attachmentId: String): Flow<AttachmentTranscript?> = error(NOT_USED)

    override fun observeLocalAttachments(conversationId: String): Flow<List<MessagePart>> = error(NOT_USED)

    override fun observeStorageSummaries(): Flow<List<AttachmentStorageSummary>> = error(NOT_USED)

    override suspend fun deleteLocalAttachments(attachmentIds: Set<String>): Result<Unit> = error(NOT_USED)

    override suspend fun deleteLocalAttachmentsForConversation(conversationId: String): Result<Unit> = error(NOT_USED)
}

/** Membership authorizes the local admin for [epoch] and addresses [recipients]. */
internal class FakeGroupMembershipRepository(
    private val epoch: Int = 2,
    private val recipients: Set<String> = setOf("alice", "bob")
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

    override suspend fun inspectInvitationTarget(groupId: String): Result<GroupInvitationTarget> = error(NOT_USED)

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

    override suspend fun findCurrentEpoch(groupId: String): Result<Int?> = error(NOT_USED)

    override suspend fun verifyGroupKeyConfirmation(
        groupId: String,
        epoch: Int,
        confirmation: ByteArray
    ): Result<Unit> = error(NOT_USED)

    override suspend fun authorizeMetadataSend(
        groupId: String,
        localSigningPublicKey: ByteArray,
        action: String
    ): Result<GroupMetadataSendContext> = Result.success(GroupMetadataSendContext(epoch, recipients))

    override suspend fun authorizeMetadataReceive(
        groupId: String,
        epoch: Int,
        contactId: String,
        adminSigningPublicKey: ByteArray,
        action: String
    ): Result<Boolean> = error(NOT_USED)

    override fun observeAdministration(groupId: String): Flow<GroupAdministrationState> = error(NOT_USED)

    override fun observeConversationMembership(
        groupId: String
    ): Flow<GroupConversationMembershipSnapshot> = error(NOT_USED)

    override fun observeVerificationSnapshot(
        groupId: String
    ): Flow<MembershipVerificationSnapshot> = error(NOT_USED)

    override suspend fun findCommittedRemoval(
        groupId: String,
        contactId: String
    ): Result<GroupCommittedMemberRemoval?> = error(NOT_USED)

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
