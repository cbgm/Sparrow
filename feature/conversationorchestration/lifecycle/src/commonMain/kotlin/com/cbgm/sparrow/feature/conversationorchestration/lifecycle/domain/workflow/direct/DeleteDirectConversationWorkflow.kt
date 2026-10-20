package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct

import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.protocol.mailbox.MailboxCapabilityLifecycle
import kotlin.reflect.KClass

/** FindContact -> RevokeExchange -> RevokeMailbox -> DeleteConversation. */
class DeleteDirectConversationWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val conversationRepository: DirectConversationRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle
) : ConversationWorkflow<
        DirectConversationCommand.DeleteConversation,
        DeleteDirectConversationWorkflow.Step,
        DeleteDirectConversationWorkflow.Outcome
    > {
    override val commandType: KClass<DirectConversationCommand.DeleteConversation> =
        DirectConversationCommand.DeleteConversation::class

    override fun firstStep(command: DirectConversationCommand.DeleteConversation): Step = Step.FindContact(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindContact ->
                conversationRepository.findContactId(step.command.conversationId).getOrThrow()
                    ?.let(Outcome::ContactFound)
                    ?: Outcome.ConversationAlreadyGone
            is Step.RevokeExchange -> {
                engine.run(LifecycleCommand.RevokePeerExchange(step.contactId)).getOrThrow()
                Outcome.ExchangeRevoked(step.contactId)
            }
            is Step.RevokeMailbox -> {
                mailboxCapabilityLifecycle.revokeForContact(step.contactId).getOrThrow()
                Outcome.MailboxRevoked
            }
            is Step.DeleteConversation -> {
                conversationRepository.delete(step.command.conversationId).getOrThrow()
                Outcome.ConversationDeleted
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ContactFound -> Transition.Next(Step.RevokeExchange(outcome.contactId, step.command))
            Outcome.ConversationAlreadyGone -> Transition.Complete()
            is Outcome.ExchangeRevoked -> Transition.Next(Step.RevokeMailbox(outcome.contactId, step.command))
            Outcome.MailboxRevoked -> Transition.Next(Step.DeleteConversation(step.command))
            Outcome.ConversationDeleted -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.DeleteConversation

        data class FindContact(
            override val command: DirectConversationCommand.DeleteConversation
        ) : Step

        data class RevokeExchange(
            val contactId: String,
            override val command: DirectConversationCommand.DeleteConversation
        ) : Step

        data class RevokeMailbox(
            val contactId: String,
            override val command: DirectConversationCommand.DeleteConversation
        ) : Step

        data class DeleteConversation(
            override val command: DirectConversationCommand.DeleteConversation
        ) : Step
    }

    sealed interface Outcome {
        data class ContactFound(
            val contactId: String
        ) : Outcome

        data object ConversationAlreadyGone : Outcome

        data class ExchangeRevoked(
            val contactId: String
        ) : Outcome

        data object MailboxRevoked : Outcome

        data object ConversationDeleted : Outcome
    }
}
