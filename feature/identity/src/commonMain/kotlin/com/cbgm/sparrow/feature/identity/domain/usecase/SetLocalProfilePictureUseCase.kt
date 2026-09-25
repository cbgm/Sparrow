package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository

class SetLocalProfilePictureUseCase(
    private val repository: LocalProfilePictureRepository
) {
    suspend operator fun invoke(bytes: ByteArray): Result<Unit> {
        require(bytes.isNotEmpty()) { "Profile picture must not be empty" }

        val previousChangedAt =
            repository
                .get()
                .getOrElse { failure ->
                    SparrowLog.error("SetLocalProfilePictureUseCase", "Could not load existing profile picture", failure)
                    return Result.failure(failure)
                }
                .changedAtEpochMilliseconds

        val changedAt =
            maxOf(
                SystemClock.nowEpochMilliseconds(),
                previousChangedAt + 1L
            )

        return repository.save(
            bytes = bytes,
            changedAtEpochMilliseconds = changedAt
        )
    }
}
