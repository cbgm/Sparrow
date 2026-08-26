package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.settings.domain.repository.DeveloperErrorLogRepository

class ClearDeveloperErrorsUseCase(
    private val repository: DeveloperErrorLogRepository
) {
    suspend operator fun invoke() {
        repository.clearErrors()
    }
}
