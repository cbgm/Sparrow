package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.feature.safety.domain.analyzer.MessageSafetyStructuralAnalyzer
import com.cbgm.sparrow.feature.safety.domain.classifier.MessageSafetyClassifier
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.domain.resolver.MessageSafetyRiskResolver

class AnalyzeMessageSafetyUseCase(
    private val structuralAnalyzer: MessageSafetyStructuralAnalyzer,
    private val classifier: MessageSafetyClassifier,
    private val riskResolver: MessageSafetyRiskResolver
) {
    suspend operator fun invoke(text: String): MessageSafetyAssessment {
        val reasons = linkedSetOf<MessageSafetyReason>()
        reasons += structuralAnalyzer(text)
        reasons += classifier.classify(text)
        return riskResolver(reasons)
    }
}
