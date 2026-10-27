package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.port.DirectReauthorizationPort
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDirectAuthorizationBlockUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.InspectDirectInvitationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.LoadDirectRecipientUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.StoreOutgoingDirectMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedAttachments
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedContacts
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.UnusedDirectMessages
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchange
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeAcceptance
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeBinding
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosure
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeOffer
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeReady
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.model.IdentityPeerState
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityReadRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationLifecycleStatusUseCase
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.IdentityPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

private const val NOT_USED = "Not used by the direct send workflows"

internal const val CONTACT_ID = "contact-1"
internal const val CONVERSATION_ID = "conversation-1"

/**
 * The authorization a direct send checks: a contact in [authorizedPeers] has mutual keys; a contact with an entry in
 * [pendingIdentityChanges] waits for the review of its new identity.
 */
internal class DirectAuthorizationFakes(
    val authorizedPeers: MutableSet<String> = mutableSetOf(CONTACT_ID)
) {
    val pendingIdentityChanges = MutableStateFlow<List<PendingRemoteIdentityChange>>(emptyList())

    private val identities = FakeIdentityExchanges(authorizedPeers)
    private val pending = FakePendingChanges(pendingIdentityChanges)

    val findDirectAuthorizationBlock =
        FindDirectAuthorizationBlockUseCase(
            GetIdentityPeerStateUseCase(identities, pending),
            GetRemoteIdentityUseCase(FakeRemoteIdentities(authorizedPeers)),
            ObservePendingRemoteIdentityChangesUseCase(pending)
        )

    val requireDirectChatAuthorization = RequireDirectChatAuthorizationUseCase(NoBlockedContacts, findDirectAuthorizationBlock)

    /** Never asked: the tests use authorized contacts or contacts with a pending identity change. */
    val inspectDirectInvitation =
        InspectDirectInvitationUseCase(
            ObserveInvitationLifecycleStatusUseCase(NoInvitations),
            ObserveIdentityHandshakeStateUseCase(identities),
            NoSetupMode
        )

    fun startIdentityChange(peerId: String) {
        pendingIdentityChanges.value =
            listOf(
                PendingRemoteIdentityChange(
                    peerId = peerId,
                    sourcePeerId = peerId,
                    invitationId = "invitation-1",
                    proposedEncryptionPublicKey = ByteArray(32) { 3 },
                    proposedSigningPublicKey = ByteArray(32) { 4 },
                    receivedAtEpochMilliseconds = 1L,
                    expiresAtEpochMilliseconds = Long.MAX_VALUE
                )
            )
    }

    fun approveIdentityChange() {
        pendingIdentityChanges.value = emptyList()
    }
}

/** Storing a waiting message is not reached by the tests; they stop at the authorization check under the lock. */
internal val unusedLoadDirectRecipient = LoadDirectRecipientUseCase(UnusedContacts)
internal val unusedStoreOutgoingDirectMessage =
    StoreOutgoingDirectMessageUseCase(UnusedDirectMessages, UnusedAttachments)

/** Every contact has the conversation [CONVERSATION_ID]; [resolved] completes once a send looked it up. */
internal class FakeDirectConversations : DirectConversationRepository {
    val resolved = MutableStateFlow(0)

    override suspend fun getOrCreate(contactId: String): Result<String> {
        resolved.value++
        return Result.success(CONVERSATION_ID)
    }

    override suspend fun findConversationId(contactId: String): Result<String?> = error(NOT_USED)

    override suspend fun findContactId(conversationId: String): Result<String?> = error(NOT_USED)

    override suspend fun requireTarget(conversationId: String): Result<DirectConversationTarget> = error(NOT_USED)

    override suspend fun delete(conversationId: String): Result<Unit> = error(NOT_USED)
}

internal object NoReauthorization : DirectReauthorizationPort {
    override suspend fun requestReauthorization(contactId: String): Result<Unit> = error(NOT_USED)
}

