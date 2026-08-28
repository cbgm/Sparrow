package com.cbgm.sparrow.feature.safety.data.repository

import com.cbgm.sparrow.feature.safety.data.datasource.MessageSafetyEmbeddingDataSource
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyAnalysisRepository
import com.cbgm.sparrow.feature.safety.util.MessageSafetyClassifier
import com.cbgm.sparrow.feature.safety.util.MessageSafetyStructuralAnalyzer

class MessageSafetyAnalysisRepositoryImpl(
    private val structuralAnalyzer: MessageSafetyStructuralAnalyzer,
    private val embeddingDataSource: MessageSafetyEmbeddingDataSource,
    private val classifier: MessageSafetyClassifier
) : MessageSafetyAnalysisRepository {
    override suspend fun analyze(text: String): Set<MessageSafetyReason> =
        linkedSetOf<MessageSafetyReason>().apply {
            addAll(structuralAnalyzer(text))
            embeddingDataSource.embed(text)?.let { embedding ->
                addAll(classifier.classify(embedding))
            }
        }
}
