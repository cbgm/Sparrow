package com.cbgm.sparrow.feature.conversationorchestration.domain.workflow

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.authorization.DirectChatAuthorizationRevocationProtocol
import com.cbgm.sparrow.core.protocol.authorization.DirectChatAuthorizationRevocationSender
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.invitation.ContactInvitationDeclineProtocol
import com.cbgm.sparrow.core.protocol.invitation.ContactInvitationHandshakeProtocol
import com.cbgm.sparrow.core.protocol.invitation.ContactInvitationRequestProtocol
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.IdentityPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.contacts.domain.model.IncomingPeerContactCandidate
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactInvitationRoutingIdUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveIncomingPeerContactsUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.ApplyIdentityPeerMergeUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.GetIdentityPeerDisplayNameUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.UpdateIncomingIdentityPeerMetadataUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.error.RemoteIdentityReplacementRequiredException
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ResolveIncomingIdentityPeerUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ResolveSigningIdentityContactUseCase
import com.cbgm.sparrow.feature.identity.domain.error.IdentityAcceptanceRequiresReviewException
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchange
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeAcceptance
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosurePhase
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeDirection
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeOffer
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeReady
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResultStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.AcceptIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.AcceptRemoteIdentityHandshakeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ApplyRemoteProfilePictureMetadataUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.CancelIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.CloseIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DeclineIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.EnsureRemoteSigningIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.EstablishMutualIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityExchangeBindingUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityExchangeClosureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.InvalidateIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReassignIdentityExchangePeerUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityAcknowledgementUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityExchangeAcceptedUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityReadyUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveManualIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.RecordRemoteIdentityDeclineUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SendIdentityVerificationReceiptUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StagePendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StageRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StartIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StartManualIdentityExchangeUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.InvalidatePendingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationTransportFailedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.RecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ShouldRecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalReason
import com.cbgm.sparrow.feature.membership.domain.model.GroupWelcomeMemberKey
import com.cbgm.sparrow.feature.membership.domain.model.MembershipDeclineDisposition
import com.cbgm.sparrow.feature.membership.domain.model.MembershipPerspective
import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipStatus
import com.cbgm.sparrow.feature.membership.domain.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.usecase.AcceptGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ApplyIncomingGroupActivationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupActivationUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupDeletionUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupRemovalUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ClearMembershipHandshakeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.CompleteIncomingGroupDeletionUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.CompleteIncomingGroupRemovalUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.CompleteIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ConfirmGroupMembershipIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DeclineGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DeleteGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DiscardSupersededMembershipsUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupCurrentEpochUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.InspectIncomingGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.LeaveGroupUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.MarkMembershipRemovedUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.OpenIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.PersistIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.PromoteGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupActivationAcknowledgementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupLeaveRequestUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipDeclineUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipJoinRequestUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipReceiptUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupReadyAcknowledgementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveIncomingGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.RemoveGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.SendGroupReadyAcknowledgementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.StartGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.TransferGroupAdminAndLeaveUseCase

/**
 * The only place in conversation orchestration that sequences public feature use cases.
 *
 * Observers forward feature results here; they do not perform workflow decisions themselves.
 */
