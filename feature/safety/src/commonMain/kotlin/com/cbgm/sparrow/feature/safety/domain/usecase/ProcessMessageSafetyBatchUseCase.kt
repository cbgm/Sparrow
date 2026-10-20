package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyRepository

class ProcessMessageSafetyBatchUseCase(
    private val repository: MessageSafetyRepository,
    private val analyzeMessageSafety: AnalyzeMessageSafetyUseCase
) {
    suspend operator fun invoke(limit: Int): Result<Int> =
        safeSuspendCall(logFailure = false) {
            val messages = repository.getUnassessedMessages(limit).getOrThrow()
            messages.forEach { message ->
                repository.saveAssessment(
                    messageId = message.messageId,
                    assessment = analyzeMessageSafety(message.text).getOrThrow()
                ).getOrThrow()
            }
            messages.size
        }
}
