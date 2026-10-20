package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.failure.ConversationFailureMapper
import com.cbgm.sparrow.feature.conversationorchestration.engine.failure.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectInvitationState
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectRecipient
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.OutgoingMessageContent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.policy.OutgoingMessageContentPolicy
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.port.DirectReauthorizationPort
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.InspectDirectInvitationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.LoadDirectRecipientUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.StoreOutgoingDirectMessageUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

/**
 * ValidateContent -> CheckBlockedContact -> CheckPendingIdentityChange -> CheckAuthorization
 * -> [InspectInvitation -> RequestReauthorization] -> ResolveConversation
 * -> DeliverMessage | (FindQueueConversation -> LoadQueueRecipient -> StoreWaitingMessage).
 *
 * An authorized contact gets the message immediately. Without authorization the message waits in
 * WAITING_FOR_AUTHORIZATION, and the workflow starts a fresh invitation when none is pending.
 */
class SendDirectMessageWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val contactBlocklistRepository: ContactBlocklistRepository,
    private val observePendingRemoteIdentityChanges: ObservePendingRemoteIdentityChangesUseCase,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val inspectDirectInvitation: InspectDirectInvitationUseCase,
    private val reauthorization: DirectReauthorizationPort,
    private val conversationRepository: DirectConversationRepository,
    private val loadDirectRecipient: LoadDirectRecipientUseCase,
    private val storeOutgoingDirectMessage: StoreOutgoingDirectMessageUseCase
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
            is Step.CheckBlockedContact ->
                if (contactBlocklistRepository.isBlocked(step.command.contactId).getOrThrow()) {
                    Outcome.ContactBlocked
                } else {
                    Outcome.ContactNotBlocked(step.content)
                }
            is Step.CheckPendingIdentityChange ->
                if (observePendingRemoteIdentityChanges().first().any { it.peerId == step.command.contactId }) {
                    // The previous exchange can remain MUTUAL until the user reviews the new
                    // identity. Never encrypt for those obsolete keys; keep the message waiting.
                    Outcome.IdentityChangePending(step.content)
                } else {
                    Outcome.NoIdentityChangePending(step.content)
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
            is Step.FindQueueConversation ->
                Outcome.QueueConversationFound(
                    target = conversationRepository.requireTarget(step.conversationId).getOrThrow(),
                    content = step.content,
                    plan = step.plan
                )
            is Step.LoadQueueRecipient ->
                Outcome.QueueRecipientLoaded(loadDirectRecipient(step.target).getOrThrow(), step.content, step.plan)
            is Step.StoreWaitingMessage -> {
                storeOutgoingDirectMessage(
                    recipient = step.recipient,
                    content = step.content,
                    deliveryStatus = MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION,
                    replyToMessageId = step.command.replyToMessageId
                ).getOrThrow()
                Outcome.MessageQueued(step.plan.authorizationFailure)
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ContentValidated -> Transition.Next(Step.CheckBlockedContact(outcome.content, step.command))
            Outcome.ContactBlocked ->
                Transition.Reject(
                    ConversationFailure(
                        reason = ConversationFailureReason.AUTHORIZATION_REQUIRED,
                        message = "Blocked contacts cannot send or receive direct messages"
                    )
                )
            is Outcome.ContactNotBlocked ->
                Transition.Next(Step.CheckPendingIdentityChange(outcome.content, step.command))
            is Outcome.IdentityChangePending ->
                Transition.Next(Step.ResolveConversation(outcome.content, DeliveryPlan.Queue(), step.command))
            is Outcome.NoIdentityChangePending ->
                Transition.Next(Step.CheckAuthorization(outcome.content, step.command))
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
                            Step.FindQueueConversation(
                                outcome.conversationId,
                                outcome.content,
                                outcome.plan,
                                step.command
                            )
                        )
                }
            Outcome.MessageDelivered -> Transition.Complete(ConversationResult.DirectMessageSent)
            is Outcome.QueueConversationFound ->
                Transition.Next(Step.LoadQueueRecipient(outcome.target, outcome.content, outcome.plan, step.command))
            is Outcome.QueueRecipientLoaded ->
                Transition.Next(
                    Step.StoreWaitingMessage(outcome.recipient, outcome.content, outcome.plan, step.command)
                )
            is Outcome.MessageQueued ->
                Transition.Complete(ConversationResult.DirectMessageQueued(outcome.authorizationFailure))
        }

    private suspend fun checkAuthorization(step: Step.CheckAuthorization): Outcome {
        val error = requireDirectChatAuthorization(step.command.contactId).exceptionOrNull()
            ?: return Outcome.Authorized(step.content)
        if (error !is DirectChatAuthorizationRequiredException) throw error
        return Outcome.AuthorizationMissing(step.content, error)
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

        data class CheckBlockedContact(
            val content: OutgoingMessageContent,
            override val command: DirectConversationCommand.SendMessage
        ) : Step

        data class CheckPendingIdentityChange(
            val content: OutgoingMessageContent,
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

        data class FindQueueConversation(
            val conversationId: String,
            val content: OutgoingMessageContent,
            val plan: DeliveryPlan.Queue,
            override val command: DirectConversationCommand.SendMessage
        ) : Step

        data class LoadQueueRecipient(
            val target: DirectConversationTarget,
            val content: OutgoingMessageContent,
            val plan: DeliveryPlan.Queue,
            override val command: DirectConversationCommand.SendMessage
        ) : Step

        data class StoreWaitingMessage(
            val recipient: DirectRecipient,
            val content: OutgoingMessageContent,
            val plan: DeliveryPlan.Queue,
            override val command: DirectConversationCommand.SendMessage
        ) : Step
    }

    sealed interface Outcome {
        data class ContentValidated(
            val content: OutgoingMessageContent
        ) : Outcome

        data object ContactBlocked : Outcome

        data class ContactNotBlocked(
            val content: OutgoingMessageContent
        ) : Outcome

        data class IdentityChangePending(
            val content: OutgoingMessageContent
        ) : Outcome

        data class NoIdentityChangePending(
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

        data class QueueConversationFound(
            val target: DirectConversationTarget,
            val content: OutgoingMessageContent,
            val plan: DeliveryPlan.Queue
        ) : Outcome

        data class QueueRecipientLoaded(
            val recipient: DirectRecipient,
            val content: OutgoingMessageContent,
            val plan: DeliveryPlan.Queue
        ) : Outcome

        data class MessageQueued(
            val authorizationFailure: ConversationFailure?
        ) : Outcome
    }
}
