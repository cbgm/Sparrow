package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyRepository

class ProcessMessageSafetyBatchUseCase(
    private val repository: MessageSafetyRepository,
    private val analyzeMessageSafety: AnalyzeMessageSafetyUseCase
) {
    suspend operator fun invoke(limit: Int): Int {
        val messages = repository.getUnassessedMessages(limit)
        messages.forEach { message ->
            repository.saveAssessment(
                messageId = message.messageId,
                assessment = analyzeMessageSafety(message.text)
            )
        }
        return messages.size
    }
}
