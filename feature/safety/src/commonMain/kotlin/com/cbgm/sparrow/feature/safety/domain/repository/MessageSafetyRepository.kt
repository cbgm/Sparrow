package com.cbgm.sparrow.feature.safety.domain.repository

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyCandidate
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MessageSafetyRepository {
    val state: StateFlow<MessageSafetyState>
    val assessments: StateFlow<Map<String, MessageSafetyAssessment>>

    fun updateState(state: MessageSafetyState): Result<Unit>

    suspend fun getUnassessedMessages(limit: Int): Result<List<MessageSafetyCandidate>>

    suspend fun getUnassessedMessageCount(): Result<Int>

    fun observeUnassessedMessageCount(): Flow<Int>

    suspend fun saveAssessment(
        messageId: String,
        assessment: MessageSafetyAssessment
    ): Result<Unit>

    suspend fun deleteAllAssessments(): Result<Unit>

    suspend fun deleteAssessmentsForOtherAnalyzers(): Result<Unit>
}
