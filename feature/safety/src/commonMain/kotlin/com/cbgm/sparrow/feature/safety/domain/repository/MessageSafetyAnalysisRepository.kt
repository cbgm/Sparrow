package com.cbgm.sparrow.feature.safety.domain.repository

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason

interface MessageSafetyAnalysisRepository {
    suspend fun analyze(text: String): Set<MessageSafetyReason>
}
