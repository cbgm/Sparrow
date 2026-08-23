package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyRepository

class ObserveMessageSafetyAssessmentsUseCase(
    private val repository: MessageSafetyRepository
) {
    operator fun invoke() = repository.assessments
}
