package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/** The active participants an operation must reach; an operation without any recipient is refused. */
class FindGroupRecipientsUseCase(
    private val messageRepository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String): Result<List<String>> =
        safeSuspendCall(logFailure = false) {
            val recipients = messageRepository.findParticipantIds(groupId).getOrThrow()
            check(recipients.isNotEmpty()) { "Group has no active recipients" }
            recipients
        }

    /**
     * Who must learn about an edit or delete of the own message [messageId]. Empty when nobody can receive it or
     * the message was never handed to anyone (it is still queued without recipient states): the change then stays
     * local, and a queued message is flushed later with its current content or not at all.
     */
    suspend fun forOwnMessage(groupId: String, messageId: String): Result<List<String>> =
        safeSuspendCall(logFailure = false) {
            if (messageRepository.findRecipientStates(messageId).getOrThrow().isEmpty()) {
                return@safeSuspendCall emptyList()
            }
            messageRepository.findParticipantIds(groupId).getOrThrow()
        }
}
