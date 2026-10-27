package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration

import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
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
 * CreateConversation -> InitializeSecurity -> InitializeVerification -> InviteMembers.
 *
 * No invitation may be created before the owner's epoch and group key exist.
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
            is Step.InviteMembers -> {
                engine.run(
                    GroupAdministrationCommand.AddMembers(step.groupId, step.command.contactIds)
                ).getOrThrow()
                Outcome.MembersInvited(step.groupId)
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ConversationCreated -> Transition.Next(Step.InitializeSecurity(outcome.groupId, step.command))
            is Outcome.SecurityInitialized ->
                Transition.Next(Step.InitializeVerification(outcome.groupId, step.command))
            is Outcome.VerificationInitialized -> Transition.Next(Step.InviteMembers(outcome.groupId, step.command))
            is Outcome.MembersInvited -> Transition.Complete(ConversationResult.GroupCreated(outcome.groupId))
        }

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

        data class InviteMembers(
            val groupId: String,
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

        data class MembersInvited(
            val groupId: String
        ) : Outcome
    }
}
