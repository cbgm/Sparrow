package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupPinPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindUnpublishedGroupStateUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.groupAdministrationLockKey
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import kotlin.reflect.KClass

/**
 * FindUnpublishedPin -> AuthorizeAdmin -> CreatePinPackets -> EnqueuePinPackets -> MarkPinPublished.
 * Without an unpublished pin (published already, or replaced by a newer one from another admin) it stops
 * after FindUnpublishedPin.
 *
 * The pin slot was saved locally before; this workflow only sends it. It holds the group administration lock, so the
 * packets are signed for the epoch and recipients that are current when they are queued. The pin is marked
 * published only while it is still the stored version; a failed send leaves it unpublished for the next attempt.
 */
class PublishGroupPinWorkflow internal constructor(
    private val pinRepository: GroupPinRepository,
    private val findUnpublishedGroupState: FindUnpublishedGroupStateUseCase,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val createGroupPinPackets: CreateGroupPinPacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : WorkflowStateMachine<
        GroupStateCommand.PublishGroupPin,
        PublishGroupPinWorkflow.Step,
        PublishGroupPinWorkflow.Outcome
    > {
    override val commandType: KClass<GroupStateCommand.PublishGroupPin> = GroupStateCommand.PublishGroupPin::class

    override fun lockKey(command: GroupStateCommand.PublishGroupPin): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupStateCommand.PublishGroupPin): Step = Step.FindUnpublishedPin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindUnpublishedPin -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                pinFound(findUnpublishedGroupState.pin(step.command.groupId).getOrThrow())
            }
            is Step.AuthorizeAdmin ->
                Outcome.AdminAuthorized(
                    step.pin,
                    requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow()
                )
            is Step.CreatePinPackets ->
                Outcome.PinPacketsCreated(step.pin, createGroupPinPackets(step.pin, step.context).getOrThrow())
            is Step.EnqueuePinPackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.PinPacketsEnqueued(step.pin)
            }
            is Step.MarkPinPublished -> {
                pinRepository.markPublished(step.command.groupId, step.pin.changedAtEpochMilliseconds).getOrThrow()
                Outcome.PinPublished
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NothingToPublish -> Transition.Complete()
            is Outcome.UnpublishedPinFound -> Transition.Next(Step.AuthorizeAdmin(outcome.pin, step.command))
            is Outcome.AdminAuthorized ->
                Transition.Next(Step.CreatePinPackets(outcome.pin, outcome.context, step.command))
            is Outcome.PinPacketsCreated ->
                Transition.Next(Step.EnqueuePinPackets(outcome.pin, outcome.packets, step.command))
            is Outcome.PinPacketsEnqueued -> Transition.Next(Step.MarkPinPublished(outcome.pin, step.command))
            Outcome.PinPublished -> Transition.Complete()
        }

    private fun pinFound(pin: GroupPinState?): Outcome =
        pin?.let(Outcome::UnpublishedPinFound) ?: Outcome.NothingToPublish

    sealed interface Step : WorkflowStep {
        val command: GroupStateCommand.PublishGroupPin

        data class FindUnpublishedPin(
            override val command: GroupStateCommand.PublishGroupPin
        ) : Step

        data class AuthorizeAdmin(
            val pin: GroupPinState,
            override val command: GroupStateCommand.PublishGroupPin
        ) : Step

        data class CreatePinPackets(
            val pin: GroupPinState,
            val context: GroupAdminContext,
            override val command: GroupStateCommand.PublishGroupPin
        ) : Step

        data class EnqueuePinPackets(
            val pin: GroupPinState,
            val packets: Map<String, GroupPinUpdatedPacket>,
            override val command: GroupStateCommand.PublishGroupPin
        ) : Step

        data class MarkPinPublished(
            val pin: GroupPinState,
            override val command: GroupStateCommand.PublishGroupPin
        ) : Step
    }

    sealed interface Outcome {
        data object NothingToPublish : Outcome

        data class UnpublishedPinFound(
            val pin: GroupPinState
        ) : Outcome

        data class AdminAuthorized(
            val pin: GroupPinState,
            val context: GroupAdminContext
        ) : Outcome

        data class PinPacketsCreated(
            val pin: GroupPinState,
            val packets: Map<String, GroupPinUpdatedPacket>
        ) : Outcome

        data class PinPacketsEnqueued(
            val pin: GroupPinState
        ) : Outcome

        data object PinPublished : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "pinned message"
    }
}
