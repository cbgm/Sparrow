package com.cbgm.sparrow.feature.conversationorchestration.domain.workflow

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentityResult
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentityResultType
import com.cbgm.sparrow.feature.identity.domain.model.DirectInvitationDirection
import com.cbgm.sparrow.feature.identity.domain.model.DirectInvitationRecord
import com.cbgm.sparrow.feature.identity.domain.usecase.AcceptRemoteIdentityHandshakeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ApplyRemoteProfilePictureMetadataUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.EnsureRemoteSigningIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.EstablishMutualIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StageRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.AcceptDirectInvitationUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.DeclineDirectInvitationUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.StartDirectInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationTransportFailedUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.RecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.ShouldRecordPendingInvitationUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalReason
import com.cbgm.sparrow.feature.membership.domain.model.MembershipDeclineDisposition
import com.cbgm.sparrow.feature.membership.domain.model.MembershipPerspective
import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipStatus
import com.cbgm.sparrow.feature.membership.domain.usecase.AcceptGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ClearMembershipHandshakeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ConfirmGroupMembershipIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DeclineGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DeleteGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.DiscardSupersededMembershipsUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.InspectIncomingGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.LeaveGroupUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.MarkMembershipRemovedUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.PromoteGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipDeclineUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipJoinRequestUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipReceiptUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveIncomingGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.RemoveGroupMemberUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.StartGroupMembershipUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.TransferGroupAdminAndLeaveUseCase

/**
 * The only place in conversation orchestration that sequences public feature use cases.
 *
 * Observers forward feature results here; they do not perform workflow decisions themselves.
 */
