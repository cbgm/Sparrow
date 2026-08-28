package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository

class RemoveLocalProfilePictureUseCase(
    private val repository: LocalProfilePictureRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val previousChangedAt =
            repository
                .get()
                .getOrNull()
                ?.changedAtEpochMilliseconds
                ?: 0L
        val changedAt =
            maxOf(
                SystemClock.nowEpochMilliseconds(),
                previousChangedAt + 1L
            )

        return repository.remove(changedAtEpochMilliseconds = changedAt)
    }
}
