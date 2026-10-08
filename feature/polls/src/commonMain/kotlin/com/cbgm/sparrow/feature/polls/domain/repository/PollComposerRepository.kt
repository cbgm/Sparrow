package com.cbgm.sparrow.feature.polls.domain.repository

import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import kotlinx.coroutines.flow.Flow

interface PollComposerRepository {
    fun observeFinishedPoll(): Flow<Poll?>

    suspend fun finishPoll(poll: Poll): Result<Unit>

    fun clearFinishedPoll()
}