@Suppress("LongParameterList")
internal class ConversationFlowHandler(
    private val startDirectInvitationUseCase: StartDirectInvitationUseCase,
    private val acceptDirectInvitation: AcceptDirectInvitationUseCase,
    private val declineDirectInvitation: DeclineDirectInvitationUseCase,
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
    private val confirmGroupMembershipIdentity: ConfirmGroupMembershipIdentityUseCase,
    private val clearMembershipHandshake: ClearMembershipHandshakeUseCase,
    private val markMembershipRemoved: MarkMembershipRemovedUseCase,
    private val getGroupLeaveRequirementUseCase: GetGroupLeaveRequirementUseCase,
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
    private val blockContact: BlockContactUseCase,
    private val blocklistRepository: ContactBlocklistRepository,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository,
    private val conversationPort: ConversationPort
) {
    private val logger = SparrowLog.withTag("ConversationFlowHandler")

    suspend fun startDirectInvitation(peerId: String): Result<Unit> =
        runCatching {
            val record = startDirectInvitationUseCase(peerId).getOrThrow() ?: return@runCatching
            recordInvitation(record.toInvitationLifecycleRecord()).getOrThrow()
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
            }
        }

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
                    epoch = result.epoch,
                    eventId = result.eventId,
                    updatedAtEpochMilliseconds = result.updatedAtEpochMilliseconds,
                    memberLeft = result.reason == GroupMemberRemovalReason.LEFT
                ).getOrThrow()
        }

    suspend fun transferGroupAdminAndLeave(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            val context = conversationPort.getGroupMembershipContext(groupId).getOrThrow()
            transferGroupAdminAndLeaveUseCase(groupId, contactId, context).getOrThrow()
        }

    suspend fun leaveGroup(groupId: String): Result<Unit> =
        runCatching {
            val context = conversationPort.getGroupMembershipContext(groupId).getOrThrow()
            leaveGroupUseCase(groupId, context).getOrThrow()
        }

    suspend fun deleteGroupConversation(groupId: String): Result<Unit> =
        runCatching {
            val context = conversationPort.getGroupMembershipContext(groupId).getOrThrow()
            conversationPort.deleteGroupAttachments(groupId).getOrThrow()
            deleteGroupMembershipUseCase(groupId, context).getOrThrow()
        }

    suspend fun onInvitationResult(result: InvitationResult): Result<Unit> =
        runCatching {
            when (result.payloadType) {
                InvitationPayloadType.DIRECT -> handleDirectInvitationResult(result)
                InvitationPayloadType.GROUP -> handleGroupInvitationResult(result)
            }
        }

    suspend fun onDirectIdentityResult(result: DirectIdentityResult): Result<Unit> =
        runCatching {
            when (result.type) {
                DirectIdentityResultType.INCOMING_INVITATION ->
                    handleIncomingDirectInvitation(result)

                DirectIdentityResultType.AUTHORIZED -> {
                    if (result.direction == DirectInvitationDirection.OUTGOING) {
                        handleInvitationResponse(
                            payloadType = InvitationPayloadType.DIRECT,
                            invitationId = result.invitationId,
                            response = InvitationResponse.ACCEPTED
                        ).getOrThrow()
                    }
                    conversationPort.activateAuthorizedConversation(result.contactId).getOrThrow()
                }

                DirectIdentityResultType.REMOTE_DECLINED -> {
                    handleInvitationResponse(
                        payloadType = InvitationPayloadType.DIRECT,
                        invitationId = result.invitationId,
                        response = InvitationResponse.DECLINED
                    ).getOrThrow()
                    conversationPort.discardPendingAuthorizationMessages(result.contactId).getOrThrow()
                }

                DirectIdentityResultType.FAILED ->
                    markInvitationTransportFailed(
                        payloadType = InvitationPayloadType.DIRECT,
                        invitationId = result.invitationId
                    ).getOrThrow()

                DirectIdentityResultType.AUTHORIZATION_REVOKED -> Unit
            }
        }

    suspend fun onMembershipPacket(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            when (packet) {
                is GroupInvitePacket -> handleIncomingGroupInvite(context, packet)
                is GroupInviteReceivedPacket -> handleGroupInviteReceived(context, packet)
                is GroupInviteDeclinedPacket -> handleGroupInviteDeclined(context, packet)
                is GroupJoinRequestPacket -> handleGroupJoinRequest(context, packet)
                else -> error("Unsupported membership packet: ${packet::class.simpleName}")
            }
        }

    suspend fun onMembershipResult(result: MembershipResult): Result<Unit> =
        runCatching {
            if (result.perspective != MembershipPerspective.OWNER) return@runCatching

            when (result.status) {
                MembershipStatus.IDENTITY_READY ->
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
                            joinedAtEpochMilliseconds = result.updatedAtEpochMilliseconds,
                            eventId = result.sourceInvitationId
                        ).getOrThrow()

                else -> Unit
            }
        }

    private suspend fun handleDirectInvitationResult(result: InvitationResult) {
        when {
            result.direction == InvitationDirection.INCOMING &&
                result.response == InvitationResponse.ACCEPTED ->
                acceptDirectInvitation(result.invitationId).getOrThrow()

            result.direction == InvitationDirection.INCOMING &&
                result.response == InvitationResponse.DECLINED -> {
                declineDirectInvitation(result.invitationId).getOrThrow()
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
                acceptRemoteIdentityHandshake(
                    contactId = result.peerId,
                    encryptionPublicKey = encryptionKey,
                    signingPublicKey = signingKey
                ).getOrThrow()
                acceptGroupMembership(result.invitationId).getOrThrow()
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

    private suspend fun handleIncomingDirectInvitation(result: DirectIdentityResult) {
        val enabled = identitySetupModeRepository.getMode() == DirectIdentitySetupMode.AUTOMATIC_INVITATION
        val blocked = blocklistRepository.isBlocked(result.contactId)
        val blockUnknown = blocklistRepository.getBlockUnknownContactInvites()
        val unknown = result.wasKnownPeerAtReceive == false

        if (!enabled || blocked || (blockUnknown && unknown)) {
            declineDirectInvitation(result.invitationId).getOrThrow()
            return
        }

        recordInvitation(
            InvitationLifecycleRecord(
                invitationId = result.invitationId,
                payloadType = InvitationPayloadType.DIRECT,
                payloadId = result.contactId,
                peerId = result.contactId,
                direction = InvitationDirection.INCOMING,
                createdAtEpochMilliseconds = result.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = result.expiresAtEpochMilliseconds,
                updatedAtEpochMilliseconds = maxOf(
                    result.createdAtEpochMilliseconds,
                    result.updatedAtEpochMilliseconds
                ),
                peerDisplayName = result.peerDisplayName
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
            applyGroupProfilePicture(context.contactId, packet.profilePicture)
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
        applyGroupProfilePicture(context.contactId, packet.profilePicture)
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
            establishMutualIdentity(
                contactId = join.peerId,
                encryptionPublicKey = join.memberEncryptionPublicKey,
                signingPublicKey = join.memberSigningPublicKey
            ).getOrThrow()
            confirmGroupMembershipIdentity(
                sourceId = join.sourceId,
                updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
            ).getOrThrow()
        }
        applyGroupProfilePicture(context.contactId, packet.profilePicture)
    }

    private suspend fun applyGroupProfilePicture(
        contactId: String,
        metadata: com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
    ) {
        applyRemoteProfilePictureMetadata(contactId, metadata)
            .onFailure { error ->
                logger.warn(error) { "Could not store profile picture for $contactId" }
            }
    }

    private fun DirectInvitationRecord.toInvitationLifecycleRecord(): InvitationLifecycleRecord =
        InvitationLifecycleRecord(
            invitationId = invitationId,
            payloadType = InvitationPayloadType.DIRECT,
            payloadId = contactId,
            peerId = contactId,
            direction =
                when (direction) {
                    DirectInvitationDirection.INCOMING -> InvitationDirection.INCOMING
                    DirectInvitationDirection.OUTGOING -> InvitationDirection.OUTGOING
                },
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            updatedAtEpochMilliseconds = maxOf(createdAtEpochMilliseconds, updatedAtEpochMilliseconds),
            peerDisplayName = peerDisplayName,
            peerSecondaryText = peerSecondaryText
        )
}
