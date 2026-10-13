package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository

/** Loads one poll part of a group message. */
class FindGroupPollUseCase(
    private val attachmentRepository: MessageAttachmentOperationsRepository
) {
    suspend operator fun invoke(messageId: String, pollId: String): Result<Poll> =
        runCatching {
            require(pollId.isNotBlank()) { "Poll ID must not be blank" }
            attachmentRepository
                .messageParts(messageId)
                .getOrThrow()
                .filterIsInstance<Poll>()
                .singleOrNull { poll -> poll.id == pollId }
                ?: error("Poll was not found")
        }
}
