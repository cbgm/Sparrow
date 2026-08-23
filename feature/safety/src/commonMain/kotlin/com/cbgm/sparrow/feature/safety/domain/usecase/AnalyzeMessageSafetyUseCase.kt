package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyAnalysisRepository

class AnalyzeMessageSafetyUseCase(
    private val repository: MessageSafetyAnalysisRepository
) {
    suspend operator fun invoke(text: String): MessageSafetyAssessment {
        val reasons = repository.analyze(text)
        return if (reasons.isEmpty()) {
            MessageSafetyAssessment.Safe
        } else {
            MessageSafetyAssessment(reasons = reasons)
        }
    }
}
