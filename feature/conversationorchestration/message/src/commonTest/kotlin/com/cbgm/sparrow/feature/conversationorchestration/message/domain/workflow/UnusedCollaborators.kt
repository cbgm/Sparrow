package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.ConversationInfo
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessagePlaintext
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupRecipientState
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.GroupMessagePlaintextCodec
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
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
import com.cbgm.sparrow.feature.membership.domain.model.GroupWelcomeMemberKey
import com.cbgm.sparrow.feature.membership.domain.model.MembershipVerificationSnapshot
import com.cbgm.sparrow.feature.membership.domain.model.OpenedGroupWelcomeDto
import com.cbgm.sparrow.feature.membership.domain.model.SecuredGroupMessageDto
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.protocol.message.OperationMessage
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.outbox.ProtocolOutboxFailureEvent
import com.cbgm.sparrow.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket
import com.cbgm.sparrow.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.protocol.profile.ProfilePictureMetadata
import kotlinx.coroutines.flow.Flow

// Collaborators a workflow needs to be built but a test never reaches; every call fails loudly.
private const val NOT_USED = "Not used by this test"

internal object UnusedContacts : ContactRepository {
    override suspend fun importDeviceContact(request: ImportDeviceContactRequest): Result<Contact> = error(NOT_USED)

    override suspend fun upsertImportedContact(request: ImportContactRequest): Result<Contact> = error(NOT_USED)

    override suspend fun getContact(contactId: String): Result<Contact?> = error(NOT_USED)

    override suspend fun usePhoneNumberAsDisplayNameWhenMissing(
        contactId: String,
        phoneNumber: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> = error(NOT_USED)

    override suspend fun findOrCreateByPhoneNumber(phoneNumber: String): Result<Contact> = error(NOT_USED)

    override suspend fun resolveAuthenticatedPeerContact(senderContactId: String?, phoneNumber: String?): Result<String> =
        error(NOT_USED)

    override fun observeContacts(): Flow<List<Contact>> = error(NOT_USED)

    override suspend fun updateContactDetails(
        contactId: String,
        displayName: String?,
        phoneNumber: String?
    ): Result<Contact> = error(NOT_USED)

    override suspend fun updateDeviceContactLinkStatus(
        deviceContactId: String,
        status: DeviceContactLinkStatus
    ): Result<Contact?> = error(NOT_USED)
}

internal object UnusedDirectMessages : DirectMessageRepository {
    override suspend fun find(messageId: String): Result<StoredMessage?> = error(NOT_USED)

    override suspend fun findText(messageId: String): Result<String?> = error(NOT_USED)

    override suspend fun saveWithText(message: StoredMessage, text: String): Result<Unit> = error(NOT_USED)

    override suspend fun update(message: StoredMessage): Result<Unit> = error(NOT_USED)

    override suspend fun updatePacketIfStatus(
        messageId: String,
        expectedStatus: MessageDeliveryStatus,
        packetId: String,
        transportMode: String,
        deliveryStatus: MessageDeliveryStatus
    ): Result<Boolean> = error(NOT_USED)

    override suspend fun replaceText(messageId: String, text: String, editedAtEpochMilliseconds: Long): Result<Unit> =
        error(NOT_USED)

    override suspend fun replaceTextIfNewer(
        messageId: String,
        text: String,
        editedAtEpochMilliseconds: Long
    ): Result<Boolean> = error(NOT_USED)

    override suspend fun delete(messages: List<StoredMessage>): Result<Unit> = error(NOT_USED)

    override suspend fun findWaitingForAuthorization(contactId: String): Result<List<StoredMessage>> = error(NOT_USED)

    override fun observeWaitingForAuthorization(): Flow<List<StoredMessage>> = error(NOT_USED)

    override suspend fun findAwaitingOutbox(createdBeforeEpochMilliseconds: Long): Result<List<StoredMessage>> =
        error(NOT_USED)

    override suspend fun findAwaitingReadReceipt(conversationId: String): Result<List<UnreadIncomingMessage>> =
        error(NOT_USED)

    override suspend fun markReadReceiptSent(messageId: String): Result<Boolean> = error(NOT_USED)

    override suspend fun saveIncoming(
        message: StoredMessage,
        text: String,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = error(NOT_USED)
}

internal object UnusedAttachments : MessageAttachmentOperationsRepository {
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

    override suspend fun messageParts(messageId: String): Result<List<MessagePart>> = error(NOT_USED)

    override suspend fun prepareOutgoing(messageId: String): Result<List<MessagePart>> = error(NOT_USED)

    override suspend fun loadDetachedBytes(part: MessagePart): Result<ByteArray> = error(NOT_USED)

    override suspend fun deleteForMessages(messageIds: List<String>): Result<Unit> = error(NOT_USED)

