package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/**
 * Sends again what could not go out yet (an attachment waited for a protected route, or its upload failed): direct
 * messages are retried, groups flush their queued messages. One that fails again stays for the next time.
 */
class SendUnsentMessagesUseCase(
    private val directMessages: DirectMessageRepository,
    private val groupMessages: GroupMessageRepository,
    private val engine: WorkflowEngine
) {
    private val logger = SparrowLog.withTag("SendUnsentMessagesUseCase")

    /** How many direct messages and groups were tried. */
    suspend operator fun invoke(): Result<Int> =
        safeSuspendCall {
            val direct = directMessages.findUnsent().getOrThrow()
            val groups = groupMessages.findGroupsWithQueued().getOrThrow()
            direct.forEach { message ->
                engine.run(DirectConversationCommand.RetryMessage(message.id))
                    .onFailure { error -> logger.info { "Unsent message ${message.id} stays for later: ${error.message}" } }
            }
            groups.forEach { groupId ->
                engine.run(MessageCommand.FlushQueuedGroupMessages(groupId))
                    .onFailure { error -> logger.info { "Queued messages of group $groupId stay for later: ${error.message}" } }
            }
            direct.size + groups.size
        }
}
