package com.cbgm.sparrow.feature.avatar.domain.usecase

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditResult
import com.cbgm.sparrow.feature.avatar.domain.repository.AvatarEditorRepository

class ConsumeAvatarEditResultUseCase internal constructor(
    private val repository: AvatarEditorRepository
) {
    suspend operator fun invoke(result: AvatarEditResult): Result<ByteArray> =
        repository.consumeResult(result)
}
