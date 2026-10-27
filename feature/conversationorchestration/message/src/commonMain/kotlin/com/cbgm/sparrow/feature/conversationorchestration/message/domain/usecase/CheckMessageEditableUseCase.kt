package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/**
 * Why a message's content cannot be edited, or null when it can: only plain text messages without
 * attachments are editable. Callers raise the reason with their own failure type.
 */
class CheckMessageEditableUseCase(
    private val directMessageRepository: DirectMessageRepository,
    private val groupMessageRepository: GroupMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository
) {
    suspend fun forDirectMessage(messageId: String): Result<String?> =
        check(messageId) { directMessageRepository.findText(messageId).getOrThrow() }

    suspend fun forGroupMessage(messageId: String): Result<String?> =
        check(messageId) { groupMessageRepository.findText(messageId).getOrThrow() }

    private suspend fun check(messageId: String, findText: suspend () -> String?): Result<String?> =
        safeSuspendCall(logFailure = false) {
            when {
                findText().isNullOrBlank() -> NOT_TEXT
                attachmentRepository.messageParts(messageId).getOrThrow().isNotEmpty() -> HAS_ATTACHMENTS
                else -> null
            }
        }

    private companion object {
        const val NOT_TEXT = "Only text messages can be edited"
        const val HAS_ATTACHMENTS = "Messages with attachments cannot be edited"
    }
}
