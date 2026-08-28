package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.settings.domain.model.DeveloperError
import com.cbgm.sparrow.feature.settings.domain.repository.DeveloperErrorLogRepository
import kotlinx.coroutines.flow.Flow

class ObserveDeveloperErrorsUseCase(
    private val repository: DeveloperErrorLogRepository
) {
    operator fun invoke(): Flow<List<DeveloperError>> = repository.observeErrors()
}
