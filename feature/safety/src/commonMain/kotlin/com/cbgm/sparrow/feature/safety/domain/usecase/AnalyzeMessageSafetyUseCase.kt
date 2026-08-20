package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.rule.MessageSafetyRuleEngine

class AnalyzeMessageSafetyUseCase(
    private val ruleEngine: MessageSafetyRuleEngine
) {
    operator fun invoke(text: String): MessageSafetyAssessment = ruleEngine(text)
}