    override fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePart>>> = error(NOT_USED)

    override fun observeExpenses(conversationId: String): Flow<List<Expense>> = error(NOT_USED)

    override fun observeExpenseBoardClosures(conversationId: String): Flow<List<ExpenseBoardClosed>> = error(NOT_USED)

    override suspend fun findExpenseMessageIds(conversationId: String, boardId: String): Result<List<String>> =
        error(NOT_USED)

    override fun cacheIncoming(messageId: String): Result<Unit> = error(NOT_USED)
}

internal object UnusedGroupMessages : GroupMessageRepository {
    override suspend fun findConversation(conversationId: String): Result<ConversationInfo?> = error(NOT_USED)

    override suspend fun findParticipantIds(groupId: String): Result<List<String>> = error(NOT_USED)

    override suspend fun hasMessageWithTransportMode(groupId: String, transportMode: String): Result<Boolean> =
        error(NOT_USED)

    override suspend fun find(messageId: String): Result<StoredMessage?> = error(NOT_USED)

    override suspend fun findText(messageId: String): Result<String?> = error(NOT_USED)

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

    override suspend fun replaceText(messageId: String, text: String, editedAtEpochMilliseconds: Long): Result<Unit> =
        error(NOT_USED)

    override suspend fun replaceTextIfNewer(
        messageId: String,
        text: String,
        editedAtEpochMilliseconds: Long
    ): Result<Boolean> = error(NOT_USED)

    override suspend fun delete(messages: List<StoredMessage>): Result<Unit> = error(NOT_USED)

    override suspend fun findAwaitingReadReceipt(groupId: String): Result<List<UnreadIncomingMessage>> = error(NOT_USED)

    override suspend fun markReadReceiptSent(messageId: String): Result<Boolean> = error(NOT_USED)

    override suspend fun saveIncoming(
        message: StoredMessage,
        text: String,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = error(NOT_USED)
}

internal object UnusedGroupSecurity : GroupSecurityRepository {
    override suspend fun initializeOwnedGroup(
        groupId: String,
        createdAtEpochMilliseconds: Long,
        localSigningKeyPair: LocalSigningKeyPair
    ): Result<Unit> = error(NOT_USED)

    override fun createKeyConfirmation(groupId: String, epoch: Int, groupKey: ByteArray): Result<ByteArray> =
        error(NOT_USED)

    override suspend fun openWelcome(
        packet: GroupCreatedPacket,
        senderContactId: String,
        expectedOwnerEncryptionPublicKey: ByteArray,
        expectedOwnerSigningPublicKey: ByteArray,
        localEncryptionKeyPair: LocalEncryptionKeyPair,
        localSigningPublicKey: ByteArray
    ): Result<OpenedGroupWelcomeDto> = error(NOT_USED)

    override suspend fun persistJoinedGroup(
        openedWelcome: OpenedGroupWelcomeDto,
        ownerContactId: String,
        authoritySigningPublicKey: ByteArray,
        localSigningPublicKey: ByteArray,
        memberKeys: List<GroupWelcomeMemberKey>,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = error(NOT_USED)

    override suspend fun encryptMessage(
        groupId: String,
        messageId: String,
        sentAtEpochMilliseconds: Long,
        plaintext: String,
        localSigningKeyPair: LocalSigningKeyPair,
        profilePicture: ProfilePictureMetadata
    ): Result<SecuredGroupMessageDto> = error(NOT_USED)

    override suspend fun decryptMessage(packet: GroupChatMessagePacket, senderContactId: String): Result<String> =
        error(NOT_USED)
}

internal object UnusedSigningKeys : LocalSigningKeyPairProvider {
    override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> = error(NOT_USED)
}

internal object UnusedProfilePictures : LocalProfilePictureMetadataProvider {
    override suspend fun forInvite(): Result<ProfilePictureMetadata> = error(NOT_USED)

    override suspend fun forMessage(): Result<ProfilePictureMetadata> = error(NOT_USED)
}

internal object UnusedPlaintextCodec : GroupMessagePlaintextCodec {
    override fun encodeMessage(
        messageId: String,
        text: String,
        parts: List<MessagePart>,
        replyToMessageId: String?
    ): String = error(NOT_USED)

    override fun encodeOperation(message: OperationMessage): String = error(NOT_USED)

    override fun decode(plaintext: String): GroupMessagePlaintext = error(NOT_USED)
}

internal object UnusedOutbox : ProtocolOutbox {
    override suspend fun enqueue(contactId: String, packet: SparrowPacket): Result<ProtocolOutboxItem> = error(NOT_USED)

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

internal object UnusedGroupMembership : GroupMembershipRepository {
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
    ): Result<GroupMetadataSendContext> = error(NOT_USED)

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
