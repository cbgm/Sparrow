package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowCommand

/**
 * Every group state job orchestration starts internally, from a runtime observer or another workflow.
 * Submit one with `engine.run(command)`; the engine finds the workflow that handles it.
 */
sealed interface GroupStateCommand : WorkflowCommand {
    /**
     * Queues the locally saved, unpublished group title for every member with the current epoch, then marks it
     * published. Nothing happens when no unpublished title is stored (published already or superseded).
     */
    data class PublishGroupTitle(
        val groupId: String
    ) : GroupStateCommand

    /** Like [PublishGroupTitle], for the group description. */
    data class PublishGroupDescription(
        val groupId: String
    ) : GroupStateCommand

    /** Like [PublishGroupTitle], for the group avatar or its removal. */
    data class PublishGroupAvatar(
        val groupId: String
    ) : GroupStateCommand

    /** Like [PublishGroupTitle], for the pin slot (a pinned message or an empty slot). */
    data class PublishGroupPin(
        val groupId: String
    ) : GroupStateCommand

    /** Replays metadata; expense history is included only for an activated join. */
    data class SendCurrentGroupMetadata(
        val groupId: String,
        val contactId: String,
        val includeExpenseHistory: Boolean = false
    ) : GroupStateCommand

    /** Replays the group's current pin to one member that just became ready. */
    data class SendCurrentGroupPin(
        val groupId: String,
        val contactId: String
    ) : GroupStateCommand

    /** Replays the expense messages of the open expense board to one member that just became ready. */
    data class SendCurrentGroupExpenses(
        val groupId: String,
        val contactId: String
    ) : GroupStateCommand
}
