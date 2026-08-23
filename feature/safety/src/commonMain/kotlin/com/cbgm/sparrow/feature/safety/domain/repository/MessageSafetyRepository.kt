package com.cbgm.sparrow.feature.safety.domain.repository

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyState
import kotlinx.coroutines.flow.StateFlow

interface MessageSafetyRepository {
    val state: StateFlow<MessageSafetyState>
    val assessments: StateFlow<Map<String, MessageSafetyAssessment>>

    suspend fun initialize()
}
