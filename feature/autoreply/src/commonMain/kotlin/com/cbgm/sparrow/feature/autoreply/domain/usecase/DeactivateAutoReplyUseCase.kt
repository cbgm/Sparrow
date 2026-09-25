package com.cbgm.sparrow.feature.autoreply.domain.usecase

import com.cbgm.sparrow.feature.autoreply.domain.repository.AutoReplyRepository

class DeactivateAutoReplyUseCase(
    private val repository: AutoReplyRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.deactivate()
}
