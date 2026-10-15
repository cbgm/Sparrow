package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupPinPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import kotlin.reflect.KClass

/** FindPin -> CreatePinPacket -> EnqueuePinPacket. */
class SendCurrentGroupPinWorkflow internal constructor(
    private val pinRepository: GroupPinRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val createGroupPinPackets: CreateGroupPinPacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : WorkflowStateMachine<
        GroupStateCommand.SendCurrentGroupPin,
        SendCurrentGroupPinWorkflow.Step,
        SendCurrentGroupPinWorkflow.Outcome
    > {
    override val commandType: KClass<GroupStateCommand.SendCurrentGroupPin> =
        GroupStateCommand.SendCurrentGroupPin::class

    override fun firstStep(command: GroupStateCommand.SendCurrentGroupPin): Step = Step.FindPin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindPin -> {
                require(step.command.contactId.isNotBlank()) { "Contact ID must not be blank" }
                pinRepository.find(step.command.groupId).getOrThrow()?.let(Outcome::PinFound) ?: Outcome.NoPin
            }
            is Step.CreatePinPacket -> {
                val context = requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow()
                check(step.command.contactId in context.recipientContactIds) { "Contact is not an active group member" }
                Outcome.PinPacketCreated(
                    createGroupPinPackets(step.pin, context, recipients = setOf(step.command.contactId)).getOrThrow()
                )
            }
            is Step.EnqueuePinPacket -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.PinPacketEnqueued
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NoPin -> Transition.Complete()
            is Outcome.PinFound -> Transition.Next(Step.CreatePinPacket(outcome.pin, step.command))
            is Outcome.PinPacketCreated -> Transition.Next(Step.EnqueuePinPacket(outcome.packets, step.command))
            Outcome.PinPacketEnqueued -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupStateCommand.SendCurrentGroupPin

        data class FindPin(
            override val command: GroupStateCommand.SendCurrentGroupPin
        ) : Step

        data class CreatePinPacket(
            val pin: GroupPinState,
            override val command: GroupStateCommand.SendCurrentGroupPin
        ) : Step

        data class EnqueuePinPacket(
            val packets: Map<String, GroupPinUpdatedPacket>,
            override val command: GroupStateCommand.SendCurrentGroupPin
        ) : Step
    }

    sealed interface Outcome {
        data object NoPin : Outcome

        data class PinFound(
            val pin: GroupPinState
        ) : Outcome

        data class PinPacketCreated(
            val packets: Map<String, GroupPinUpdatedPacket>
        ) : Outcome

        data object PinPacketEnqueued : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "pinned message"
    }
}
