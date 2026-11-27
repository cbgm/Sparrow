package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake

/**
 * Removes the pending conversation of a declined group invitation. The change is never dated before the
 * invitation itself, even when the local clock is behind; a missing handshake does not bound it.
 */
class DiscardDeclinedGroupConversationUseCase(
    private val conversationRepository: GroupConversationRepository
) {
    suspend operator fun invoke(groupId: String, handshake: MembershipHandshake?): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            conversationRepository.discardPending(
                groupId = groupId,
                updatedAtEpochMilliseconds =
                    maxOf(handshake?.createdAtEpochMilliseconds ?: 0L, SystemClock.nowEpochMilliseconds())
            ).getOrThrow()
        }
}
