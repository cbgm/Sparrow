package com.cbgm.sparrow.feature.polls.domain.usecase

import com.cbgm.sparrow.feature.polls.domain.repository.PollComposerRepository

class ClearFinishedPollUseCase(
    private val repository: PollComposerRepository
) {
    operator fun invoke() = repository.clearFinishedPoll()
}
