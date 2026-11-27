package com.cbgm.sparrow.feature.conversationorchestration.groupstate.runtime

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateKind
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.UnpublishedGroupState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindUnpublishedGroupStateUseCase

/**
 * Publishes group state that was saved locally but not queued for the members, because the app stopped or the
 * publish failed. Runs once after the identity is ready; a state that fails again stays unpublished for the next
 * start or the next change of the same state.
 */
class UnpublishedGroupStateRunner internal constructor(
    private val engine: WorkflowEngine,
    private val findUnpublishedGroupState: FindUnpublishedGroupStateUseCase
) {
    private val logger = SparrowLog.withTag("UnpublishedGroupStateRunner")

    suspend fun run(): Result<Unit> =
        findUnpublishedGroupState().map { states -> states.forEach { state -> publish(state) } }

    /** One failed group does not stop the others. */
    private suspend fun publish(state: UnpublishedGroupState) {
        engine.run(publishCommand(state)).onFailure { error ->
            logger.warn(error) { "Could not publish group ${state.kind} of ${state.groupId}" }
        }
    }

    private fun publishCommand(state: UnpublishedGroupState): GroupStateCommand =
        when (state.kind) {
            GroupStateKind.TITLE -> GroupStateCommand.PublishGroupTitle(state.groupId)
            GroupStateKind.DESCRIPTION -> GroupStateCommand.PublishGroupDescription(state.groupId)
            GroupStateKind.AVATAR -> GroupStateCommand.PublishGroupAvatar(state.groupId)
            GroupStateKind.PIN -> GroupStateCommand.PublishGroupPin(state.groupId)
        }
}
