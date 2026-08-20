package com.cbgm.sparrow.feature.safety.data.index

import com.cbgm.sparrow.data.database.dao.MessageSafetyDao
import com.cbgm.sparrow.feature.safety.data.mapper.toEntity
import com.cbgm.sparrow.feature.safety.data.model.MessageSafetyAnalyzer
import com.cbgm.sparrow.feature.safety.domain.usecase.AnalyzeMessageSafetyUseCase

class MessageSafetyIndexer(
    private val dao: MessageSafetyDao,
    private val analyzeMessageSafety: AnalyzeMessageSafetyUseCase
) {
    suspend fun indexNextBatch(): Int {
        val messages =
            dao.getMessagesMissingAssessment(
                analyzerVersion = MessageSafetyAnalyzer.VERSION,
                limit = MessageSafetyAnalyzer.INDEX_BATCH_SIZE
            )
        if (messages.isEmpty()) return 0

        val assessments =
            messages.map { message ->
                analyzeMessageSafety(message.text).toEntity(
                    messageId = message.messageId,
                    analyzerVersion = MessageSafetyAnalyzer.VERSION
                )
            }
        dao.upsertAssessments(assessments)
        return assessments.size
    }
}
