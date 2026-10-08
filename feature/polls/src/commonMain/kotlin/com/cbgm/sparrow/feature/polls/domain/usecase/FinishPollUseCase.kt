package com.cbgm.sparrow.feature.polls.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.PollPolicy
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.polls.domain.repository.PollComposerRepository

class FinishPollUseCase(
    private val repository: PollComposerRepository
) {
    suspend operator fun invoke(poll: Poll): Result<Unit> {
        PollPolicy.requireValid(poll)
        require(poll.closedAtEpochMilliseconds == null) { "A new poll cannot be closed" }
        val expiresAt = poll.expiresAtEpochMilliseconds
        require(expiresAt == null || expiresAt > SystemClock.nowEpochMilliseconds()) {
            "Poll expiry must be in the future"
        }
        return repository.finishPoll(poll)
    }
}
