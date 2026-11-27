package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.failure.ConversationFailureMapper
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectInvitationState
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectSendAuthorization
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.OutgoingMessageContent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.policy.OutgoingMessageContentPolicy
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.port.DirectReauthorizationPort
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.InspectDirectInvitationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import kotlin.reflect.KClass

/**
 * ValidateContent -> CheckAuthorization -> [InspectInvitation -> RequestReauthorization] -> ResolveConversation
 * -> DeliverMessage | (QueueMessage [-> DeliverMessage]).
 *
 * An authorized contact gets the message immediately. Without authorization the message waits in
 * WAITING_FOR_AUTHORIZATION, and the workflow starts a fresh invitation when none is pending.
 *
 * The workflow takes no lock, so a message is stored and shown at once even while an earlier message to the same
 * contact still uploads its attachments. Only QueueMessage ([MessageCommand.StoreWaitingDirectMessage]) holds the
 * contact's release lock: otherwise a release could run between "authorization missing" and storing the waiting
 * message, find nothing, and leave it waiting until the next identity event. When it finds the contact authorized
 * meanwhile, the message is delivered normally instead.
 */
class SendDirectMessageWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val inspectDirectInvitation: InspectDirectInvitationUseCase,
    private val reauthorization: DirectReauthorizationPort,
    private val conversationRepository: DirectConversationRepository
) : ConversationWorkflow<
        DirectConversationCommand.SendMessage,
        SendDirectMessageWorkflow.Step,
        SendDirectMessageWorkflow.Outcome
    > {
    override val commandType: KClass<DirectConversationCommand.SendMessage> =
        DirectConversationCommand.SendMessage::class

    override fun firstStep(command: DirectConversationCommand.SendMessage): Step = Step.ValidateContent(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.ValidateContent -> {
                require(step.command.contactId.isNotBlank()) { "Contact ID must not be blank" }
                Outcome.ContentValidated(OutgoingMessageContentPolicy.requireDirectContent(step.command.parts))
            }
            is Step.CheckAuthorization -> checkAuthorization(step)
            is Step.InspectInvitation ->
                when (inspectDirectInvitation(step.command.contactId).getOrThrow()) {
                    DirectInvitationState.PENDING -> Outcome.InvitationPending(step.content)
                    DirectInvitationState.RETRYABLE -> Outcome.InvitationRetryable(step.content)
                    DirectInvitationState.UNAVAILABLE -> Outcome.InvitationUnavailable(step.authorizationError)
                }
            is Step.RequestReauthorization ->
                reauthorization.requestReauthorization(step.command.contactId).fold(
                    onSuccess = { Outcome.Reauthorized(step.content) },
                    onFailure = { error -> Outcome.ReauthorizationFailed(step.content, error) }
                )
            is Step.ResolveConversation ->
                Outcome.ConversationResolved(
                    conversationId = step.command.conversationId
                        ?: conversationRepository.getOrCreate(step.command.contactId).getOrThrow(),
                    content = step.content,
                    plan = step.plan
                )
            is Step.DeliverMessage -> {
                engine.run(
                    MessageCommand.DeliverDirectMessage(
                        conversationId = step.conversationId,
                        content = step.content,
                        replyToMessageId = step.command.replyToMessageId
                    )
                ).getOrThrow()
                Outcome.MessageDelivered
            }
            is Step.QueueMessage ->
                messageQueued(
                    engine.run(
                        MessageCommand.StoreWaitingDirectMessage(
                            contactId = step.command.contactId,
                            conversationId = step.conversationId,
                            content = step.content,
                            replyToMessageId = step.command.replyToMessageId,
                            authorizationFailure = step.plan.authorizationFailure
                        )
                    ).getOrThrow(),
                    step
                )
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ContentValidated -> Transition.Next(Step.CheckAuthorization(outcome.content, step.command))
            is Outcome.ContactBlocked ->
                Transition.Reject(
                    ConversationFailure(
                        reason = ConversationFailureReason.AUTHORIZATION_REQUIRED,
                        message = outcome.reason
                    )
                )
            is Outcome.IdentityChangePending ->
                Transition.Next(Step.ResolveConversation(outcome.content, DeliveryPlan.Queue(), step.command))
            is Outcome.Authorized ->
                Transition.Next(Step.ResolveConversation(outcome.content, DeliveryPlan.Send, step.command))
            is Outcome.AuthorizationMissing ->
                Transition.Next(Step.InspectInvitation(outcome.content, outcome.error, step.command))
            is Outcome.InvitationPending ->
                Transition.Next(Step.ResolveConversation(outcome.content, DeliveryPlan.Queue(), step.command))
            is Outcome.InvitationRetryable ->
                Transition.Next(Step.RequestReauthorization(outcome.content, step.command))
            is Outcome.InvitationUnavailable -> Transition.Reject.because(outcome.error)
            is Outcome.Reauthorized ->
                Transition.Next(Step.ResolveConversation(outcome.content, DeliveryPlan.Queue(), step.command))
            is Outcome.ReauthorizationFailed ->
                Transition.Next(
                    Step.ResolveConversation(
                        content = outcome.content,
                        plan = DeliveryPlan.Queue(ConversationFailureMapper.toFailure(outcome.error)),
                        command = step.command
                    )
                )
            is Outcome.ConversationResolved ->
                when (outcome.plan) {
                    DeliveryPlan.Send ->
                        Transition.Next(Step.DeliverMessage(outcome.conversationId, outcome.content, step.command))
                    is DeliveryPlan.Queue ->
                        Transition.Next(
                            Step.QueueMessage(outcome.conversationId, outcome.content, outcome.plan, step.command)
                        )
                }
            Outcome.MessageDelivered -> Transition.Complete(ConversationResult.DirectMessageSent)
            is Outcome.AuthorizedWhileQueuing ->
                Transition.Next(Step.DeliverMessage(outcome.conversationId, outcome.content, step.command))
            is Outcome.MessageQueued -> Transition.Complete(outcome.result)
        }

    /** The waiting store completes without a queued result when the contact became authorized meanwhile. */
    private fun messageQueued(result: ConversationResult, step: Step.QueueMessage): Outcome =
        when (result) {
            is ConversationResult.DirectMessageQueued -> Outcome.MessageQueued(result)
            else -> Outcome.AuthorizedWhileQueuing(step.conversationId, step.content)
        }

    private suspend fun checkAuthorization(step: Step.CheckAuthorization): Outcome {
        val authorization = requireDirectChatAuthorization.checkForNewMessage(step.command.contactId).getOrThrow()
        return when (authorization) {
            DirectSendAuthorization.Granted -> Outcome.Authorized(step.content)
            is DirectSendAuthorization.ContactBlocked -> Outcome.ContactBlocked(authorization.reason)
            DirectSendAuthorization.IdentityChangePending -> Outcome.IdentityChangePending(step.content)
            is DirectSendAuthorization.Missing -> Outcome.AuthorizationMissing(step.content, authorization.error)
        }
    }

    /** How the message leaves the device once its conversation is known. */
    sealed interface DeliveryPlan {
        data object Send : DeliveryPlan

        data class Queue(
            val authorizationFailure: ConversationFailure? = null
        ) : DeliveryPlan
    }

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.SendMessage

        data class ValidateContent(
            override val command: DirectConversationCommand.SendMessage
        ) : Step

        data class CheckAuthorization(
            val content: OutgoingMessageContent,
            override val command: DirectConversationCommand.SendMessage
        ) : Step

        data class InspectInvitation(
            val content: OutgoingMessageContent,
            val authorizationError: Throwable,
            override val command: DirectConversationCommand.SendMessage
        ) : Step

        data class RequestReauthorization(
            val content: OutgoingMessageContent,
            override val command: DirectConversationCommand.SendMessage
        ) : Step

        data class ResolveConversation(
            val content: OutgoingMessageContent,
            val plan: DeliveryPlan,
            override val command: DirectConversationCommand.SendMessage
        ) : Step

        data class DeliverMessage(
            val conversationId: String,
            val content: OutgoingMessageContent,
            override val command: DirectConversationCommand.SendMessage
        ) : Step

        data class QueueMessage(
            val conversationId: String,
            val content: OutgoingMessageContent,
            val plan: DeliveryPlan.Queue,
            override val command: DirectConversationCommand.SendMessage
        ) : Step
    }

    sealed interface Outcome {
        data class ContentValidated(
            val content: OutgoingMessageContent
        ) : Outcome

        data class ContactBlocked(
            val reason: String
        ) : Outcome

        data class IdentityChangePending(
            val content: OutgoingMessageContent
        ) : Outcome

        data class Authorized(
            val content: OutgoingMessageContent
        ) : Outcome

        data class AuthorizationMissing(
            val content: OutgoingMessageContent,
            val error: Throwable
        ) : Outcome

        data class InvitationPending(
            val content: OutgoingMessageContent
        ) : Outcome

        data class InvitationRetryable(
            val content: OutgoingMessageContent
        ) : Outcome

        data class InvitationUnavailable(
            val error: Throwable
        ) : Outcome

        data class Reauthorized(
            val content: OutgoingMessageContent
        ) : Outcome

        data class ReauthorizationFailed(
            val content: OutgoingMessageContent,
            val error: Throwable
        ) : Outcome

        data class ConversationResolved(
            val conversationId: String,
            val content: OutgoingMessageContent,
            val plan: DeliveryPlan
        ) : Outcome

        data object MessageDelivered : Outcome

        data class AuthorizedWhileQueuing(
            val conversationId: String,
            val content: OutgoingMessageContent
        ) : Outcome

        data class MessageQueued(
            val result: ConversationResult.DirectMessageQueued
        ) : Outcome
    }
}
