package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.MessageReactionRepository

/** Adds the local user's reaction, or removes it when it is already there; returns whether it was removed. */
class ToggleLocalReactionUseCase(
    private val reactionRepository: MessageReactionRepository
) {
    suspend operator fun invoke(messageId: String, conversationId: String, emoji: String): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            val removed = reactionRepository.exists(messageId, LOCAL_REACTOR, emoji).getOrThrow()
            if (removed) {
                reactionRepository.delete(messageId, LOCAL_REACTOR, emoji).getOrThrow()
            } else {
                reactionRepository.save(messageId, conversationId, LOCAL_REACTOR, emoji).getOrThrow()
            }
            removed
        }

    private companion object {
        /** The reaction repository keys the local user's reactions by a null reactor. */
        val LOCAL_REACTOR: String? = null
    }
}
