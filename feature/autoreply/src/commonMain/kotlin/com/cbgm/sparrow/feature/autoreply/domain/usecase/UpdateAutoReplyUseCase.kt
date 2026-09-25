package com.cbgm.sparrow.feature.autoreply.domain.usecase

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.autoreply.domain.repository.AutoReplyRepository

class UpdateAutoReplyUseCase(
    private val repository: AutoReplyRepository
) {
    suspend operator fun invoke(
        id: String,
        name: String,
        text: String
    ): Result<Unit> =
        repository.update(
            id = id,
            name = name,
            text = text,
            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )
}
