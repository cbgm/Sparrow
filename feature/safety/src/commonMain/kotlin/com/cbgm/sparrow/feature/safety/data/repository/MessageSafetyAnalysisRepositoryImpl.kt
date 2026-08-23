package com.cbgm.sparrow.feature.safety.data.repository

import com.cbgm.sparrow.feature.safety.data.analyzer.MessageSafetyStructuralAnalyzer
import com.cbgm.sparrow.feature.safety.data.classifier.EmbeddingMessageSafetyClassifier
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyAnalysisRepository

class MessageSafetyAnalysisRepositoryImpl(
    private val structuralAnalyzer: MessageSafetyStructuralAnalyzer,
    private val classifier: EmbeddingMessageSafetyClassifier
) : MessageSafetyAnalysisRepository {
    override suspend fun analyze(text: String): Set<MessageSafetyReason> =
        linkedSetOf<MessageSafetyReason>().apply {
            addAll(structuralAnalyzer(text))
            addAll(classifier.classify(text))
        }
}
