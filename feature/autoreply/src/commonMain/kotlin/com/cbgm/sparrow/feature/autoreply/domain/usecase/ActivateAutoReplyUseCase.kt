package com.cbgm.sparrow.feature.autoreply.domain.usecase

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.autoreply.domain.repository.AutoReplyRepository

class ActivateAutoReplyUseCase(
    private val repository: AutoReplyRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> =
        repository.activate(
            id = id,
            activationSessionId = IdGenerator.generate()
        )
}
