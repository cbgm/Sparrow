package com.cbgm.sparrow.feature.polls.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.feature.polls.domain.repository.PollComposerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

class ObserveFinishedPollUseCase(
    private val repository: PollComposerRepository
) {
    operator fun invoke(): Flow<Poll> = repository.observeFinishedPoll().filterNotNull()
}
