package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.feature.contacts.domain.model.IncomingPeerContactCandidate
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveIncomingPeerContactsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupIncomingConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.FindMemberDisplayNameUseCase
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.usecase.EstablishMutualIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupIncomingWelcomeAuthorization
import com.cbgm.sparrow.feature.membership.domain.model.GroupWelcomeMemberKey
import com.cbgm.sparrow.feature.membership.domain.model.OpenedIncomingGroupWelcome
import com.cbgm.sparrow.feature.membership.domain.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.CompleteIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.OpenIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.PersistIncomingGroupWelcomeUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.SendGroupReadyAcknowledgementUseCase
import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket
import kotlin.reflect.KClass

/**
 * AuthorizeWelcome -> LoadAuthority -> OpenWelcome -> RecordRestart -> ResolveMemberContacts -> PersistWelcome
 * -> ResolveMemberNames -> InstallWelcome -> [EstablishMutualIdentity] -> SendReadyAcknowledgement -> CompleteWelcome
 *
 * The first welcome must come from the accepted, pinned inviter identity; later welcomes are checked against
 * Membership's prior epoch. Contacts are resolved only after Membership verified and decrypted the welcome.
 */
@Suppress("LongParameterList")
class ReceiveGroupWelcomeWorkflow internal constructor(
    private val authorizeIncomingGroupWelcome: AuthorizeIncomingGroupWelcomeUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val openIncomingGroupWelcome: OpenIncomingGroupWelcomeUseCase,
    private val incomingConversationRepository: GroupIncomingConversationRepository,
    private val resolveIncomingPeerContacts: ResolveIncomingPeerContactsUseCase,
    private val persistIncomingGroupWelcome: PersistIncomingGroupWelcomeUseCase,
    private val findMemberDisplayName: FindMemberDisplayNameUseCase,
    private val establishMutualIdentity: EstablishMutualIdentityUseCase,
    private val sendGroupReadyAcknowledgement: SendGroupReadyAcknowledgementUseCase,
    private val completeIncomingGroupWelcome: CompleteIncomingGroupWelcomeUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupWelcome,
        ReceiveGroupWelcomeWorkflow.Step,
        ReceiveGroupWelcomeWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupWelcome> = IncomingCommand.ReceiveGroupWelcome::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupWelcome): Step = Step.AuthorizeWelcome(command)

    @Suppress("LongMethod")
    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.AuthorizeWelcome ->
                authorizeIncomingGroupWelcome(
                    packet.groupId,
                    context.contactId,
                    packet.packetId,
                    packet.epoch
                ).getOrThrow()
                    ?.let { authorization ->
                        Outcome.Advanced(
                            Welcome(
                                authorization = authorization,
                                persistedAt =
                                    maxOf(packet.createdAtEpochMilliseconds, context.receivedAtEpochMilliseconds)
                            )
                        )
                    }
                    ?: Outcome.NotApplicable
            is Step.LoadAuthority -> {
                val authority =
                    if (step.welcome.authorization.isFirstWelcome) {
                        getRemoteIdentity(context.contactId).getOrThrow() ?: error(
                            "Inviting group admin has no accepted Sparrow identity"
                        )
                    } else {
                        null
                    }
                Outcome.Advanced(step.welcome.copy(firstWelcomeAuthority = authority))
            }
            is Step.OpenWelcome -> {
                val welcome = step.welcome
                val opened =
                    openIncomingGroupWelcome(
                        packet = packet,
                        senderContactId = context.contactId,
                        isFirstWelcome = welcome.authorization.isFirstWelcome,
                        previousAuthorityEncryptionPublicKey =
                            welcome.firstWelcomeAuthority?.encryptionPublicKey ?: welcome.authorization.priorAdminEncryptionPublicKey,
                        previousAuthoritySigningPublicKey =
                            welcome.firstWelcomeAuthority?.signingPublicKey ?: welcome.authorization.priorAdminSigningPublicKey
                    ).getOrThrow()
                Outcome.Advanced(welcome.copy(opened = opened))
            }
            is Step.RecordRestart -> {
                incomingConversationRepository.recordWelcomeRestart(
                    packet = packet,
                    invitationId = step.welcome.authorization.sourceInvitationId,
                    isFirstWelcome = step.welcome.authorization.isFirstWelcome,
                    persistedAt = step.welcome.persistedAt
                ).getOrThrow()
                Outcome.Advanced(step.welcome)
            }
            is Step.ResolveMemberContacts -> {
                val opened = step.welcome.requireOpened()
                val contactIds =
                    resolveIncomingPeerContacts(
                        candidates =
                            packet.members.map { member -> IncomingPeerContactCandidate(member.signingPublicKey, member.phoneNumber) },
                        senderContactId = context.contactId,
                        senderSigningPublicKey = opened.authoritySigningPublicKey,
                        localSigningPublicKey = opened.localSigningPublicKey
                    ).getOrThrow()
                Outcome.Advanced(step.welcome.copy(contactIdsByMember = contactIds))
            }
            is Step.PersistWelcome -> {
                persistIncomingGroupWelcome(
                    welcome = step.welcome.requireOpened(),
                    ownerContactId = continuingAdminId(packet, step.welcome) ?: context.contactId,
                    memberKeys = memberKeys(packet, step.welcome.contactIdsByMember),
                    receivedAtEpochMilliseconds = step.welcome.persistedAt
                ).getOrThrow()
                Outcome.Advanced(step.welcome)
            }
            is Step.ResolveMemberNames -> {
                val previousIds = incomingConversationRepository.getParticipantIds(packet.groupId).getOrThrow()
                val names =
                    (previousIds + step.welcome.contactIdsByMember.filterNotNull())
                        .distinct()
                        .associateWith { contactId -> findMemberDisplayName(contactId).getOrThrow() }
                Outcome.Advanced(step.welcome.copy(memberDisplayNames = names))
            }
            is Step.InstallWelcome -> {
                val removed =
                    incomingConversationRepository.installWelcome(
                        packet = packet,
                        previousSigningKeysByContactId = step.welcome.authorization.previousSigningKeysByContactId,
                        contactIdsByMember = step.welcome.contactIdsByMember,
                        contactDisplayNames = step.welcome.memberDisplayNames,
                        persistedAt = step.welcome.persistedAt
                    ).getOrThrow()
                Outcome.Advanced(step.welcome.copy(removedContactIds = removed))
            }
            is Step.EstablishMutualIdentity -> {
                val authority = requireNotNull(step.welcome.firstWelcomeAuthority)
                establishMutualIdentity(
                    context.contactId,
                    authority.encryptionPublicKey,
                    authority.signingPublicKey
                ).getOrThrow()
                Outcome.Advanced(step.welcome)
            }
            is Step.SendReadyAcknowledgement -> {
                sendGroupReadyAcknowledgement(
                    packet.groupId,
                    packet.epoch,
                    packet.packetId,
                    context.contactId
                ).getOrThrow()
                Outcome.Advanced(step.welcome)
            }
            is Step.CompleteWelcome -> {
                completeIncomingGroupWelcome(
                    groupId = packet.groupId,
                    senderContactId = context.contactId,
                    isFirstWelcome = step.welcome.authorization.isFirstWelcome,
                    removedContactIds = step.welcome.removedContactIds,
                    persistedAt = step.welcome.persistedAt
                ).getOrThrow()
                Outcome.Completed
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NotApplicable,
            Outcome.Completed -> Transition.Complete()
            is Outcome.Advanced -> Transition.Next(nextStep(step, outcome.welcome))
        }

    private fun nextStep(step: Step, welcome: Welcome): Step =
        when (step) {
            is Step.AuthorizeWelcome -> Step.LoadAuthority(welcome, step.command)
            is Step.LoadAuthority -> Step.OpenWelcome(welcome, step.command)
            is Step.OpenWelcome -> Step.RecordRestart(welcome, step.command)
            is Step.RecordRestart -> Step.ResolveMemberContacts(welcome, step.command)
            is Step.ResolveMemberContacts -> Step.PersistWelcome(welcome, step.command)
            is Step.PersistWelcome -> Step.ResolveMemberNames(welcome, step.command)
            is Step.ResolveMemberNames -> Step.InstallWelcome(welcome, step.command)
            is Step.InstallWelcome ->
                if (welcome.authorization.isFirstWelcome) {
                    Step.EstablishMutualIdentity(welcome, step.command)
                } else {
                    Step.SendReadyAcknowledgement(welcome, step.command)
                }
            is Step.EstablishMutualIdentity -> Step.SendReadyAcknowledgement(welcome, step.command)
            is Step.SendReadyAcknowledgement -> Step.CompleteWelcome(welcome, step.command)
            is Step.CompleteWelcome -> error("CompleteWelcome is the last step")
        }

    /** When the admin left with this welcome, the member it named admin keeps the group. */
    private fun continuingAdminId(packet: GroupCreatedPacket, welcome: Welcome): String? {
        if (!welcome.requireOpened().authorityLeft) return null
        return packet.members.indices.firstNotNullOfOrNull { index ->
            welcome.contactIdsByMember[index]?.takeIf { packet.members[index].role.isGroupAdminRole() }
        }
    }

    private fun memberKeys(packet: GroupCreatedPacket, contactIds: List<String?>): List<GroupWelcomeMemberKey> =
        packet.members.mapIndexedNotNull { index, member ->
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

    /** What the workflow learned about the welcome so far. */
    data class Welcome(
        val authorization: GroupIncomingWelcomeAuthorization,
        val persistedAt: Long,
        val firstWelcomeAuthority: RemotePeerIdentity? = null,
        val opened: OpenedIncomingGroupWelcome? = null,
        val contactIdsByMember: List<String?> = emptyList(),
        val memberDisplayNames: Map<String, String> = emptyMap(),
        val removedContactIds: Set<String> = emptySet()
    ) {
        fun requireOpened(): OpenedIncomingGroupWelcome = requireNotNull(opened) { "The group welcome was not opened" }
    }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupWelcome

        data class AuthorizeWelcome(
            override val command: IncomingCommand.ReceiveGroupWelcome
        ) : Step

        data class LoadAuthority(
            val welcome: Welcome,
            override val command: IncomingCommand.ReceiveGroupWelcome
        ) : Step

        data class OpenWelcome(
            val welcome: Welcome,
            override val command: IncomingCommand.ReceiveGroupWelcome
        ) : Step

        data class RecordRestart(
            val welcome: Welcome,
            override val command: IncomingCommand.ReceiveGroupWelcome
        ) : Step

        data class ResolveMemberContacts(
            val welcome: Welcome,
            override val command: IncomingCommand.ReceiveGroupWelcome
        ) : Step

        data class PersistWelcome(
            val welcome: Welcome,
            override val command: IncomingCommand.ReceiveGroupWelcome
        ) : Step

        data class ResolveMemberNames(
            val welcome: Welcome,
            override val command: IncomingCommand.ReceiveGroupWelcome
        ) : Step

        data class InstallWelcome(
            val welcome: Welcome,
            override val command: IncomingCommand.ReceiveGroupWelcome
        ) : Step

        data class EstablishMutualIdentity(
            val welcome: Welcome,
            override val command: IncomingCommand.ReceiveGroupWelcome
        ) : Step

        data class SendReadyAcknowledgement(
            val welcome: Welcome,
            override val command: IncomingCommand.ReceiveGroupWelcome
        ) : Step

        data class CompleteWelcome(
            val welcome: Welcome,
            override val command: IncomingCommand.ReceiveGroupWelcome
        ) : Step
    }

    sealed interface Outcome {
        /** Membership did not authorize this welcome for the local device, e.g. an obsolete epoch. */
        data object NotApplicable : Outcome

        data class Advanced(
            val welcome: Welcome
        ) : Outcome

        data object Completed : Outcome
    }
}
