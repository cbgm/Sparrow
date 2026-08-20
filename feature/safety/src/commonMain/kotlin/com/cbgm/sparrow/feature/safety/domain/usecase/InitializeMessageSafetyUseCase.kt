package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyRepository

class InitializeMessageSafetyUseCase(
    private val repository: MessageSafetyRepository
) {
    suspend operator fun invoke() = repository.initialize()
}
