package com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.workflow.event

import com.cbgm.sparrow.core.message.domain.model.GroupDeliveryStatusAggregation
import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.command.DeliveryCommand
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DeliveryEventTarget
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DirectMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.GroupMessageDeliveryStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.GroupRecipientDelivery
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository.DirectMessageDeliveryRepository
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository.GroupRecipientDeliveryRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import kotlin.reflect.KClass

/**
 * FindGroupRecipient -> (UpdateGroupRecipient -> UpdateGroupMessageStatus) | FindDirectMessage -> UpdateDirectMessage.
 *
 * Group recipients are always checked first because one group packet belongs to exactly one recipient.
 */
class ApplyDeliveryEventWorkflow internal constructor(
    private val directDeliveryRepository: DirectMessageDeliveryRepository,
    private val groupDeliveryRepository: GroupRecipientDeliveryRepository
) : WorkflowStateMachine<
        DeliveryCommand.ApplyDeliveryEvent,
        ApplyDeliveryEventWorkflow.Step,
        ApplyDeliveryEventWorkflow.Outcome
    > {
    override val commandType: KClass<DeliveryCommand.ApplyDeliveryEvent> = DeliveryCommand.ApplyDeliveryEvent::class

    override fun lockKey(command: DeliveryCommand.ApplyDeliveryEvent): String = DELIVERY_LOCK_KEY

    override fun firstStep(command: DeliveryCommand.ApplyDeliveryEvent): Step =
        when (command.target) {
            is DeliveryEventTarget.DirectMessage -> Step.FindDirectMessage(command)
            else -> Step.FindGroupRecipient(command)
        }

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindGroupRecipient -> findGroupRecipient(step.command)
            is Step.UpdateGroupRecipient -> updateGroupRecipient(step)
            is Step.UpdateGroupMessageStatus -> updateGroupMessageStatus(step.messageId)
            is Step.FindDirectMessage -> findDirectMessage(step.command)
            is Step.UpdateDirectMessage -> updateDirectMessage(step)
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.GroupRecipientFound ->
                Transition.Next(Step.UpdateGroupRecipient(outcome.recipient, step.command))
            is Outcome.NoGroupRecipient -> Transition.Next(Step.FindDirectMessage(step.command))
            is Outcome.GroupRecipientUpdated ->
                Transition.Next(Step.UpdateGroupMessageStatus(outcome.messageId, step.command))
            is Outcome.DirectMessageFound -> Transition.Next(Step.UpdateDirectMessage(outcome.status, step.command))
            Outcome.NoDeliveryOwner,
            Outcome.StatusUpdated -> Transition.Complete()
        }

    private suspend fun findGroupRecipient(command: DeliveryCommand.ApplyDeliveryEvent): Outcome {
        val recipient =
            when (val target = command.target) {
                is DeliveryEventTarget.Packet -> groupDeliveryRepository.findByPacketId(target.packetId).getOrThrow()
                is DeliveryEventTarget.Recipient ->
                    groupDeliveryRepository.findByRecipient(target.messageId, target.contactId).getOrThrow()
                is DeliveryEventTarget.DirectMessage -> null
            }
        return recipient?.let(Outcome::GroupRecipientFound) ?: Outcome.NoGroupRecipient
    }

    private suspend fun updateGroupRecipient(step: Step.UpdateGroupRecipient): Outcome {
        val current = step.recipient.status
        val next = GroupMessageDeliveryStateMachine.transition(current, step.command.event)
        if (next != current) {
            groupDeliveryRepository.updateRecipientStatus(
                recipient = step.recipient,
                status = next,
                lastError = step.command.errorMessage.takeIf { next == MessageDeliveryStatus.FAILED }
            ).getOrThrow()
        }
        return Outcome.GroupRecipientUpdated(step.recipient.messageId)
    }

    private suspend fun updateGroupMessageStatus(messageId: String): Outcome {
        val statuses = groupDeliveryRepository.findStatuses(messageId).getOrThrow()
        groupDeliveryRepository
            .updateMessageStatus(messageId, GroupDeliveryStatusAggregation.aggregate(statuses))
            .getOrThrow()
        return Outcome.StatusUpdated
    }

    private suspend fun findDirectMessage(command: DeliveryCommand.ApplyDeliveryEvent): Outcome {
        val status =
            when (val target = command.target) {
                is DeliveryEventTarget.Packet -> directDeliveryRepository.findStatusByPacketId(target.packetId)
                is DeliveryEventTarget.Recipient -> {
                    require(command.event.isReceipt()) { "Only receipt events can be applied by message ID" }
                    directDeliveryRepository.findStatusByRecipient(target.messageId, target.contactId)
                }
                is DeliveryEventTarget.DirectMessage -> directDeliveryRepository.findStatusByMessageId(target.messageId)
            }.getOrThrow()
        return status?.let(Outcome::DirectMessageFound) ?: Outcome.NoDeliveryOwner
    }

    private suspend fun updateDirectMessage(step: Step.UpdateDirectMessage): Outcome {
        val next = DirectMessageDeliveryStateMachine.transition(step.current, step.command.event)
        if (next == step.current) return Outcome.StatusUpdated
        when (val target = step.command.target) {
            is DeliveryEventTarget.Packet -> directDeliveryRepository.updateStatusByPacketId(target.packetId, next)
            is DeliveryEventTarget.Recipient -> directDeliveryRepository.updateStatusByMessageId(target.messageId, next)
            is DeliveryEventTarget.DirectMessage ->
                directDeliveryRepository.updateStatusByMessageId(target.messageId, next)
        }.getOrThrow()
        return Outcome.StatusUpdated
    }

    private fun MessageDeliveryEvent.isReceipt(): Boolean =
        this == MessageDeliveryEvent.DELIVERY_CONFIRMED || this == MessageDeliveryEvent.READ_CONFIRMED

    sealed interface Step : WorkflowStep {
        val command: DeliveryCommand.ApplyDeliveryEvent

        data class FindGroupRecipient(
            override val command: DeliveryCommand.ApplyDeliveryEvent
        ) : Step

        data class UpdateGroupRecipient(
            val recipient: GroupRecipientDelivery,
            override val command: DeliveryCommand.ApplyDeliveryEvent
        ) : Step

        data class UpdateGroupMessageStatus(
            val messageId: String,
            override val command: DeliveryCommand.ApplyDeliveryEvent
        ) : Step

        data class FindDirectMessage(
            override val command: DeliveryCommand.ApplyDeliveryEvent
        ) : Step

        data class UpdateDirectMessage(
            val current: MessageDeliveryStatus,
            override val command: DeliveryCommand.ApplyDeliveryEvent
        ) : Step
    }

    sealed interface Outcome {
        data class GroupRecipientFound(
            val recipient: GroupRecipientDelivery
        ) : Outcome

        data object NoGroupRecipient : Outcome

        data class GroupRecipientUpdated(
            val messageId: String
        ) : Outcome

        data class DirectMessageFound(
            val status: MessageDeliveryStatus
        ) : Outcome

        data object NoDeliveryOwner : Outcome

        data object StatusUpdated : Outcome
    }

    private companion object {
        const val DELIVERY_LOCK_KEY = "message-delivery"
    }
}
