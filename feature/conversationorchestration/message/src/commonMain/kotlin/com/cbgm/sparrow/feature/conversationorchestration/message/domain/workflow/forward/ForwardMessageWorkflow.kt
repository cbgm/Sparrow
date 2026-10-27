package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.forward

import com.cbgm.sparrow.core.base.messagepart.domain.model.MessageForwardPolicy
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.command.ForwardMessage
import com.cbgm.sparrow.feature.conversationorchestration.api.command.ForwardTarget
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.ConversationUnseenCountRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import kotlin.reflect.KClass

/**
 * ValidateParts -> (FindContact ->) SendDirectMessage -> (FindDirectConversation ->) IncrementUnseen
 * ValidateParts -> SendGroupMessage -> IncrementUnseen
 */
class ForwardMessageWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val directConversationRepository: DirectConversationRepository,
    private val unseenCountRepository: ConversationUnseenCountRepository
) : ConversationWorkflow<ForwardMessage, ForwardMessageWorkflow.Step, ForwardMessageWorkflow.Outcome> {
    override val commandType: KClass<ForwardMessage> = ForwardMessage::class

    override fun firstStep(command: ForwardMessage): Step = Step.ValidateParts(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.ValidateParts -> {
                MessageForwardPolicy.requireForwardable(step.command.parts)
                Outcome.PartsValid
            }
            is Step.FindContact -> {
                val contactId = directConversationRepository.findContactId(step.conversationId).getOrThrow()
                    ?: error("Direct conversation contact was not found")
                Outcome.ContactFound(contactId, step.conversationId)
            }
            is Step.SendDirectMessage -> {
                engine.run(
                    DirectConversationCommand.SendMessage(
                        contactId = step.contactId,
                        parts = step.command.parts,
                        conversationId = step.conversationId
                    )
                ).getOrThrow()
                Outcome.DirectMessageSent(step.contactId, step.conversationId)
            }
            is Step.FindDirectConversation -> {
                val conversationId = directConversationRepository.findConversationId(step.contactId).getOrThrow()
                    ?: error("Direct conversation was not found after forwarding")
                Outcome.DirectConversationFound(conversationId)
            }
            is Step.SendGroupMessage -> {
                engine.run(
                    GroupMessageCommand.SendMessage(step.groupId, step.command.parts)
                ).getOrThrow()
                Outcome.GroupMessageSent(step.groupId)
            }
            is Step.IncrementUnseen -> {
                unseenCountRepository.increment(step.conversationId).getOrThrow()
                Outcome.UnseenIncremented
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.PartsValid -> Transition.Next(sendStepFor(step.command))
            is Outcome.ContactFound ->
                Transition.Next(Step.SendDirectMessage(outcome.contactId, outcome.conversationId, step.command))
            is Outcome.DirectMessageSent ->
                Transition.Next(
                    outcome.conversationId?.let { Step.IncrementUnseen(it, step.command) }
                        ?: Step.FindDirectConversation(outcome.contactId, step.command)
                )
            is Outcome.DirectConversationFound ->
                Transition.Next(Step.IncrementUnseen(outcome.conversationId, step.command))
            is Outcome.GroupMessageSent -> Transition.Next(Step.IncrementUnseen(outcome.groupId, step.command))
            Outcome.UnseenIncremented -> Transition.Complete()
        }

    private fun sendStepFor(command: ForwardMessage): Step =
        when (val target = command.target) {
            is ForwardTarget.DirectConversation -> Step.FindContact(target.conversationId, command)
            is ForwardTarget.GroupConversation -> Step.SendGroupMessage(target.groupId, command)
            is ForwardTarget.Contact -> Step.SendDirectMessage(target.contactId, conversationId = null, command)
        }

    sealed interface Step : WorkflowStep {
        val command: ForwardMessage

        data class ValidateParts(
            override val command: ForwardMessage
        ) : Step

        data class FindContact(
            val conversationId: String,
            override val command: ForwardMessage
        ) : Step

        data class SendDirectMessage(
            val contactId: String,
            val conversationId: String?,
            override val command: ForwardMessage
        ) : Step

        data class FindDirectConversation(
            val contactId: String,
            override val command: ForwardMessage
        ) : Step

        data class SendGroupMessage(
            val groupId: String,
            override val command: ForwardMessage
        ) : Step

        data class IncrementUnseen(
            val conversationId: String,
            override val command: ForwardMessage
        ) : Step
    }

    sealed interface Outcome {
        data object PartsValid : Outcome

        data class ContactFound(
            val contactId: String,
            val conversationId: String
        ) : Outcome

        data class DirectMessageSent(
            val contactId: String,
            val conversationId: String?
        ) : Outcome

        data class DirectConversationFound(
            val conversationId: String
        ) : Outcome

        data class GroupMessageSent(
            val groupId: String
        ) : Outcome

        data object UnseenIncremented : Outcome
    }
}
