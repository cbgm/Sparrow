package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SendCurrentGroupAvatarUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SendCurrentGroupDescriptionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SendCurrentGroupTitleUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.groupAdministrationLockKey
import kotlin.reflect.KClass

/**
 * SendTitle -> SendDescription -> SendAvatar -> SendPin -> SendExpenses.
 *
 * Every item is replayed independently: a failed item is logged and the next one is still sent. The replay holds
 * the group administration lock, so no epoch rotation runs between authorizing the member and queueing an item;
 * the nested pin and expense replays take no lock of their own.
 */
class SendCurrentGroupMetadataWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val sendCurrentGroupTitle: SendCurrentGroupTitleUseCase,
    private val sendCurrentGroupDescription: SendCurrentGroupDescriptionUseCase,
    private val sendCurrentGroupAvatar: SendCurrentGroupAvatarUseCase
) : WorkflowStateMachine<
        GroupStateCommand.SendCurrentGroupMetadata,
        SendCurrentGroupMetadataWorkflow.Step,
        SendCurrentGroupMetadataWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("SendCurrentGroupMetadataWorkflow")

    override val commandType: KClass<GroupStateCommand.SendCurrentGroupMetadata> =
        GroupStateCommand.SendCurrentGroupMetadata::class

    override fun lockKey(command: GroupStateCommand.SendCurrentGroupMetadata): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupStateCommand.SendCurrentGroupMetadata): Step = Step.SendTitle(command)

    override suspend fun perform(step: Step): Outcome {
        val groupId = step.command.groupId
        val contactId = step.command.contactId
        val result =
            when (step) {
                is Step.SendTitle -> sendCurrentGroupTitle(groupId, contactId)
                is Step.SendDescription -> sendCurrentGroupDescription(groupId, contactId)
                is Step.SendAvatar -> sendCurrentGroupAvatar(groupId, contactId)
                is Step.SendPin -> engine.run(GroupStateCommand.SendCurrentGroupPin(groupId, contactId)).map { }
                is Step.SendExpenses ->
                    engine.run(GroupStateCommand.SendCurrentGroupExpenses(groupId, contactId)).map { }
            }
        result.onFailure { error -> logger.warn(error) { "Could not queue current group ${step.item} for $contactId" } }
        return Outcome.ItemHandled
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (step) {
            is Step.SendTitle -> Transition.Next(Step.SendDescription(step.command))
            is Step.SendDescription -> Transition.Next(Step.SendAvatar(step.command))
            is Step.SendAvatar -> Transition.Next(Step.SendPin(step.command))
            is Step.SendPin -> afterPin(step.command)
            is Step.SendExpenses -> Transition.Complete()
        }

    /** Expense history is replayed only to members that join while expenses are open. */
    private fun afterPin(command: GroupStateCommand.SendCurrentGroupMetadata): Transition<Step> =
        if (command.includeExpenseHistory) Transition.Next(Step.SendExpenses(command)) else Transition.Complete()

    sealed interface Step : WorkflowStep {
        val command: GroupStateCommand.SendCurrentGroupMetadata
        val item: String

        data class SendTitle(
            override val command: GroupStateCommand.SendCurrentGroupMetadata
        ) : Step {
            override val item: String get() = "title"
        }

        data class SendDescription(
            override val command: GroupStateCommand.SendCurrentGroupMetadata
        ) : Step {
            override val item: String get() = "description"
        }

        data class SendAvatar(
            override val command: GroupStateCommand.SendCurrentGroupMetadata
        ) : Step {
            override val item: String get() = "avatar"
        }

        data class SendPin(
            override val command: GroupStateCommand.SendCurrentGroupMetadata
        ) : Step {
            override val item: String get() = "pin"
        }

        data class SendExpenses(
            override val command: GroupStateCommand.SendCurrentGroupMetadata
        ) : Step {
            override val item: String get() = "expenses"
        }
    }

    sealed interface Outcome {
        /** The item was queued, skipped because it was never published, or its failure was logged. */
        data object ItemHandled : Outcome
    }
}
