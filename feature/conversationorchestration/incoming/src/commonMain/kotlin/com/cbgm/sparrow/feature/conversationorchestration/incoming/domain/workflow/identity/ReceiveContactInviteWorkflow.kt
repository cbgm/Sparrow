package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity

import com.cbgm.sparrow.core.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerResolution
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.ApplyIdentityPeerMergeUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.GetIdentityPeerDisplayNameUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.UpdateIncomingIdentityPeerMetadataUseCase
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.RemoteIdentityReplacementRequiredException
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.ResolveIncomingIdentityPeerUseCase
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.ApplyPeerProfilePictureUseCase
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.result.RecordIncomingDirectInvitationWorkflow
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeDirection
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeOffer
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResultStatus
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.usecase.ReassignIdentityExchangePeerUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StagePendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.protocol.invitation.ContactInvitationRequestProtocol
import com.cbgm.sparrow.protocol.packet.ContactInvitePacket
import kotlin.reflect.KClass

/**
 * VerifyInvitation -> ResolvePeer -> MergePeers -> [UpdatePeerPhoneNumber] -> ReceiveExchange -> ApplyProfilePicture
 * -> ResolveDisplayName -> RecordInvitation
 *
 * Signature and time bounds are verified BEFORE contacts are looked up or merged. Keys that differ from a
 * known contact's are proof of the proposed keys only: they become an identity review, never a merge.
 * The Identity result observer restores the inbox record after a restart; it alone declines rejected senders.
 */
