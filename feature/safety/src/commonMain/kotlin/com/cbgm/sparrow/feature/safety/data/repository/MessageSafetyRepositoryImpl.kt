package com.cbgm.sparrow.feature.safety.data.repository

import com.cbgm.sparrow.core.util.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.util.result.safeCall
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.safety.data.datasource.MessageSafetyLocalDataSource
import com.cbgm.sparrow.feature.safety.data.mapper.toMessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.data.mapper.toMessageSafetyAssessmentEntity
import com.cbgm.sparrow.feature.safety.data.mapper.toMessageSafetyCandidate
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyCandidate
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyState
import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MessageSafetyRepositoryImpl(
    private val localDataSource: MessageSafetyLocalDataSource,
    applicationScope: ApplicationCoroutineScope
) : MessageSafetyRepository {
    private val mutableState = MutableStateFlow<MessageSafetyState>(MessageSafetyState.Disabled)
    override val state: StateFlow<MessageSafetyState> = mutableState

    override val assessments: StateFlow<Map<String, MessageSafetyAssessment>> =
        combine(
            localDataSource.observeVisibleAssessments(ANALYZER_VERSION),
            state
        ) { stored, currentState ->
            if (currentState == MessageSafetyState.Disabled) {
                emptyMap()
            } else {
                stored.associate { assessment -> assessment.messageId to assessment.toMessageSafetyAssessment() }
            }
        }.stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    override fun updateState(state: MessageSafetyState): Result<Unit> =
        safeCall {
            mutableState.value = state
        }

    override suspend fun getUnassessedMessages(limit: Int): Result<List<MessageSafetyCandidate>> =
        safeSuspendCall {
            localDataSource
                .getMessagesMissingAssessment(
                    analyzerVersion = ANALYZER_VERSION,
                    limit = limit
                ).map { source -> source.toMessageSafetyCandidate() }
        }

    override suspend fun getUnassessedMessageCount(): Result<Int> =
        safeSuspendCall { localDataSource.getUnassessedMessageCount(ANALYZER_VERSION) }

    override fun observeUnassessedMessageCount(): Flow<Int> =
        localDataSource.observeUnassessedMessageCount(ANALYZER_VERSION)

    override suspend fun saveAssessment(
        messageId: String,
        assessment: MessageSafetyAssessment
    ): Result<Unit> =
        safeSuspendCall {
            localDataSource.upsertAssessment(
                assessment.toMessageSafetyAssessmentEntity(
                    messageId = messageId,
                    analyzerVersion = ANALYZER_VERSION
                )
            )
        }

    override suspend fun deleteAllAssessments(): Result<Unit> =
        safeSuspendCall {
            localDataSource.deleteAllAssessments()
        }

    override suspend fun deleteAssessmentsForOtherAnalyzers(): Result<Unit> =
        safeSuspendCall {
            localDataSource.deleteAssessmentsForOtherAnalyzers(ANALYZER_VERSION)
        }

    private companion object {
        const val ANALYZER_VERSION = 4
    }
}
