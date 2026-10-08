package com.cbgm.sparrow.feature.polls.data.repository

import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.polls.domain.repository.PollComposerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class PollComposerRepositoryImpl : PollComposerRepository {
    private val finishedPoll = MutableStateFlow<Poll?>(null)

    override fun observeFinishedPoll(): Flow<Poll?> = finishedPoll

    override suspend fun finishPoll(poll: Poll): Result<Unit> =
        safeSuspendCall { finishedPoll.value = poll }

    override fun clearFinishedPoll() {
        finishedPoll.value = null
    }
}