private class FakeRemoteIdentities(
    private val authorizedPeers: Set<String>
) : RemoteIdentityReadRepository {
    override suspend fun get(peerId: String): Result<RemotePeerIdentity?> =
        Result.success(
            RemotePeerIdentity(
                peerId = peerId,
                encryptionPublicKey = ByteArray(32) { 1 },
                signingPublicKey = ByteArray(32) { 2 },
                verificationStatus = ContactVerificationStatus.UNVERIFIED,
                keyExchangeStatus = KeyExchangeStatus.MUTUAL,
                verifiedByContact = false,
                locallyImported = false,
                updatedAtEpochMilliseconds = 1L
            ).takeIf { peerId in authorizedPeers }
        )

    override suspend fun findPeerIdBySigningPublicKey(signingPublicKey: ByteArray): Result<String?> = error(NOT_USED)

    override fun observeAll(): Flow<List<RemotePeerIdentity>> = error(NOT_USED)
}

private class FakePendingChanges(
    private val changes: MutableStateFlow<List<PendingRemoteIdentityChange>>
) : PendingRemoteIdentityChangeRepository {
    override suspend fun stage(candidate: PendingRemoteIdentityChange): Result<Unit> = error(NOT_USED)

    override fun observeAll(): Flow<List<PendingRemoteIdentityChange>> = changes

    override suspend fun discard(peerId: String, invitationId: String): Result<Unit> = error(NOT_USED)

    override suspend fun approveReplacement(
        peerId: String,
        invitationId: String,
        presentedSigningFingerprint: String,
        presentedEncryptionFingerprint: String
    ): Result<Unit> = error(NOT_USED)
}

private object NoBlockedContacts : ContactBlocklistRepository {
    override fun observeBlockUnknownContactInvites(): Flow<Boolean> = error(NOT_USED)

    override suspend fun getBlockUnknownContactInvites(): Result<Boolean> = error(NOT_USED)

    override suspend fun setBlockUnknownContactInvites(enabled: Boolean): Result<Unit> = error(NOT_USED)

    override fun observeBlockedContactIds(): Flow<Set<String>> = error(NOT_USED)

    override suspend fun getBlockedContactIds(): Result<Set<String>> = error(NOT_USED)

    override suspend fun isBlocked(contactId: String): Result<Boolean> = Result.success(false)

    override suspend fun block(contactId: String): Result<Unit> = error(NOT_USED)

    override suspend fun unblock(contactId: String): Result<Unit> = error(NOT_USED)
}

private object NoSetupMode : DirectIdentitySetupModeRepository {
    override fun observeMode(): Flow<DirectIdentitySetupMode> = error(NOT_USED)

    override suspend fun getMode(): Result<DirectIdentitySetupMode> = error(NOT_USED)

    override suspend fun setMode(mode: DirectIdentitySetupMode): Result<Unit> = error(NOT_USED)
}

private object NoInvitations : InvitationRepository {
    override fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>> = error(NOT_USED)

    override fun observeInvitationResults(): Flow<List<InvitationResult>> = error(NOT_USED)

