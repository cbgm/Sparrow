package com.cbgm.sparrow.feature.safety.data.datasource

import com.cbgm.sparrow.data.database.dao.MessageSafetyDao
import com.cbgm.sparrow.data.database.entity.MessageSafetyAssessmentEntity
import com.cbgm.sparrow.data.database.model.MessageSafetySourceDto
import kotlinx.coroutines.flow.Flow

class MessageSafetyLocalDataSource(
    private val dao: MessageSafetyDao
) {
    fun observeVisibleAssessments(analyzerVersion: Int): Flow<List<MessageSafetyAssessmentEntity>> =
        dao.observeVisibleAssessments(analyzerVersion)

    suspend fun getMessagesMissingAssessment(
        analyzerVersion: Int,
        limit: Int
    ): List<MessageSafetySourceDto> =
        dao.getMessagesMissingAssessment(
            analyzerVersion = analyzerVersion,
            limit = limit
        )

    suspend fun getUnassessedMessageCount(analyzerVersion: Int): Int =
        dao.getUnassessedMessageCount(analyzerVersion)

    fun observeUnassessedMessageCount(analyzerVersion: Int): Flow<Int> =
        dao.observeUnassessedMessageCount(analyzerVersion)

    suspend fun upsertAssessment(assessment: MessageSafetyAssessmentEntity) {
        dao.upsertAssessments(listOf(assessment))
    }

    suspend fun deleteAllAssessments() {
        dao.deleteAllAssessments()
    }

    suspend fun deleteAssessmentsForOtherAnalyzers(analyzerVersion: Int) {
        dao.deleteAssessmentsForOtherAnalyzers(analyzerVersion)
    }
}
