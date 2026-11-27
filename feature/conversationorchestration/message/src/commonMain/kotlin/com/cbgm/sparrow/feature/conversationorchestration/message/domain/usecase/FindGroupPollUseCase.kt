package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.base.messagepart.domain.model.PollPolicy
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository

/** Loads one poll part of a group message. */
class FindGroupPollUseCase(
    private val attachmentRepository: MessageAttachmentOperationsRepository
) {
    suspend operator fun invoke(messageId: String, pollId: String): Result<Poll> =
        safeSuspendCall(logFailure = false) {
            require(pollId.isNotBlank()) { "Poll ID must not be blank" }
            attachmentRepository
                .messageParts(messageId)
                .getOrThrow()
                .filterIsInstance<Poll>()
                .singleOrNull { poll -> poll.id == pollId }
                ?: error("Poll was not found")
        }

    /**
     * The poll as it is once closed at [closedAtEpochMilliseconds] ([PollPolicy.close]); nothing is saved. A poll
     * that is closed already fails here, before the close is sent.
     */
    suspend fun closing(messageId: String, pollId: String, closedAtEpochMilliseconds: Long): Result<Poll> =
        safeSuspendCall(logFailure = false) {
            PollPolicy.close(invoke(messageId, pollId).getOrThrow(), closedAtEpochMilliseconds)
        }
}