@Suppress("LongParameterList")
class ReceiveContactInviteWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val invitationRequestProtocol: ContactInvitationRequestProtocol,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val resolveIncomingIdentityPeer: ResolveIncomingIdentityPeerUseCase,
    private val blocklistRepository: ContactBlocklistRepository,
    private val stagePendingRemoteIdentityChange: StagePendingRemoteIdentityChangeUseCase,
    private val reassignIdentityExchangePeer: ReassignIdentityExchangePeerUseCase,
    private val applyIdentityPeerMerge: ApplyIdentityPeerMergeUseCase,
    private val updateIncomingIdentityPeerMetadata: UpdateIncomingIdentityPeerMetadataUseCase,
    private val receiveIdentityExchange: ReceiveIdentityExchangeUseCase,
    private val applyPeerProfilePicture: ApplyPeerProfilePictureUseCase,
    private val getIdentityPeerDisplayName: GetIdentityPeerDisplayNameUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveContactInvite,
        ReceiveContactInviteWorkflow.Step,
        ReceiveContactInviteWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveContactInvite> = IncomingCommand.ReceiveContactInvite::class

    override fun firstStep(command: IncomingCommand.ReceiveContactInvite): Step = Step.VerifyInvitation(command)

    @Suppress("LongMethod")
    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.VerifyInvitation -> {
                invitationRequestProtocol.verifyPacket(packet, context.receivedAtEpochMilliseconds).getOrThrow()
                Outcome.InvitationVerified(
                    packet.displayName?.trim()?.takeIf(String::isNotBlank)?.let { value ->
                        phoneNumberNormalizer.normalize(value).getOrNull()
                    }
                )
            }
            is Step.ResolvePeer ->
                try {
                    Outcome.PeerResolved(
                        resolveIncomingIdentityPeer(
                            resolvedPeerId = context.contactId,
                            remotePhoneNumber = step.remotePhoneNumber,
                            remoteEncryptionPublicKey = packet.encryptionPublicKey,
                            remoteSigningPublicKey = packet.signingPublicKey
                        ),
                        step.remotePhoneNumber
                    )
                } catch (conflict: RemoteIdentityReplacementRequiredException) {
                    Outcome.ReplacementRequired(conflict.peerId)
                }
            is Step.StageReplacementReview -> {
                if (!blocklistRepository.isBlocked(step.peerId)) {
                    stagePendingRemoteIdentityChange(
                        PendingRemoteIdentityChange(
                            peerId = step.peerId,
                            sourcePeerId = context.contactId,
                            invitationId = packet.invitationId,
                            proposedEncryptionPublicKey = packet.encryptionPublicKey.copyOf(),
                            proposedSigningPublicKey = packet.signingPublicKey.copyOf(),
                            receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                            expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                            originalInviteChallenge = packet.inviteChallenge.copyOf(),
                            originalInviteCreatedAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                            originalInviteAutoSharesIdentity = packet.autoSharesIdentity
                        )
                    ).getOrThrow()
                }
                Outcome.Done
            }
            is Step.MergePeers -> {
                step.resolution.merges.forEach { merge ->
                    reassignIdentityExchangePeer(merge.fromPeerId, merge.toPeerId).getOrThrow()
                    applyIdentityPeerMerge(merge)
                }
                Outcome.PeersMerged(step.resolution, step.remotePhoneNumber)
            }
            is Step.UpdatePeerPhoneNumber -> {
                updateIncomingIdentityPeerMetadata(
                    step.resolution.peerId,
                    step.phoneNumber,
                    context.receivedAtEpochMilliseconds
                )
                Outcome.PeerUpdated(step.resolution)
            }
            is Step.ReceiveExchange -> {
                receiveIdentityExchange(
                    context = context.copy(contactId = step.resolution.peerId),
                    offer =
                        IdentityExchangeOffer(
                            exchangeId = packet.invitationId,
                            createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                            expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                            inviteChallenge = packet.inviteChallenge,
                            encryptionPublicKey = packet.encryptionPublicKey,
                            signingPublicKey = packet.signingPublicKey,
                            autoSharesIdentity = packet.autoSharesIdentity
                        ),
                    wasKnownPeerAtReceive = step.resolution.wasKnownPeer
                ).getOrThrow()
                Outcome.ExchangeReceived(step.resolution)
            }
            is Step.ApplyProfilePicture -> {
                applyPeerProfilePicture(step.resolution.peerId, packet.profilePicture)
                Outcome.ProfilePictureApplied(step.resolution)
            }
            is Step.ResolveDisplayName ->
                Outcome.DisplayNameResolved(
                    step.resolution,
                    getIdentityPeerDisplayName(step.resolution.peerId) ?: packet.displayName?.trim()?.takeIf(
                        String::isNotBlank
                    )
                )
            is Step.RecordInvitation -> {
                engine.run(
                    LifecycleCommand.RecordIncomingDirectInvitation(
                        step.command.toIdentityResult(step.resolution),
                        step.peerDisplayName,
                        declineIfRejected = false
                    )
                ).getOrThrow()
                Outcome.Done
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.InvitationVerified -> Transition.Next(Step.ResolvePeer(outcome.remotePhoneNumber, step.command))
            is Outcome.ReplacementRequired -> Transition.Next(Step.StageReplacementReview(outcome.peerId, step.command))
            is Outcome.PeerResolved ->
                Transition.Next(Step.MergePeers(outcome.resolution, outcome.remotePhoneNumber, step.command))
            is Outcome.PeersMerged ->
                outcome.remotePhoneNumber
                    ?.let { phoneNumber ->
                        Transition.Next(Step.UpdatePeerPhoneNumber(outcome.resolution, phoneNumber, step.command))
                    }
                    ?: Transition.Next(Step.ReceiveExchange(outcome.resolution, step.command))
            is Outcome.PeerUpdated -> Transition.Next(Step.ReceiveExchange(outcome.resolution, step.command))
            is Outcome.ExchangeReceived -> Transition.Next(Step.ApplyProfilePicture(outcome.resolution, step.command))
            is Outcome.ProfilePictureApplied ->
                Transition.Next(Step.ResolveDisplayName(outcome.resolution, step.command))
            is Outcome.DisplayNameResolved ->
                Transition.Next(Step.RecordInvitation(outcome.resolution, outcome.peerDisplayName, step.command))
            Outcome.Done -> Transition.Complete()
        }

    private fun IncomingCommand.ReceiveContactInvite.toIdentityResult(
        resolution: IdentityPeerResolution
    ): IdentityResult =
        IdentityResult(
            exchangeId = packet.invitationId,
            peerId = resolution.peerId,
            direction = IdentityExchangeDirection.INCOMING,
            status = IdentityResultStatus.INCOMING_EXCHANGE,
            createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
            expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
            updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
            wasKnownPeerAtReceive = resolution.wasKnownPeer
        )

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveContactInvite

        data class VerifyInvitation(
            override val command: IncomingCommand.ReceiveContactInvite
        ) : Step

        data class ResolvePeer(
            val remotePhoneNumber: String?,
            override val command: IncomingCommand.ReceiveContactInvite
        ) : Step

        data class StageReplacementReview(
            val peerId: String,
            override val command: IncomingCommand.ReceiveContactInvite
        ) : Step

        data class MergePeers(
            val resolution: IdentityPeerResolution,
            val remotePhoneNumber: String?,
            override val command: IncomingCommand.ReceiveContactInvite
        ) : Step

        data class UpdatePeerPhoneNumber(
            val resolution: IdentityPeerResolution,
            val phoneNumber: String,
            override val command: IncomingCommand.ReceiveContactInvite
        ) : Step

        data class ReceiveExchange(
            val resolution: IdentityPeerResolution,
            override val command: IncomingCommand.ReceiveContactInvite
        ) : Step

        data class ApplyProfilePicture(
            val resolution: IdentityPeerResolution,
            override val command: IncomingCommand.ReceiveContactInvite
        ) : Step

        data class ResolveDisplayName(
            val resolution: IdentityPeerResolution,
            override val command: IncomingCommand.ReceiveContactInvite
        ) : Step

        data class RecordInvitation(
            val resolution: IdentityPeerResolution,
            val peerDisplayName: String?,
            override val command: IncomingCommand.ReceiveContactInvite
        ) : Step
    }

    sealed interface Outcome {
        data class InvitationVerified(
            val remotePhoneNumber: String?
        ) : Outcome

        data class PeerResolved(
            val resolution: IdentityPeerResolution,
            val remotePhoneNumber: String?
        ) : Outcome

        /** The keys differ from those of an existing contact; the user must review the change. */
        data class ReplacementRequired(
            val peerId: String
        ) : Outcome

        data class PeersMerged(
            val resolution: IdentityPeerResolution,
            val remotePhoneNumber: String?
        ) : Outcome

        data class PeerUpdated(
            val resolution: IdentityPeerResolution
        ) : Outcome

        data class ExchangeReceived(
            val resolution: IdentityPeerResolution
        ) : Outcome

        data class ProfilePictureApplied(
            val resolution: IdentityPeerResolution
        ) : Outcome

        data class DisplayNameResolved(
            val resolution: IdentityPeerResolution,
            val peerDisplayName: String?
        ) : Outcome

        data object Done : Outcome
    }
}
