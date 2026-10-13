package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DirectMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RecordRetryRequestedUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import kotlin.reflect.KClass

/**
 * FindRetryableMessage -> FindConversation -> CheckPacketIdentity -> CheckAuthorization
 * -> ResendPacket -> RecordRetry.
 */
class RetryDirectMessageWorkflow internal constructor(
    private val conversationRepository: DirectConversationRepository,
    private val messageRepository: DirectMessageRepository,
    private val protocolOutbox: ProtocolOutbox,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val recordRetryRequested: RecordRetryRequestedUseCase
) : ConversationWorkflow<
        DirectConversationCommand.RetryMessage,
        RetryDirectMessageWorkflow.Step,
        RetryDirectMessageWorkflow.Outcome
    > {
    override val commandType: KClass<DirectConversationCommand.RetryMessage> =
        DirectConversationCommand.RetryMessage::class

    override fun firstStep(command: DirectConversationCommand.RetryMessage): Step = Step.FindRetryableMessage(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindRetryableMessage -> {
                require(step.command.messageId.isNotBlank()) { "Message ID must not be blank" }
                val message =
                    messageRepository.find(step.command.messageId).getOrThrow() ?: error("Message was not found")
                check(message.isMine) { "Only outgoing messages can be retried" }
                check(
                    DirectMessageDeliveryStateMachine.canTransition(
                        current = message.deliveryStatus,
                        event = MessageDeliveryEvent.RETRY_REQUESTED
                    )
                ) { "Only failed direct messages can be retried" }
                Outcome.RetryableMessageFound(message.conversationId, message.packetId)
            }
            is Step.FindConversation ->
                Outcome.ConversationFound(
                    target = conversationRepository.requireTarget(step.conversationId).getOrThrow(),
                    packetId =
                        step.packetId?.takeIf(String::isNotBlank) ?: error("Message has no linked protocol packet")
                )
            is Step.CheckPacketIdentity -> {
                // An explicit retry must not resurrect an obsolete packet after a contact
                // changes cryptographic identity: its stored transport payload was prepared
                // for a different recipient key.
                check(protocolOutbox.findByPacketId(step.packetId).getOrThrow()?.status != OutboxStatus.QUARANTINED) {
                    "This message belongs to the contact's previous identity. After reconnecting, send its content as a new message."
                }
                Outcome.PacketCurrent(step.target, step.packetId)
            }
            is Step.CheckAuthorization -> {
                requireDirectChatAuthorization(step.target.contactId).getOrThrow()
                Outcome.Authorized(step.packetId)
            }
            is Step.ResendPacket -> {
                protocolOutbox.resend(step.packetId).getOrThrow()
                Outcome.PacketResent
            }
            is Step.RecordRetry -> {
                recordRetryRequested.forDirectMessage(step.command.messageId).getOrThrow()
                Outcome.RetryRecorded
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.RetryableMessageFound ->
                Transition.Next(Step.FindConversation(outcome.conversationId, outcome.packetId, step.command))
            is Outcome.ConversationFound ->
                Transition.Next(Step.CheckPacketIdentity(outcome.target, outcome.packetId, step.command))
            is Outcome.PacketCurrent ->
                Transition.Next(Step.CheckAuthorization(outcome.target, outcome.packetId, step.command))
            is Outcome.Authorized -> Transition.Next(Step.ResendPacket(outcome.packetId, step.command))
            Outcome.PacketResent -> Transition.Next(Step.RecordRetry(step.command))
            Outcome.RetryRecorded -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.RetryMessage

        data class FindRetryableMessage(
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class FindConversation(
            val conversationId: String,
            val packetId: String?,
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class CheckPacketIdentity(
            val target: DirectConversationTarget,
            val packetId: String,
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class CheckAuthorization(
            val target: DirectConversationTarget,
            val packetId: String,
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class ResendPacket(
            val packetId: String,
            override val command: DirectConversationCommand.RetryMessage
        ) : Step

        data class RecordRetry(
            override val command: DirectConversationCommand.RetryMessage
        ) : Step
    }

    sealed interface Outcome {
        data class RetryableMessageFound(
            val conversationId: String,
            val packetId: String?
        ) : Outcome

        data class ConversationFound(
            val target: DirectConversationTarget,
            val packetId: String
        ) : Outcome

        data class PacketCurrent(
            val target: DirectConversationTarget,
            val packetId: String
        ) : Outcome

        data class Authorized(
            val packetId: String
        ) : Outcome

        data object PacketResent : Outcome

        data object RetryRecorded : Outcome
    }
}
