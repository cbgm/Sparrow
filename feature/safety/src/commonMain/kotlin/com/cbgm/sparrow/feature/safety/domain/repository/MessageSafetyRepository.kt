package com.cbgm.sparrow.feature.safety.domain.repository

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyCandidate
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MessageSafetyRepository {
    val state: StateFlow<MessageSafetyState>
    val assessments: StateFlow<Map<String, MessageSafetyAssessment>>

    fun updateState(state: MessageSafetyState)

    suspend fun getUnassessedMessages(limit: Int): List<MessageSafetyCandidate>

    suspend fun getUnassessedMessageCount(): Int

    fun observeUnassessedMessageCount(): Flow<Int>

    suspend fun saveAssessment(
        messageId: String,
        assessment: MessageSafetyAssessment
    )

    suspend fun deleteAllAssessments()

    suspend fun deleteAssessmentsForOtherAnalyzers()
}
