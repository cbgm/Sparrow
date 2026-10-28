package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/**
 * The group messages still to hand to the outbox, oldest first: those written while no recipient was active
 * and those interrupted on the way to the outbox.
 */
class FindGroupMessagesToFlushUseCase(
    private val messageRepository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String): Result<List<StoredMessage>> =
        safeSuspendCall(logFailure = false) {
            val waiting = messageRepository.findQueued(groupId).getOrThrow()
            val interrupted = messageRepository.findAwaitingOutbox(groupId).getOrThrow()
            (waiting + interrupted)
                .distinctBy { it.id }
                .sortedWith(compareBy<StoredMessage> { it.createdAtEpochMilliseconds }.thenBy { it.id })
        }
}
