package com.cbgm.sparrow.feature.autoreply.domain.usecase

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.autoreply.domain.model.AutoReply
import com.cbgm.sparrow.feature.autoreply.domain.repository.AutoReplyRepository

class CreateAutoReplyUseCase(
    private val repository: AutoReplyRepository
) {
    suspend operator fun invoke(
        name: String,
        text: String
    ): Result<Unit> {
        val now = SystemClock.nowEpochMilliseconds()

        return repository.create(
            AutoReply(
                id = IdGenerator.generate(),
                name = name,
                text = text,
                isActive = false,
                activationSessionId = null,
                createdAtEpochMilliseconds = now,
                updatedAtEpochMilliseconds = now
            )
        )
    }
}
