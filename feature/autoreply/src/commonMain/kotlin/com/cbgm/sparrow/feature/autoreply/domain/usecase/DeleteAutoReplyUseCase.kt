package com.cbgm.sparrow.feature.autoreply.domain.usecase

import com.cbgm.sparrow.feature.autoreply.domain.repository.AutoReplyRepository

class DeleteAutoReplyUseCase(
    private val repository: AutoReplyRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.delete(id)
}
