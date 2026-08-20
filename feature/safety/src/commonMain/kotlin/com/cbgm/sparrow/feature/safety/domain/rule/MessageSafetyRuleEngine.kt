package com.cbgm.sparrow.feature.safety.domain.rule

import com.cbgm.sparrow.feature.safety.domain.analyzer.MessageSafetyStructuralAnalyzer

@Deprecated(
    message = "Natural-language safety detection now uses EmbeddingMessageSafetyClassifier; use MessageSafetyStructuralAnalyzer for deterministic URL/domain checks.",
    replaceWith = ReplaceWith("MessageSafetyStructuralAnalyzer")
)
typealias MessageSafetyRuleEngine = MessageSafetyStructuralAnalyzer
