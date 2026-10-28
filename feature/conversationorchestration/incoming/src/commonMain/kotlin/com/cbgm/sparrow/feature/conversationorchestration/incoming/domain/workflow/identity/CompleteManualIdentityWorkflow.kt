package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.CheckDirectPeerAuthorizedUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.identity.domain.usecase.SendIdentityVerificationReceiptUseCase
import kotlin.reflect.KClass

/**
 * LoadAuthorization -> [ActivateConversation] -> SendVerificationReceipt
 *
 * Manual setup may precede an invitation: only an accepted invitation creates the chat and releases its
 * queued messages.
 */
class CompleteManualIdentityWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val checkDirectPeerAuthorized: CheckDirectPeerAuthorizedUseCase,
    private val sendIdentityVerificationReceipt: SendIdentityVerificationReceiptUseCase
) : WorkflowStateMachine<
        IncomingCommand.CompleteManualIdentity,
        CompleteManualIdentityWorkflow.Step,
        CompleteManualIdentityWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.CompleteManualIdentity> =
        IncomingCommand.CompleteManualIdentity::class

    override fun firstStep(command: IncomingCommand.CompleteManualIdentity): Step = Step.LoadAuthorization(command)

    override suspend fun perform(step: Step): Outcome {
        val contactId = step.command.contactId
        return when (step) {
            is Step.LoadAuthorization -> loadAuthorization(contactId)
            is Step.ActivateConversation -> {
                engine.run(MessageCommand.ActivateAuthorizedConversation(contactId)).getOrThrow()
                Outcome.ConversationActivated
            }
            is Step.SendVerificationReceipt -> {
                sendIdentityVerificationReceipt(contactId).getOrThrow()
                Outcome.ReceiptSent
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.Authorized -> Transition.Next(Step.ActivateConversation(step.command))
            Outcome.NotAuthorized,
            Outcome.ConversationActivated -> Transition.Next(Step.SendVerificationReceipt(step.command))
            Outcome.ReceiptSent -> Transition.Complete()
        }

    private suspend fun loadAuthorization(contactId: String): Outcome =
        if (checkDirectPeerAuthorized(contactId).getOrThrow()) Outcome.Authorized else Outcome.NotAuthorized

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.CompleteManualIdentity

        data class LoadAuthorization(
            override val command: IncomingCommand.CompleteManualIdentity
        ) : Step

        data class ActivateConversation(
            override val command: IncomingCommand.CompleteManualIdentity
        ) : Step

        data class SendVerificationReceipt(
            override val command: IncomingCommand.CompleteManualIdentity
        ) : Step
    }

    sealed interface Outcome {
        data object Authorized : Outcome

        data object NotAuthorized : Outcome

        data object ConversationActivated : Outcome

        data object ReceiptSent : Outcome
    }
}
