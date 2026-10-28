package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration

import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.RefreshOwnedGroupVerificationUseCase
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import kotlin.reflect.KClass

/**
 * CreateConversation -> InitializeSecurity -> InitializeVerification -> InviteMember for every contact.
 *
 * No invitation may be created before the owner's epoch and group key exist. Once they do, the group exists: a
 * contact whose invitation fails is logged and returned in [ConversationResult.GroupCreated.failedContactIds]
 * instead of failing the creation, so a retry does not create a second group; the admin can add that contact again.
 */
class CreateGroupWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val conversationRepository: GroupConversationRepository,
    private val groupSecurityRepository: GroupSecurityRepository,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val refreshOwnedGroupVerification: RefreshOwnedGroupVerificationUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.CreateGroup,
        CreateGroupWorkflow.Step,
        CreateGroupWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.CreateGroup> =
        GroupAdministrationCommand.CreateGroup::class

    override fun firstStep(command: GroupAdministrationCommand.CreateGroup): Step = Step.CreateConversation(command)

    private val logger = SparrowLog.withTag("CreateGroupWorkflow")

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.CreateConversation -> {
                val title = GroupMetadataPolicy.requireValidTitle(step.command.title)
                require(step.command.contactIds.isNotEmpty()) { "A group requires at least one contact" }
                Outcome.ConversationCreated(conversationRepository.create(title).getOrThrow())
            }
            is Step.InitializeSecurity -> {
                groupSecurityRepository.initializeOwnedGroup(
                    groupId = step.groupId,
                    createdAtEpochMilliseconds =
                        conversationRepository.getMembershipContext(step.groupId).getOrThrow().createdAtEpochMilliseconds,
                    localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                ).getOrThrow()
                Outcome.SecurityInitialized(step.groupId)
            }
            is Step.InitializeVerification -> {
                refreshOwnedGroupVerification(step.groupId).getOrThrow()
                Outcome.VerificationInitialized(step.groupId)
            }
            is Step.InviteMember -> inviteMember(step)
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ConversationCreated -> Transition.Next(Step.InitializeSecurity(outcome.groupId, step.command))
            is Outcome.SecurityInitialized ->
                Transition.Next(Step.InitializeVerification(outcome.groupId, step.command))
            is Outcome.VerificationInitialized ->
                nextInvitation(outcome.groupId, step.command.contactIds.toList(), emptyList(), step.command)
            is Outcome.InvitationAttempted ->
                nextInvitation(outcome.groupId, outcome.remaining, outcome.failedContactIds, step.command)
        }

    /** Each contact is invited on its own, so one failing contact neither fails the group nor skips the others. */
    private suspend fun inviteMember(step: Step.InviteMember): Outcome {
        val invited =
            engine.run(GroupAdministrationCommand.AddMembers(step.groupId, setOf(step.contactId)))
                .onFailure { error ->
                    logger.warn(error) { "Invitation of ${step.contactId} to new group ${step.groupId} failed" }
                }.isSuccess
        val failed = if (invited) step.failedContactIds else step.failedContactIds + step.contactId
        return Outcome.InvitationAttempted(step.groupId, step.remaining, failed)
    }

    private fun nextInvitation(
        groupId: String,
        remaining: List<String>,
        failedContactIds: List<String>,
        command: GroupAdministrationCommand.CreateGroup
    ): Transition<Step> =
        remaining.firstOrNull()
            ?.let { contactId ->
                Transition.Next(Step.InviteMember(groupId, contactId, remaining.drop(1), failedContactIds, command))
            }
            ?: Transition.Complete(ConversationResult.GroupCreated(groupId, failedContactIds))

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.CreateGroup

        data class CreateConversation(
            override val command: GroupAdministrationCommand.CreateGroup
        ) : Step

        data class InitializeSecurity(
            val groupId: String,
            override val command: GroupAdministrationCommand.CreateGroup
        ) : Step

        data class InitializeVerification(
            val groupId: String,
            override val command: GroupAdministrationCommand.CreateGroup
        ) : Step

        /** [remaining] are the contacts still to invite after this one. */
        data class InviteMember(
            val groupId: String,
            val contactId: String,
            val remaining: List<String>,
            val failedContactIds: List<String>,
            override val command: GroupAdministrationCommand.CreateGroup
        ) : Step
    }

    sealed interface Outcome {
        data class ConversationCreated(
            val groupId: String
        ) : Outcome

        data class SecurityInitialized(
            val groupId: String
        ) : Outcome

        data class VerificationInitialized(
            val groupId: String
        ) : Outcome

        data class InvitationAttempted(
            val groupId: String,
            val remaining: List<String>,
            val failedContactIds: List<String>
        ) : Outcome
    }
}