@Suppress("LongParameterList")
internal class ConversationFlowHandler(
    private val startIdentityExchange: StartIdentityExchangeUseCase,
    private val getIdentityPeerState: GetIdentityPeerStateUseCase,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val resolveContactInvitationRoutingId: ResolveContactInvitationRoutingIdUseCase,
    private val acceptIdentityExchange: AcceptIdentityExchangeUseCase,
    private val declineIdentityExchange: DeclineIdentityExchangeUseCase,
    private val receiveIdentityExchange: ReceiveIdentityExchangeUseCase,
    private val receiveIdentityExchangeAccepted: ReceiveIdentityExchangeAcceptedUseCase,
    private val recordRemoteIdentityDecline: RecordRemoteIdentityDeclineUseCase,
    private val invitationDeclineProtocol: ContactInvitationDeclineProtocol,
    private val invitationRequestProtocol: ContactInvitationRequestProtocol,
    private val invitationHandshakeProtocol: ContactInvitationHandshakeProtocol,
    private val receiveIdentityReady: ReceiveIdentityReadyUseCase,
    private val getIdentityExchangeBinding: GetIdentityExchangeBindingUseCase,
    private val invalidateIdentityExchange: InvalidateIdentityExchangeUseCase,
    private val revocationProtocol: DirectChatAuthorizationRevocationProtocol,
    private val getIdentityExchangeClosure: GetIdentityExchangeClosureUseCase,
    private val closeIdentityExchange: CloseIdentityExchangeUseCase,
    private val cancelIdentityExchange: CancelIdentityExchangeUseCase,
    private val startManualIdentityExchange: StartManualIdentityExchangeUseCase,
    private val revocationSender: DirectChatAuthorizationRevocationSender,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle,
    private val receiveManualIdentity: ReceiveManualIdentityUseCase,
    private val receiveIdentityAcknowledgement: ReceiveIdentityAcknowledgementUseCase,
    private val reassignIdentityExchangePeer: ReassignIdentityExchangePeerUseCase,
    private val getIdentityPeerDisplayName: GetIdentityPeerDisplayNameUseCase,
    private val resolveIncomingIdentityPeer: ResolveIncomingIdentityPeerUseCase,
    private val stagePendingRemoteIdentityChange: StagePendingRemoteIdentityChangeUseCase,
    private val applyIdentityPeerMerge: ApplyIdentityPeerMergeUseCase,
    private val updateIncomingIdentityPeerMetadata: UpdateIncomingIdentityPeerMetadataUseCase,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val startGroupMembership: StartGroupMembershipUseCase,
    private val inspectIncomingGroupMembership: InspectIncomingGroupMembershipUseCase,
    private val receiveIncomingGroupMembership: ReceiveIncomingGroupMembershipUseCase,
    private val discardSupersededMemberships: DiscardSupersededMembershipsUseCase,
    private val getMembershipHandshake: GetMembershipHandshakeUseCase,
    private val acceptGroupMembership: AcceptGroupMembershipUseCase,
    private val declineGroupMembership: DeclineGroupMembershipUseCase,
    private val receiveGroupMembershipReceipt: ReceiveGroupMembershipReceiptUseCase,
    private val receiveGroupMembershipDecline: ReceiveGroupMembershipDeclineUseCase,
    private val receiveGroupMembershipJoinRequest: ReceiveGroupMembershipJoinRequestUseCase,
    private val receiveGroupReadyAcknowledgement: ReceiveGroupReadyAcknowledgementUseCase,
    private val getContact: GetContactUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val authorizeIncomingGroupWelcome: AuthorizeIncomingGroupWelcomeUseCase,
    private val openIncomingGroupWelcome: OpenIncomingGroupWelcomeUseCase,
    private val persistIncomingGroupWelcome: PersistIncomingGroupWelcomeUseCase,
    private val resolveIncomingPeerContacts: ResolveIncomingPeerContactsUseCase,
    private val completeIncomingGroupWelcome: CompleteIncomingGroupWelcomeUseCase,
    private val sendGroupReadyAcknowledgement: SendGroupReadyAcknowledgementUseCase,
    private val authorizeIncomingGroupDeletion: AuthorizeIncomingGroupDeletionUseCase,
    private val authorizeIncomingGroupRemoval: AuthorizeIncomingGroupRemovalUseCase,
    private val authorizeIncomingGroupActivation: AuthorizeIncomingGroupActivationUseCase,
    private val applyIncomingGroupActivation: ApplyIncomingGroupActivationUseCase,
    private val resolveSigningIdentityContact: ResolveSigningIdentityContactUseCase,
    private val completeIncomingGroupRemoval: CompleteIncomingGroupRemovalUseCase,
    private val completeIncomingGroupDeletion: CompleteIncomingGroupDeletionUseCase,
    private val receiveGroupActivationAcknowledgement: ReceiveGroupActivationAcknowledgementUseCase,
    private val receiveGroupLeaveRequest: ReceiveGroupLeaveRequestUseCase,
    private val confirmGroupMembershipIdentity: ConfirmGroupMembershipIdentityUseCase,
    private val clearMembershipHandshake: ClearMembershipHandshakeUseCase,
    private val markMembershipRemoved: MarkMembershipRemovedUseCase,
    private val getGroupLeaveRequirementUseCase: GetGroupLeaveRequirementUseCase,
    private val getGroupCurrentEpochUseCase: GetGroupCurrentEpochUseCase,
    private val promoteGroupMemberUseCase: PromoteGroupMemberUseCase,
    private val removeGroupMemberUseCase: RemoveGroupMemberUseCase,
    private val transferGroupAdminAndLeaveUseCase: TransferGroupAdminAndLeaveUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val deleteGroupMembershipUseCase: DeleteGroupMembershipUseCase,
    private val stageRemoteIdentity: StageRemoteIdentityUseCase,
    private val acceptRemoteIdentityHandshake: AcceptRemoteIdentityHandshakeUseCase,
    private val establishMutualIdentity: EstablishMutualIdentityUseCase,
    private val ensureRemoteSigningIdentity: EnsureRemoteSigningIdentityUseCase,
    private val applyRemoteProfilePictureMetadata: ApplyRemoteProfilePictureMetadataUseCase,
    private val recordInvitation: RecordPendingInvitationUseCase,
    private val shouldRecordPendingInvitation: ShouldRecordPendingInvitationUseCase,
    private val handleInvitationResponse: HandleInvitationResponseUseCase,
    private val markInvitationTransportFailed: MarkInvitationTransportFailedUseCase,
    private val invalidatePendingInvitation: InvalidatePendingInvitationUseCase,
    private val blockContact: BlockContactUseCase,
    private val sendContactVerificationReceipt: SendIdentityVerificationReceiptUseCase,
    private val blocklistRepository: ContactBlocklistRepository,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository,
    private val conversationPort: ConversationPort
) {
    private val logger = SparrowLog.withTag("ConversationFlowHandler")

    /** One internal workflow handler sequences Identity, protocol delivery, mailbox and Chats. */
    suspend fun deletePeerConversation(conversationId: String): Result<Unit> =
        runCatching {
            val peerId = conversationPort.findPeerId(conversationId).getOrThrow() ?: return@runCatching
            revokePeerExchange(peerId)
            mailboxCapabilityLifecycle.revokeForContact(peerId).getOrThrow()
            conversationPort.deleteConversation(conversationId).getOrThrow()
        }

    /** A persisted contact block triggers revocation even if the block was recorded before startup. */
    suspend fun onContactBlocked(peerId: String): Result<Unit> =
        runCatching {
            if (!blocklistRepository.isBlocked(peerId)) return@runCatching
            revokePeerExchange(peerId)
        }

    /** Only orchestration chooses whether to decline, revoke, or close the Identity-owned exchange. */
    private suspend fun revokePeerExchange(peerId: String) {
        val exchange = getIdentityExchangeClosure(peerId).getOrThrow()
        if (exchange != null) {
            when (exchange.phase) {
                IdentityExchangeClosurePhase.INCOMING_PENDING ->
                    declineIdentityExchange(exchange.exchangeId).getOrThrow()
                IdentityExchangeClosurePhase.ACTIVE ->
                    revocationSender.enqueueOrResend(
                        peerId = exchange.peerId,
                        exchangeId = exchange.exchangeId,
                        inviteChallenge = exchange.inviteChallenge
                    ).getOrThrow()
                IdentityExchangeClosurePhase.TERMINAL -> Unit
                IdentityExchangeClosurePhase.ALREADY_CLOSED -> return
            }
            closeIdentityExchange(exchange.exchangeId, peerId).getOrThrow()
        }
    }

    suspend fun startDirectInvitation(peerId: String): Result<Unit> =
        runCatching {
            require(peerId.isNotBlank()) { "Peer ID must not be blank" }
            check(!blocklistRepository.isBlocked(peerId)) { "Blocked contacts cannot be invited" }
            // A peer can retain their original keys after deleting a local chat.
            // If the conversation is missing, an explicit contact selection may start a
            // fresh invitation even while an old authorization still appears active.
            // Revoke ONLY that old conversation authorization: remote identity/trust
            // records and the user's own keys are never reset.
            if (getIdentityPeerState(peerId).getOrThrow().hasEstablishedExchange) {
                if (conversationPort.findConversationId(peerId).getOrThrow() != null) {
                    return@runCatching
                }
                // Do not revoke still-valid authorization if the contact cannot
                // be reached by the established invitation routing policy.
                resolveContactInvitationRoutingId(peerId)
                revokePeerExchange(peerId)
            }
            val peerDisplayName = getIdentityPeerDisplayName(peerId)
            val senderLabel = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
            val exchange = startIdentityExchange(peerId, senderLabel).getOrThrow()
                ?: error("No new invitation was created; an existing exchange must be resolved first")
            // Only Invite persists user-facing contact metadata. The Identity exchange
            // stores remote cryptographic identity, never a local Contacts label.
            recordInvitation(
                exchange.toInvitationLifecycleRecord(peerDisplayName)
            ).getOrThrow()
        }

    /**
     * User-triggered reconnection from an EXISTING direct conversation. Ordinary
     * contact selection must keep its existing no-duplicate behavior; this action
     * deliberately closes a prior authorization before starting a fresh invite.
     * The contact, keys and local conversation/history are never deleted here.
     */
    suspend fun startExplicitReconnection(peerId: String): Result<Unit> =
        runCatching {
            require(peerId.isNotBlank()) { "Peer ID must not be blank" }
            check(!blocklistRepository.isBlocked(peerId)) { "Blocked contacts cannot be invited" }
            check(conversationPort.findConversationId(peerId).getOrThrow() != null) {
                "Reconnection requires an existing direct conversation"
            }
            check(getIdentityExchangeClosure(peerId).getOrThrow()?.phase != IdentityExchangeClosurePhase.INCOMING_PENDING) {
                "Review or decline the pending invitation before starting a reconnection"
            }
            // Preflight the SAME direct-invitation routing policy as the outbox
            // before revoking a still-authorized conversation. A contact without
            // a phone number can still reconnect when the other installation has
            // restored the ORIGINAL public signing identity from its backup.
            // An unrecognized replacement identity cannot claim that old route.
            resolveContactInvitationRoutingId(peerId)
            if (getIdentityPeerState(peerId).getOrThrow().hasEstablishedExchange) {
                revokePeerExchange(peerId)
            }
            startDirectInvitation(peerId).getOrThrow()
        }

    /**
     * Called ONLY after an actual authorization failure. A retained chat with
     * stale authorization must not cause startDirectInvitation's normal
     * already-open-conversation guard to swallow a needed reconnection.
     * A merely offline contact with valid authorization does not reach here.
     */
    suspend fun requestReauthorization(peerId: String): Result<Unit> =
        runCatching {
            require(peerId.isNotBlank())
            if (conversationPort.findConversationId(peerId).getOrThrow() != null &&
                getIdentityPeerState(peerId).getOrThrow().hasEstablishedExchange
            ) {
                startExplicitReconnection(peerId).getOrThrow()
            } else {
                startDirectInvitation(peerId).getOrThrow()
            }
        }

    /**
     * Restart-only reconciliation of a conversation which ALREADY exists and has
     * CURRENT mutual authorization. Never replays old acceptance, makes a new
     * conversation, or begins an identity exchange. The observer suppresses this
     * while a replacement identity is pending independent verification.
     */
    suspend fun recoverExistingAuthorizedConversation(peerId: String): Result<Unit> =
        runCatching {
            require(peerId.isNotBlank())
            if (conversationPort.findConversationId(peerId).getOrThrow() == null) return@runCatching
            if (!getIdentityPeerState(peerId).getOrThrow().hasEstablishedExchange) return@runCatching
            conversationPort.activateAuthorizedConversation(peerId).getOrThrow()
        }

    /** Manual setup selection is a cross-feature workflow, not a Contacts use case. */
    @Suppress("unused") // Retained manual identity setup entry point for navigation integration.
    suspend fun startManualIdentitySetup(peerId: String): Result<Unit> =
        runCatching {
            require(peerId.isNotBlank()) { "Peer ID must not be blank" }
            cancelIdentityExchange(peerId).getOrThrow()
            startManualIdentityExchange(peerId).getOrThrow()
        }

    suspend fun startGroupInvitations(
        groupId: String,
        peerIds: Set<String>
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            if (peerIds.isEmpty()) return@runCatching

            val title = conversationPort.getGroupTitle(groupId).getOrThrow()
            peerIds.forEach { peerId ->
                val started = startGroupMembership(groupId, title, peerId).getOrThrow()
                recordInvitation(
                    InvitationLifecycleRecord(
                        invitationId = started.sourceId,
                        payloadType = InvitationPayloadType.GROUP,
                        payloadId = started.groupId,
                        peerId = started.peerId,
                        direction = InvitationDirection.OUTGOING,
                        createdAtEpochMilliseconds = started.createdAtEpochMilliseconds,
                        expiresAtEpochMilliseconds = started.expiresAtEpochMilliseconds,
                        updatedAtEpochMilliseconds = started.createdAtEpochMilliseconds,
                        peerDisplayName = title
                    )
                ).getOrThrow()
                // Include fresh and re-added STAGED memberships in the persisted
                // verification/details projection immediately, before acceptance.
                conversationPort.refreshOwnedGroupVerification(groupId).getOrThrow()
            }
        }

    private suspend fun groupMemberDisplayName(contactId: String): String =
        getContact(contactId).getOrThrow()?.displayName?.trim()?.takeIf(String::isNotEmpty)
            ?: "Member"

    suspend fun getGroupLeaveRequirement(groupId: String): Result<GroupLeaveRequirement> =
        getGroupLeaveRequirementUseCase(groupId)

    suspend fun promoteGroupMember(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            val context = conversationPort.getGroupMembershipContext(groupId).getOrThrow()
            val result = promoteGroupMemberUseCase(groupId, contactId, context).getOrThrow()
            conversationPort
                .promoteGroupParticipant(
                    groupId = result.groupId,
                    peerId = result.contactId,
                    updatedAtEpochMilliseconds = result.updatedAtEpochMilliseconds
                ).getOrThrow()
            conversationPort.refreshOwnedGroupVerification(groupId).getOrThrow()
        }

    suspend fun removeGroupMember(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            val context = conversationPort.getGroupMembershipContext(groupId).getOrThrow()
            val result = removeGroupMemberUseCase(groupId, contactId, context).getOrThrow()
            conversationPort
                .removeGroupParticipant(
                    groupId = result.groupId,
                    peerId = result.contactId,
                    memberDisplayName = groupMemberDisplayName(result.contactId),
                    epoch = result.epoch,
                    eventId = result.eventId,
                    updatedAtEpochMilliseconds = result.updatedAtEpochMilliseconds,
                    memberLeft = result.reason == GroupMemberRemovalReason.LEFT
                ).getOrThrow()
            conversationPort.refreshOwnedGroupVerification(groupId).getOrThrow()
        }

    suspend fun transferGroupAdminAndLeave(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            val context = conversationPort.getGroupMembershipContext(groupId).getOrThrow()
            val ended = transferGroupAdminAndLeaveUseCase(groupId, contactId, context).getOrThrow()
            conversationPort.endLocalGroupMembership(
                ended.groupId,
                ended.referenceId,
                ended.epoch,
                ended.endedAtEpochMilliseconds
            ).getOrThrow()
        }

    suspend fun leaveGroup(groupId: String): Result<Unit> =
        runCatching {
            val context = conversationPort.getGroupMembershipContext(groupId).getOrThrow()
            val ended = leaveGroupUseCase(groupId, context).getOrThrow()
            conversationPort.endLocalGroupMembership(
                ended.groupId,
                ended.referenceId,
                ended.epoch,
                ended.endedAtEpochMilliseconds
            ).getOrThrow()
        }

    suspend fun deleteGroupConversation(groupId: String): Result<Unit> =
        runCatching {
            val context = conversationPort.getGroupMembershipContext(groupId).getOrThrow()
            conversationPort.deleteGroupAttachments(groupId).getOrThrow()
            val deletedAt = deleteGroupMembershipUseCase(groupId, context).getOrThrow()
            conversationPort.deleteLocalGroupConversation(groupId, deletedAt).getOrThrow()
        }

    suspend fun recoverAcceptedDirectInvitation(result: InvitationResult): Result<Unit> =
        runCatching {
            if (result.payloadType != InvitationPayloadType.DIRECT ||
                result.direction != InvitationDirection.INCOMING ||
                result.response != InvitationResponse.ACCEPTED
            ) {
                return@runCatching
            }

            val exchange = getIdentityExchangeClosure(result.peerId).getOrThrow()
                ?: return@runCatching
            if (exchange.exchangeId != result.invitationId ||
                exchange.phase == IdentityExchangeClosurePhase.TERMINAL ||
                exchange.phase == IdentityExchangeClosurePhase.ALREADY_CLOSED
            ) {
                return@runCatching
            }

            handleDirectInvitationResult(result)
        }

    /** Historical Invite results can outlive a removed or superseded membership handshake.
     * Do not reapply those results on every restart, or create a new group membership.
     * A newly accepted result still goes through onInvitationResult and remains an error
     * if its staged handshake is unexpectedly missing.
     */
    suspend fun recoverAcceptedGroupInvitation(result: InvitationResult): Result<Unit> =
        runCatching {
            if (result.payloadType != InvitationPayloadType.GROUP ||
                result.direction != InvitationDirection.INCOMING ||
                result.response != InvitationResponse.ACCEPTED
            ) {
                return@runCatching
            }
            if (getMembershipHandshake(result.invitationId).getOrThrow() == null) return@runCatching
            handleGroupInvitationResult(result)
        }

    suspend fun onInvitationResult(result: InvitationResult): Result<Unit> =
        runCatching {
            when (result.payloadType) {
                InvitationPayloadType.DIRECT -> handleDirectInvitationResult(result)
                InvitationPayloadType.GROUP -> handleGroupInvitationResult(result)
            }
        }

    suspend fun onIdentityResult(result: IdentityResult): Result<Unit> =
        runCatching {
            when (result.status) {
                IdentityResultStatus.INCOMING_EXCHANGE ->
                    handleIncomingDirectInvitation(
                        result = result,
                        peerDisplayName = getIdentityPeerDisplayName(result.peerId)
                    )

                IdentityResultStatus.ESTABLISHED -> {
                    if (result.direction == IdentityExchangeDirection.OUTGOING) {
                        handleInvitationResponse(
                            payloadType = InvitationPayloadType.DIRECT,
                            invitationId = result.exchangeId,
                            response = InvitationResponse.ACCEPTED
                        ).getOrThrow()
                    }
                    // Invitation acceptance, not the identity setup mode, authorizes the
                    // conversation. Release any previously queued messages in both modes.
                    conversationPort.activateAuthorizedConversation(result.peerId).getOrThrow()
                    if (identitySetupModeRepository.getMode() == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                        sendContactVerificationReceipt(result.peerId)
                            .onFailure { error ->
                                logger.warn(error) {
                                    "Could not queue contact verification receipt for ${result.peerId}"
                                }
                            }
                    }
                }

                IdentityResultStatus.REMOTE_DECLINED -> {
                    handleInvitationResponse(
                        payloadType = InvitationPayloadType.DIRECT,
                        invitationId = result.exchangeId,
                        response = InvitationResponse.DECLINED
                    ).getOrThrow()
                    conversationPort.discardPendingAuthorizationMessages(result.peerId).getOrThrow()
                }

                IdentityResultStatus.FAILED ->
                    markInvitationTransportFailed(
                        payloadType = InvitationPayloadType.DIRECT,
                        invitationId = result.exchangeId
                    ).getOrThrow()

                IdentityResultStatus.EXCHANGE_INVALIDATED ->
                    invalidatePendingInvitation(result.exchangeId).getOrThrow()
            }
        }

    private suspend fun receiveAuthorizationRevoked(
        context: IncomingPacketContext,
        packet: DirectChatAuthorizationRevokedPacket
    ): Result<Unit> =
        runCatching {
            val binding = getIdentityExchangeBinding(packet.invitationId).getOrThrow()
                ?: error("Invitation was not found: ${packet.invitationId}")
            // Verify the packet signature before distinguishing an old/replaced sender
            // from the key pinned to this invitation. A self-signed packet from a new
            // identity is NOT authorization to revoke the previous identity's chat.
            revocationProtocol.verifyPacket(packet).getOrThrow()
            when (decideIncomingAuthorizationRevocation(context, packet, binding)) {
                IncomingAuthorizationRevocationDecision.IGNORE_UNRECOGNIZED_SIGNER -> {
                    // The invitation can outlive a deleted chat / identity replacement.
                    // Acknowledge this packet as handled so the envelope is not retried
                    // and reported as a failure on every reconnect. No identity, trust,
                    // pending messages, or conversation is modified here.
                    logger.info {
                        "Ignoring authorization revocation with a signing key that does not match " +
                            "the stored invitation identity: invitationId=${packet.invitationId}"
                    }
                    return@runCatching
                }

                IncomingAuthorizationRevocationDecision.APPLY -> Unit
            }
            invalidateIdentityExchange(
                exchangeId = packet.invitationId,
                peerId = context.contactId,
                expectedChallenge = packet.inviteChallenge,
                expectedSigningPublicKey = packet.revokerSigningPublicKey,
                atEpochMilliseconds = context.receivedAtEpochMilliseconds
            ).getOrThrow()
        }

    suspend fun onIdentityPacket(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        when (packet) {
            is ContactInvitePacket -> handleIncomingIdentityExchangePacket(context, packet)
            is ContactInviteAcceptedPacket -> runCatching {
                invitationHandshakeProtocol.verifyAccepted(
                    packet = packet,
                    receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                ).getOrThrow()
                // An acceptance may arrive after its outgoing exchange has been
                // cancelled, replaced, or removed locally. It cannot establish a
                // conversation without the original exchange/challenge binding.
                // Verify the packet above, then acknowledge the unmatched response
                // as handled rather than retrying this envelope forever.
                // A failed binding lookup (e.g. database error) still propagates.
                if (getIdentityExchangeBinding(packet.invitationId).getOrThrow() == null) {
                    logger.warn {
                        "Ignoring unmatched invitation acceptance: invitationId=${packet.invitationId}, " +
                            "contactId=${context.contactId}"
                    }
                    return@runCatching
                }
                try {
                    receiveIdentityExchangeAccepted(
                        context,
                        IdentityExchangeAcceptance(
                            exchangeId = packet.invitationId,
                            acceptedAtEpochMilliseconds = packet.acceptedAtEpochMilliseconds,
                            inviteChallenge = packet.inviteChallenge,
                            responseChallenge = packet.responseChallenge,
                            inviterEncryptionPublicKey = packet.inviterEncryptionPublicKey,
                            inviterSigningPublicKey = packet.inviterSigningPublicKey,
                            responderEncryptionPublicKey = packet.responderEncryptionPublicKey,
                            responderSigningPublicKey = packet.responderSigningPublicKey,
                            autoSharesIdentity = packet.autoSharesIdentity
                        )
                    ).getOrThrow()
                } catch (review: IdentityAcceptanceRequiresReviewException) {
                    // This branch is reached only AFTER the accepted packet signature,
                    // local-key binding, challenge, invitation lifetime and outgoing
                    // stage were verified. The newly signed key is NOT proof of the
                    // original contact's identity; never accept or activate this chat.
                    check(review.peerId == context.contactId) {
                        "Identity review does not match the invitation contact"
                    }
                    stagePendingRemoteIdentityChange(
                        PendingRemoteIdentityChange(
                            peerId = review.peerId,
                            sourcePeerId = context.contactId,
                            invitationId = packet.invitationId,
                            proposedEncryptionPublicKey = packet.responderEncryptionPublicKey.copyOf(),
                            proposedSigningPublicKey = packet.responderSigningPublicKey.copyOf(),
                            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                            expiresAtEpochMilliseconds = review.invitationExpiresAtEpochMilliseconds
                        )
                    ).getOrThrow()
                    return@runCatching
                }
                // A verified acceptance creates the sender's chat immediately. The
                // Identity observer can be behind this packet, especially on restart.
                conversationPort.activateAuthorizedConversation(context.contactId).getOrThrow()
                handleInvitationResponse(
                    payloadType = InvitationPayloadType.DIRECT,
                    invitationId = packet.invitationId,
                    response = InvitationResponse.ACCEPTED
                ).getOrThrow()
                // The exchange validates the bound challenge, signing key and packet
                // signature before profile metadata may be persisted.
                applyPeerProfilePicture(context.contactId, packet.profilePicture)
            }
            is ContactInviteDeclinedPacket -> handleRemoteInvitationDecline(context, packet)
            is ContactReadyPacket -> runCatching {
                val binding = getIdentityExchangeBinding(packet.invitationId).getOrThrow()
                    ?: error("Exchange was not found: ${packet.invitationId}")
                check(binding.peerId == context.contactId) { "Ready sender does not match exchange" }
                invitationHandshakeProtocol.verifyReady(
                    packet = packet,
                    remoteSigningPublicKey = binding.remoteSigningPublicKey,
                    receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                ).getOrThrow()
                receiveIdentityReady(
                    context,
                    IdentityExchangeReady(
                        exchangeId = packet.invitationId,
                        responseChallenge = packet.responseChallenge,
                        acceptedResponderEncryptionPublicKey = packet.acceptedResponderEncryptionPublicKey,
                        acceptedResponderSigningPublicKey = packet.acceptedResponderSigningPublicKey,
                        senderEncryptionPublicKey = packet.senderEncryptionPublicKey,
                        senderSigningPublicKey = packet.senderSigningPublicKey
                    )
                ).getOrThrow()
            }
            is DirectChatAuthorizationRevokedPacket -> receiveAuthorizationRevoked(context, packet)
            is IdentityPacket ->
                receiveManualIdentity(context, packet).fold(
                    onSuccess = { mutual ->
                        if (mutual) {
                            // Explicit manual setup may precede an invitation. Never create a
                            // conversation or flush queued messages until it is accepted.
                            if (getIdentityPeerState(context.contactId).getOrThrow().hasEstablishedExchange) {
                                conversationPort.activateAuthorizedConversation(context.contactId).getOrThrow()
                            }
                            sendContactVerificationReceipt(context.contactId)
                        } else {
                            Result.success(Unit)
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            is IdentityAcknowledgementPacket ->
                receiveIdentityAcknowledgement(context, packet).fold(
                    onSuccess = { mutual ->
                        if (mutual) {
                            // Explicit manual setup may precede an invitation. Never create a
                            // conversation or flush queued messages until it is accepted.
                            if (getIdentityPeerState(context.contactId).getOrThrow().hasEstablishedExchange) {
                                conversationPort.activateAuthorizedConversation(context.contactId).getOrThrow()
                            }
                            sendContactVerificationReceipt(context.contactId)
                        } else {
                            Result.success(Unit)
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            else -> Result.failure(IllegalArgumentException("Unsupported identity packet: ${packet::class.simpleName}"))
        }

    private suspend fun handleRemoteInvitationDecline(
        context: IncomingPacketContext,
        packet: ContactInviteDeclinedPacket
    ): Result<Unit> = runCatching {
        invitationDeclineProtocol.verifyPacket(packet, context.receivedAtEpochMilliseconds).getOrThrow()
        recordRemoteIdentityDecline(
            exchangeId = packet.invitationId,
            peerId = context.contactId,
            inviteChallenge = packet.inviteChallenge,
            remoteSigningPublicKey = packet.declinerSigningPublicKey,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
        ).getOrThrow()
    }

    private suspend fun handleIncomingIdentityExchangePacket(
        context: IncomingPacketContext,
        packet: ContactInvitePacket
    ): Result<Unit> =
        runCatching {
            // Verify the signature and all time bounds BEFORE looking up or merging contacts.
            invitationRequestProtocol.verifyPacket(packet, context.receivedAtEpochMilliseconds).getOrThrow()
            val remotePhoneNumber =
                packet.displayName
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?.let { value -> phoneNumberNormalizer.normalize(value).getOrNull() }
            val resolution = try {
                resolveIncomingIdentityPeer(
                    resolvedPeerId = context.contactId,
                    remotePhoneNumber = remotePhoneNumber,
                    remoteEncryptionPublicKey = packet.encryptionPublicKey,
                    remoteSigningPublicKey = packet.signingPublicKey
                )
            } catch (conflict: RemoteIdentityReplacementRequiredException) {
                // The signature on this invitation authenticates the PROPOSED keys,
                // not ownership of the previous identity or of the claimed number.
                // Record it durably for later explicit review; never merge contacts,
                // update their phone/profile or authorize delivery in this path.
                stagePendingRemoteIdentityChange(
                    PendingRemoteIdentityChange(
                        peerId = conflict.peerId,
                        sourcePeerId = context.contactId,
                        invitationId = packet.invitationId,
                        proposedEncryptionPublicKey = packet.encryptionPublicKey.copyOf(),
                        proposedSigningPublicKey = packet.signingPublicKey.copyOf(),
                        receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                        expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds
                    )
                ).getOrThrow()
                return@runCatching
            }

            resolution.merges.forEach { merge ->
                reassignIdentityExchangePeer(
                    fromPeerId = merge.fromPeerId,
                    toPeerId = merge.toPeerId
                ).getOrThrow()
                applyIdentityPeerMerge(merge)
            }

            remotePhoneNumber?.let { phoneNumber ->
                updateIncomingIdentityPeerMetadata(
                    peerId = resolution.peerId,
                    phoneNumber = phoneNumber,
                    updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                )
            }

            receiveIdentityExchange(
                context = context.copy(contactId = resolution.peerId),
                offer = IdentityExchangeOffer(
                    exchangeId = packet.invitationId,
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                    inviteChallenge = packet.inviteChallenge,
                    encryptionPublicKey = packet.encryptionPublicKey,
                    signingPublicKey = packet.signingPublicKey,
                    autoSharesIdentity = packet.autoSharesIdentity
                ),
                wasKnownPeerAtReceive = resolution.wasKnownPeer
            ).getOrThrow()
            // Contact/profile side effects happen outside Identity only after its
            // cryptographic replay and binding checks have accepted this packet.
            applyPeerProfilePicture(resolution.peerId, packet.profilePicture)

            // Invite owns presentation metadata. Store it from the already-verified
            // packet instead of publishing the sender's label through IdentityResult.
            // The Identity result observer still restores this record after a restart
            // if the process dies between exchange persistence and Invite persistence.
            handleIncomingDirectInvitation(
                result = IdentityResult(
                    exchangeId = packet.invitationId,
                    peerId = resolution.peerId,
                    direction = IdentityExchangeDirection.INCOMING,
                    status = IdentityResultStatus.INCOMING_EXCHANGE,
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                    wasKnownPeerAtReceive = resolution.wasKnownPeer
                ),
                peerDisplayName =
                    getIdentityPeerDisplayName(resolution.peerId)
                        ?: packet.displayName?.trim()?.takeIf(String::isNotBlank),
                declineIfRejected = false
            )
        }

    suspend fun onMembershipPacket(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            when (packet) {
                is GroupCreatedPacket -> {
                    val authorization = authorizeIncomingGroupWelcome(
                        groupId = packet.groupId,
                        senderContactId = context.contactId,
                        packetId = packet.packetId,
                        epoch = packet.epoch
                    ).getOrThrow() ?: return@runCatching
                    // Contacts owns contact identity lookup. First welcome must use the
                    // accepted/pinned inviter identity; updates use Membership's prior epoch.
                    val firstWelcomeAuthority = if (authorization.isFirstWelcome) {
                        getRemoteIdentity(context.contactId).getOrThrow()
                            ?: error("Inviting group admin has no accepted Sparrow identity")
                    } else {
                        null
                    }
                    val persistedAt = maxOf(packet.createdAtEpochMilliseconds, context.receivedAtEpochMilliseconds)
                    val opened = openIncomingGroupWelcome(
                        packet = packet,
                        senderContactId = context.contactId,
                        isFirstWelcome = authorization.isFirstWelcome,
                        previousAuthorityEncryptionPublicKey =
                            firstWelcomeAuthority?.encryptionPublicKey
                                ?: authorization.priorAdminEncryptionPublicKey,
                        previousAuthoritySigningPublicKey =
                            firstWelcomeAuthority?.signingPublicKey
                                ?: authorization.priorAdminSigningPublicKey
                    ).getOrThrow()
                    conversationPort.recordIncomingGroupWelcomeRestart(
                        packet,
                        authorization.sourceInvitationId,
                        authorization.isFirstWelcome,
                        persistedAt
                    ).getOrThrow()
                    val contactIds = resolveIncomingPeerContacts(
                        candidates = packet.members.map { member ->
                            IncomingPeerContactCandidate(member.signingPublicKey, member.phoneNumber)
                        },
                        senderContactId = context.contactId,
                        senderSigningPublicKey = opened.authoritySigningPublicKey,
                        localSigningPublicKey = opened.localSigningPublicKey
                    ).getOrThrow()
                    val memberKeys = packet.members.mapIndexedNotNull { index, member ->
                        val contactId = contactIds[index] ?: return@mapIndexedNotNull null
                        GroupWelcomeMemberKey(
                            groupId = packet.groupId,
                            epoch = packet.epoch,
                            contactId = contactId,
                            encryptionPublicKey = member.encryptionPublicKey.copyOf(),
                            signingPublicKey = member.signingPublicKey.copyOf(),
                            role = member.role,
                            phoneNumber = member.phoneNumber
                        )
                    }
                    val continuingAdminId = if (opened.authorityLeft) {
                        packet.members.indices.firstNotNullOfOrNull { index ->
                            contactIds[index]?.takeIf {
                                packet.members[index].role.isGroupAdminRole()
                            }
                        }
                    } else {
                        null
                    }
                    persistIncomingGroupWelcome(
                        welcome = opened,
                        ownerContactId = continuingAdminId ?: context.contactId,
                        memberKeys = memberKeys,
                        receivedAtEpochMilliseconds = persistedAt
                    ).getOrThrow()
                    val previousIds = conversationPort.getCurrentGroupParticipantIds(packet.groupId).getOrThrow()
                    val displayNames = (previousIds + contactIds.filterNotNull()).distinct().associateWith { contactId ->
                        getContact(contactId).getOrThrow()?.displayName
                            ?.trim()?.takeIf(String::isNotEmpty) ?: "Member"
                    }
                    val removed = conversationPort.installIncomingGroupWelcome(
                        packet = packet,
                        previousSigningKeysByContactId = authorization.previousSigningKeysByContactId,
                        contactIdsByMember = contactIds,
                        contactDisplayNames = displayNames,
                        persistedAt = persistedAt
                    ).getOrThrow()
                    if (authorization.isFirstWelcome) {
                        val authority = requireNotNull(firstWelcomeAuthority)
                        establishMutualIdentity(
                            contactId = context.contactId,
                            encryptionPublicKey = authority.encryptionPublicKey,
                            signingPublicKey = authority.signingPublicKey
                        ).getOrThrow()
                    }
                    sendGroupReadyAcknowledgement(
                        groupId = packet.groupId,
                        epoch = packet.epoch,
                        welcomePacketId = packet.packetId,
                        recipientContactId = context.contactId
                    ).getOrThrow()
                    completeIncomingGroupWelcome(
                        groupId = packet.groupId,
                        senderContactId = context.contactId,
                        isFirstWelcome = authorization.isFirstWelcome,
                        removedContactIds = removed,
                        persistedAt = maxOf(packet.createdAtEpochMilliseconds, context.receivedAtEpochMilliseconds)
                    ).getOrThrow()
                }
                is GroupMemberActivatedPacket -> {
                    val owner = getRemoteIdentity(context.contactId).getOrThrow()
                        ?: error("Group owner has no Sparrow identity")
                    check(owner.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
                        "Group owner key exchange is not mutual"
                    }
                    val local = authorizeIncomingGroupActivation(
                        packet = packet,
                        ownerContactId = context.contactId,
                        ownerSigningPublicKey = owner.signingPublicKey,
                        transportMode = context.transportMode
                    ).getOrThrow()
                    val memberContactId = if (local) {
                        null
                    } else {
                        resolveSigningIdentityContact(
                            signingPublicKey = packet.member.signingPublicKey,
                            encryptionPublicKey = packet.member.encryptionPublicKey,
                            phoneNumber = packet.member.phoneNumber
                        ).getOrThrow()
                    }
                    val appliedLocal = applyIncomingGroupActivation(
                        packet = packet,
                        ownerContactId = context.contactId,
                        ownerSigningPublicKey = owner.signingPublicKey,
                        transportMode = context.transportMode,
                        memberContactId = memberContactId,
                        receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                    ).getOrThrow()
                    if (appliedLocal) {
                        conversationPort.onLocalGroupMemberActivated(packet.groupId).getOrThrow()
                    } else if (packet.activationRound == GroupMemberActivatedPacket.DISCOVERY_ROUND) {
                        conversationPort.recordRemoteGroupMemberAdded(
                            groupId = packet.groupId,
                            contactId = requireNotNull(memberContactId),
                            epoch = packet.epoch,
                            activationId = packet.activationId,
                            memberDisplayName = groupMemberDisplayName(requireNotNull(memberContactId)),
                            joinedAtEpochMilliseconds = packet.activatedAtEpochMilliseconds
                        ).getOrThrow()
                    } else if (packet.activationRound == GroupMemberActivatedPacket.FINAL_ROUND) {
                        conversationPort.onRemoteGroupMemberActivated(
                            groupId = packet.groupId,
                            contactId = requireNotNull(memberContactId),
                            role = packet.member.role,
                            epoch = packet.epoch,
                            activationId = packet.activationId,
                            memberDisplayName = groupMemberDisplayName(requireNotNull(memberContactId)),
                            joinedAtEpochMilliseconds = packet.activatedAtEpochMilliseconds
                        ).getOrThrow()
                    }
                }
                is GroupMemberRemovedPacket -> {
                    val pendingOwnerSigningPublicKey =
                        if (packet.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
                            getRemoteIdentity(context.contactId).getOrThrow()
                                ?.signingPublicKey
                                ?: error("Pending group membership owner identity was not found")
                        } else {
                            null
                        }
                    val applicable = authorizeIncomingGroupRemoval(
                        packet = packet,
                        senderContactId = context.contactId,
                        pendingOwnerSigningPublicKey = pendingOwnerSigningPublicKey
                    ).getOrThrow()
                    if (applicable) {
                        conversationPort.applyIncomingGroupRemoval(packet, context.contactId).getOrThrow()
                        completeIncomingGroupRemoval(packet).getOrThrow()
                    }
                }
                is GroupConversationDeletedPacket -> {
                    val ownerSigningPublicKey = getRemoteIdentity(context.contactId).getOrThrow()
                        ?.signingPublicKey
                        ?: error("Group owner identity was not found")
                    authorizeIncomingGroupDeletion(
                        packet = packet,
                        ownerContactId = context.contactId,
                        ownerSigningPublicKey = ownerSigningPublicKey
                    ).getOrThrow()
                    conversationPort.prepareIncomingGroupDeletion(packet.groupId).getOrThrow()
                    completeIncomingGroupDeletion(packet, context.contactId).getOrThrow()
                    conversationPort.finishIncomingGroupDeletion(
                        packet.groupId,
                        packet.deletedAtEpochMilliseconds
                    ).getOrThrow()
                }
                is GroupInvitePacket -> handleIncomingGroupInvite(context, packet)
                is GroupInviteReceivedPacket -> handleGroupInviteReceived(context, packet)
                is GroupInviteDeclinedPacket -> handleGroupInviteDeclined(context, packet)
                is GroupJoinRequestPacket -> handleGroupJoinRequest(context, packet)
                is GroupReadyAcknowledgementPacket -> {
                    // The owner discards old epoch keys on rotation. A delayed ACK
                    // for that old welcome is obsolete, not a missing current key.
                    // Do not project obsolete readiness into Chats either.
                    if (packet.epoch < getGroupCurrentEpochUseCase(packet.groupId).getOrThrow()) {
                        return@runCatching
                    }
                    receiveGroupReadyAcknowledgement(
                        memberContactId = context.contactId,
                        packet = packet,
                        receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                    ).getOrThrow()
                    if (packet.epoch != getGroupCurrentEpochUseCase(packet.groupId).getOrThrow()) {
                        return@runCatching
                    }
                    // The verified ready ACK has activated this membership. Install the Chats
                    // projection synchronously: group sends must not await a Flow observer.
                    val welcomePrefix = "group-welcome-${packet.groupId}-"
                    val epochSuffix = "-${packet.epoch}"
                    val sourceId = packet.welcomePacketId
                        .takeIf { it.startsWith(welcomePrefix) && it.endsWith(epochSuffix) }
                        ?.removePrefix(welcomePrefix)?.removeSuffix(epochSuffix)
                    val activeHandshake = sourceId?.let { getMembershipHandshake(it).getOrThrow() }
                    if (activeHandshake?.status == MembershipStatus.ACTIVE &&
                        activeHandshake.groupId == packet.groupId &&
                        activeHandshake.peerId == context.contactId
                    ) {
                        conversationPort.addGroupParticipant(
                            groupId = packet.groupId,
                            peerId = context.contactId,
                            memberDisplayName = groupMemberDisplayName(context.contactId),
                            epoch = packet.epoch,
                            joinedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                            eventId = activeHandshake.sourceId
                        ).getOrThrow()
                    }
                    conversationPort.refreshOwnedGroupVerification(packet.groupId).getOrThrow()
                    conversationPort.sendCurrentGroupMetadataTo(packet.groupId, context.contactId)
                }
                is GroupMemberActivationAcknowledgementPacket -> {
                    receiveGroupActivationAcknowledgement(
                        packet = packet,
                        acknowledgingContactId = context.contactId,
                        transportMode = context.transportMode
                    ).getOrThrow()
                }
                is GroupLeaveRequestPacket -> {
                    val groupContext = conversationPort.getGroupMembershipContext(packet.groupId).getOrThrow()
                    val removed = receiveGroupLeaveRequest(
                        memberContactId = context.contactId,
                        packet = packet,
                        context = groupContext
                    ).getOrThrow()
                    conversationPort.removeGroupParticipant(
                        groupId = removed.groupId,
                        peerId = removed.contactId,
                        memberDisplayName = groupMemberDisplayName(removed.contactId),
                        epoch = removed.epoch,
                        eventId = removed.eventId,
                        updatedAtEpochMilliseconds = removed.updatedAtEpochMilliseconds,
                        memberLeft = removed.reason == GroupMemberRemovalReason.LEFT
                    ).getOrThrow()
                    conversationPort.refreshOwnedGroupVerification(removed.groupId).getOrThrow()
                }
                else -> error("Unsupported membership packet: ${packet::class.simpleName}")
            }
        }

    suspend fun onMembershipResult(result: MembershipResult): Result<Unit> =
        runCatching {
            if (result.perspective != MembershipPerspective.OWNER) return@runCatching

            when (result.status) {
                MembershipStatus.IDENTITY_READY -> {
                    handleInvitationResponse(
                        payloadType = InvitationPayloadType.GROUP,
                        invitationId = result.sourceInvitationId,
                        response = InvitationResponse.ACCEPTED
                    ).getOrThrow()
                    // Recover legacy owner memberships left at IDENTITY_READY if the
                    // process stopped after the signed join request but before welcome.
                    val peerIdentity = requireNotNull(getRemoteIdentity(result.peerId).getOrThrow()) {
                        "Accepted group member identity was not found for welcome recovery"
                    }
                    check(peerIdentity.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
                        "Group member identity exchange is not mutual"
                    }
                    confirmGroupMembershipIdentity(
                        sourceId = result.sourceInvitationId,
                        updatedAtEpochMilliseconds = result.updatedAtEpochMilliseconds,
                        context = conversationPort.getGroupMembershipContext(result.groupId).getOrThrow(),
                        memberEncryptionPublicKey = peerIdentity.encryptionPublicKey,
                        memberSigningPublicKey = peerIdentity.signingPublicKey,
                        memberPhoneNumber = requireGroupMemberPhoneNumber(result.peerId)
                    ).getOrThrow()
                }

                MembershipStatus.WELCOME_SENT ->
                    handleInvitationResponse(
                        payloadType = InvitationPayloadType.GROUP,
                        invitationId = result.sourceInvitationId,
                        response = InvitationResponse.ACCEPTED
                    ).getOrThrow()

                MembershipStatus.ACTIVE ->
                    conversationPort
                        .addGroupParticipant(
                            groupId = result.groupId,
                            peerId = result.peerId,
                            memberDisplayName = groupMemberDisplayName(result.peerId),
                            epoch = getGroupCurrentEpochUseCase(result.groupId).getOrThrow(),
                            joinedAtEpochMilliseconds = result.updatedAtEpochMilliseconds,
                            eventId = result.sourceInvitationId
                        ).getOrThrow()

                else -> Unit
            }
        }

    private suspend fun handleDirectInvitationResult(result: InvitationResult) {
        when {
            result.direction == InvitationDirection.INCOMING &&
                result.response == InvitationResponse.ACCEPTED -> {
                acceptIdentityExchange(result.invitationId).getOrThrow()
                // Both modes create the receiver's chat and release any locally
                // queued messages as soon as the invitation is accepted.
                // Manual identity exchange only changes the encryption mode.
                conversationPort.activateAuthorizedConversation(result.peerId).getOrThrow()
            }

            result.direction == InvitationDirection.INCOMING &&
                result.response == InvitationResponse.DECLINED -> {
                declineIdentityExchange(result.invitationId).getOrThrow()
                if (result.action == InvitationResultAction.BLOCK_PEER) {
                    blockContact(result.peerId).getOrThrow()
                }
            }

            result.direction == InvitationDirection.OUTGOING &&
                result.response == InvitationResponse.DECLINED ->
                conversationPort.discardPendingAuthorizationMessages(result.peerId).getOrThrow()
        }
    }

    private suspend fun handleGroupInvitationResult(result: InvitationResult) {
        require(result.action == null) { "Group invitations do not support invitation actions" }
        if (result.direction != InvitationDirection.INCOMING) return

        when (result.response) {
            InvitationResponse.ACCEPTED -> {
                val handshake =
                    requireNotNull(getMembershipHandshake(result.invitationId).getOrThrow()) {
                        "Group membership handshake was not found"
                    }
                check(handshake.groupId == result.payloadId) { "Invitation belongs to the wrong group" }
                check(handshake.peerId == result.peerId) { "Invitation belongs to the wrong peer" }
                val encryptionKey = requireNotNull(handshake.ownerEncryptionPublicKey) {
                    "Group owner encryption identity was not staged"
                }
                val signingKey = requireNotNull(handshake.ownerSigningPublicKey) {
                    "Group owner signing identity was not staged"
                }
                if (handshake.status !in setOf(
                        MembershipStatus.STAGED,
                        MembershipStatus.JOIN_REQUEST_SENT,
                        MembershipStatus.WAITING_FOR_ACTIVATION,
                        MembershipStatus.ACTIVE
                    )
                ) {
                    return
                }
                // The receiver accepted the invite: publish its staged chat even if
                // joining the group is still waiting for transport or identity work.
                conversationPort.showAcceptedIncomingGroupConversation(result.payloadId).getOrThrow()
                when (handshake.status) {
                    MembershipStatus.STAGED -> {
                        acceptRemoteIdentityHandshake(
                            contactId = result.peerId,
                            encryptionPublicKey = encryptionKey,
                            signingPublicKey = signingKey
                        ).getOrThrow()
                        acceptGroupMembership(result.invitationId).getOrThrow()
                    }
                    MembershipStatus.JOIN_REQUEST_SENT,
                    MembershipStatus.WAITING_FOR_ACTIVATION,
                    MembershipStatus.ACTIVE -> Unit // An accepted invitation is replayable after restart.
                    else -> Unit // Exhausted by the active-status guard above.
                }
            }

            InvitationResponse.DECLINED -> {
                val handshake = getMembershipHandshake(result.invitationId).getOrThrow()
                declineGroupMembership(result.invitationId).getOrThrow()
                conversationPort
                    .discardPendingGroupConversation(
                        groupId = result.payloadId,
                        updatedAtEpochMilliseconds =
                            maxOf(
                                handshake?.createdAtEpochMilliseconds ?: 0L,
                                SystemClock.nowEpochMilliseconds()
                            )
                    ).getOrThrow()
            }
        }
    }

    private suspend fun handleIncomingDirectInvitation(
        result: IdentityResult,
        peerDisplayName: String?,
        declineIfRejected: Boolean = true
    ) {
        // An explicit invitation must be deliverable in both identity-setup modes.
        // Manual sharing controls how keys can also be exchanged; it must not
        // silently discard a verified invitation packet.
        val blocked = blocklistRepository.isBlocked(result.peerId)
        val blockUnknown = blocklistRepository.getBlockUnknownContactInvites()
        val unknown = result.wasKnownPeerAtReceive == false

        if (blocked || (blockUnknown && unknown)) {
            // Only the persisted Identity result observer declines a rejected
            // exchange. The packet path may race that observer; declining in
            // both places would try to close the same exchange twice.
            if (declineIfRejected) {
                declineIdentityExchange(result.exchangeId).getOrThrow()
            }
            return
        }

        recordInvitation(
            InvitationLifecycleRecord(
                invitationId = result.exchangeId,
                payloadType = InvitationPayloadType.DIRECT,
                payloadId = result.peerId,
                peerId = result.peerId,
                direction = InvitationDirection.INCOMING,
                createdAtEpochMilliseconds = result.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = result.expiresAtEpochMilliseconds,
                updatedAtEpochMilliseconds = maxOf(
                    result.createdAtEpochMilliseconds,
                    result.updatedAtEpochMilliseconds
                ),
                peerDisplayName = peerDisplayName
            )
        ).getOrThrow()
    }

    private suspend fun handleIncomingGroupInvite(
        context: IncomingPacketContext,
        packet: GroupInvitePacket
    ) {
        val offer = inspectIncomingGroupMembership(context.contactId, packet).getOrThrow()
        val record =
            InvitationLifecycleRecord(
                invitationId = offer.sourceId,
                payloadType = InvitationPayloadType.GROUP,
                payloadId = offer.groupId,
                peerId = offer.peerId,
                direction = InvitationDirection.INCOMING,
                createdAtEpochMilliseconds = offer.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = offer.expiresAtEpochMilliseconds,
                updatedAtEpochMilliseconds =
                    maxOf(offer.createdAtEpochMilliseconds, context.receivedAtEpochMilliseconds),
                peerDisplayName = offer.title
            )
        val shouldRecord = shouldRecordPendingInvitation(record).getOrThrow()

        if (!shouldRecord) {
            receiveIncomingGroupMembership(
                peerId = context.contactId,
                packet = packet,
                receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                shouldStage = false
            ).getOrThrow()
            applyPeerProfilePicture(context.contactId, packet.profilePicture)
            return
        }

        val canStage =
            conversationPort
                .stageIncomingGroupConversation(
                    groupId = offer.groupId,
                    title = offer.title,
                    createdAtEpochMilliseconds = offer.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                ).getOrThrow()
        if (!canStage) {
            receiveIncomingGroupMembership(
                peerId = context.contactId,
                packet = packet,
                receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                shouldStage = false
            ).getOrThrow()
            return
        }

        val identityChanged =
            stageRemoteIdentity(
                contactId = context.contactId,
                encryptionPublicKey = offer.ownerEncryptionPublicKey,
                signingPublicKey = offer.ownerSigningPublicKey
            ).getOrThrow()
        if (identityChanged) {
            discardSupersededMemberships(
                peerId = context.contactId,
                currentSourceId = offer.sourceId
            ).getOrThrow()
        }

        receiveIncomingGroupMembership(
            peerId = context.contactId,
            packet = packet,
            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
            shouldStage = true
        ).getOrThrow()
        recordInvitation(record).getOrThrow()
        applyPeerProfilePicture(context.contactId, packet.profilePicture)
    }

    private suspend fun handleGroupInviteReceived(
        context: IncomingPacketContext,
        packet: GroupInviteReceivedPacket
    ) {
        val proof = receiveGroupMembershipReceipt(context.contactId, packet).getOrThrow() ?: return
        ensureRemoteSigningIdentity(
            contactId = proof.peerId,
            signingPublicKey = proof.signingPublicKey
        ).getOrThrow()
    }

    private suspend fun handleGroupInviteDeclined(
        context: IncomingPacketContext,
        packet: GroupInviteDeclinedPacket
    ) {
        val decline = receiveGroupMembershipDecline(context.contactId, packet).getOrThrow() ?: return
        ensureRemoteSigningIdentity(
            contactId = decline.peerId,
            signingPublicKey = decline.signingPublicKey
        ).getOrThrow()

        when (decline.disposition) {
            MembershipDeclineDisposition.PENDING_HANDSHAKE -> {
                clearMembershipHandshake(decline.sourceId).getOrThrow()
                handleInvitationResponse(
                    payloadType = InvitationPayloadType.GROUP,
                    invitationId = decline.sourceId,
                    response = InvitationResponse.DECLINED
                ).getOrThrow()
            }

            MembershipDeclineDisposition.ACTIVE_MEMBER ->
                markMembershipRemoved(
                    sourceId = decline.sourceId,
                    updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                ).getOrThrow()
        }
    }

    private suspend fun handleGroupJoinRequest(
        context: IncomingPacketContext,
        packet: GroupJoinRequestPacket
    ) {
        val join = receiveGroupMembershipJoinRequest(context.contactId, packet).getOrThrow() ?: return
        if (join.alreadyAccepted) {
            ensureRemoteSigningIdentity(
                contactId = join.peerId,
                signingPublicKey = join.memberSigningPublicKey
            ).getOrThrow()
        } else {
            // receiveGroupMembershipJoinRequest has verified the join signature and
            // bound its challenge to the staged membership. The group join therefore
            // provides authenticated evidence of the member's remote identity.
            // Persist that evidence *before* establishing mutual identity: an existing
            // imported contact can have remoteIdentityPacketReceived = false, in which
            // case markMutual's compare-and-set rightly refuses the update. Do not
            // loosen that security check or mark unrelated manual chats mutual.
            stageRemoteIdentity(
                contactId = join.peerId,
                encryptionPublicKey = join.memberEncryptionPublicKey,
                signingPublicKey = join.memberSigningPublicKey
            ).getOrThrow()
            establishMutualIdentity(
                contactId = join.peerId,
                encryptionPublicKey = join.memberEncryptionPublicKey,
                signingPublicKey = join.memberSigningPublicKey
            ).getOrThrow()
            confirmGroupMembershipIdentity(
                sourceId = join.sourceId,
                updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                context = conversationPort.getGroupMembershipContext(join.groupId).getOrThrow(),
                memberEncryptionPublicKey = join.memberEncryptionPublicKey,
                memberSigningPublicKey = join.memberSigningPublicKey,
                memberPhoneNumber = requireGroupMemberPhoneNumber(join.peerId)
            ).getOrThrow()
        }
        applyPeerProfilePicture(context.contactId, packet.profilePicture)
    }

    /** An invitation supplies a genuine contact phone once; membership then retains it per epoch. */
    private suspend fun requireGroupMemberPhoneNumber(peerId: String): String =
        getContact(peerId).getOrThrow()?.preferredPhoneNumber?.value
            ?.trim()?.takeIf(String::isNotBlank)
            ?: error("Accepted group member has no real phone number")

    private suspend fun applyPeerProfilePicture(
        contactId: String,
        metadata: com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
    ) {
        applyRemoteProfilePictureMetadata(contactId, metadata)
            .onFailure { error ->
                logger.warn(error) { "Could not store profile picture for $contactId" }
            }
    }

    private fun IdentityExchange.toInvitationLifecycleRecord(peerDisplayName: String?): InvitationLifecycleRecord =
        InvitationLifecycleRecord(
            invitationId = exchangeId,
            payloadType = InvitationPayloadType.DIRECT,
            payloadId = peerId,
            peerId = peerId,
            direction =
                when (direction) {
                    IdentityExchangeDirection.INCOMING -> InvitationDirection.INCOMING
                    IdentityExchangeDirection.OUTGOING -> InvitationDirection.OUTGOING
                },
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            updatedAtEpochMilliseconds = maxOf(createdAtEpochMilliseconds, updatedAtEpochMilliseconds),
            peerDisplayName = peerDisplayName
        )
}
