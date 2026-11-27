package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectPendingAuthorizationMessagePolicy
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage

/** Drops the waiting direct messages whose retention ended; returns the ones that may still be released. */
class DiscardExpiredWaitingDirectMessagesUseCase(
    private val discardDirectMessages: DiscardDirectMessagesUseCase
) {
    suspend operator fun invoke(messages: List<StoredMessage>): Result<List<StoredMessage>> =
        safeSuspendCall(logFailure = false) {
            val now = SystemClock.nowEpochMilliseconds()
            val expired = messages.filter { message ->
                DirectPendingAuthorizationMessagePolicy.isExpired(
                    createdAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                    nowEpochMilliseconds = now
                )
            }
            discardDirectMessages(expired).getOrThrow()
            val expiredIds = expired.mapTo(mutableSetOf(), StoredMessage::id)
            messages.filterNot { message -> message.id in expiredIds }
        }
}
