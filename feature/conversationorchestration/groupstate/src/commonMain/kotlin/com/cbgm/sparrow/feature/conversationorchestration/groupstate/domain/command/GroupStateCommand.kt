package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowCommand

/**
 * Every group state job orchestration starts internally, from a runtime observer or another workflow.
 * Submit one with `engine.run(command)`; the engine finds the workflow that handles it.
 */
sealed interface GroupStateCommand : WorkflowCommand {
    /** Stores and publishes a new group avatar; [bytes] null removes it. */
    class PublishGroupAvatar(
        val groupId: String,
        val bytes: ByteArray?
    ) : GroupStateCommand

    /** Replays title, description, avatar and pin to a member that just became ready. */
    data class SendCurrentGroupMetadata(
        val groupId: String,
        val contactId: String
    ) : GroupStateCommand

    /** Replays the group's current pin to one member that just became ready. */
    data class SendCurrentGroupPin(
        val groupId: String,
        val contactId: String
    ) : GroupStateCommand
}