    override fun observeLifecycleStatus(
        payloadType: InvitationPayloadType,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Flow<InvitationLifecycleStatus?> = error(NOT_USED)

    override suspend fun getPeerId(invitationId: String): Result<String> = error(NOT_USED)

    override suspend fun shouldRecordPending(record: InvitationLifecycleRecord): Result<Boolean> = error(NOT_USED)

    override suspend fun recordPending(record: InvitationLifecycleRecord): Result<Unit> = error(NOT_USED)

    override suspend fun validatePending(
        payloadType: InvitationPayloadType,
        invitationId: String,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection,
        atEpochMilliseconds: Long
    ): Result<Unit> = error(NOT_USED)

    override suspend fun getPayloadType(invitationId: String): Result<InvitationPayloadType> = error(NOT_USED)

    override suspend fun accept(invitationId: String): Result<Unit> = error(NOT_USED)

    override suspend fun decline(invitationId: String, action: InvitationResultAction?): Result<Unit> = error(NOT_USED)

    override suspend fun applyResponse(
        payloadType: InvitationPayloadType,
        invitationId: String,
        response: InvitationResponse
    ): Result<Unit> = error(NOT_USED)

    override suspend fun invalidatePending(invitationId: String): Result<Unit> = error(NOT_USED)

    override suspend fun markTransportFailed(payloadType: InvitationPayloadType, invitationId: String): Result<Unit> =
        error(NOT_USED)

    override suspend fun markViewed(direction: InvitationDirection): Result<Unit> = error(NOT_USED)

    override suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> = error(NOT_USED)
}

/** Identity exchanges; a contact in [authorizedPeers] has an established exchange. */
internal class FakeIdentityExchanges(
    private val authorizedPeers: Set<String>
) : IdentityExchangeRepository {
    override suspend fun getPeerState(peerId: String): Result<IdentityPeerState> =
        Result.success(IdentityPeerState(hasEstablishedExchange = peerId in authorizedPeers, hasMutualIdentity = true))

    override suspend fun stageRemoteIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Boolean> = error(NOT_USED)

    override suspend fun acceptRemoteIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> = error(NOT_USED)

    override suspend fun establishMutualIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> = error(NOT_USED)

    override suspend fun ensureRemoteSigningIdentity(peerId: String, signingPublicKey: ByteArray): Result<Unit> =
        error(NOT_USED)

    override suspend fun start(peerId: String, invitationSenderLabel: String): Result<IdentityExchange?> =
        error(NOT_USED)

    override suspend fun startManual(peerId: String): Result<Unit> = error(NOT_USED)

    override suspend fun accept(exchangeId: String): Result<Unit> = error(NOT_USED)

    override suspend fun decline(exchangeId: String): Result<Unit> = error(NOT_USED)

    override fun observeState(peerId: String): Flow<IdentityHandshakeState?> = error(NOT_USED)

    override fun observeResults(): Flow<List<IdentityResult>> = error(NOT_USED)

    override suspend fun cancel(peerId: String): Result<Unit> = error(NOT_USED)

    override suspend fun getExchangeClosure(peerId: String): Result<IdentityExchangeClosure?> = error(NOT_USED)

    override suspend fun closeExchange(exchangeId: String, peerId: String): Result<Unit> = error(NOT_USED)

    override suspend fun getExchangeBinding(exchangeId: String): Result<IdentityExchangeBinding?> = error(NOT_USED)

    override suspend fun invalidateExchange(
        exchangeId: String,
        peerId: String,
        expectedChallenge: ByteArray,
        expectedSigningPublicKey: ByteArray,
        atEpochMilliseconds: Long
    ): Result<Unit> = error(NOT_USED)

    override suspend fun receiveExchange(
        context: IncomingPacketContext,
        offer: IdentityExchangeOffer,
        wasKnownPeerAtReceive: Boolean
    ): Result<Unit> = error(NOT_USED)

    override suspend fun acceptApprovedIncomingExchange(
        context: IncomingPacketContext,
        offer: IdentityExchangeOffer,
        wasKnownPeerAtReceive: Boolean
    ): Result<Unit> = error(NOT_USED)

    override suspend fun reassignPeer(fromPeerId: String, toPeerId: String): Result<Unit> = error(NOT_USED)

    override suspend fun receiveAccepted(
        context: IncomingPacketContext,
        acceptance: IdentityExchangeAcceptance
    ): Result<Unit> = error(NOT_USED)

    override suspend fun recordRemoteDecline(
        exchangeId: String,
        peerId: String,
        inviteChallenge: ByteArray,
        remoteSigningPublicKey: ByteArray,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = error(NOT_USED)

    override suspend fun receiveReady(context: IncomingPacketContext, ready: IdentityExchangeReady): Result<Unit> =
        error(NOT_USED)

    override suspend fun receiveManualIdentity(context: IncomingPacketContext, packet: IdentityPacket): Result<Boolean> =
        error(NOT_USED)

    override suspend fun receiveIdentityAcknowledgement(
        context: IncomingPacketContext,
        packet: IdentityAcknowledgementPacket
    ): Result<Boolean> = error(NOT_USED)
}
