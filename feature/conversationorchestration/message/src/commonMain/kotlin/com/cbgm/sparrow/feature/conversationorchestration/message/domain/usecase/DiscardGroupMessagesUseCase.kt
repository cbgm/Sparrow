package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/** Removes group messages together with their local attachments. */
class DiscardGroupMessagesUseCase(
    private val messageRepository: GroupMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository
) {
    suspend operator fun invoke(messages: List<StoredMessage>): Result<Unit> =
        runCatching {
            if (messages.isEmpty()) return@runCatching
            attachmentRepository.deleteForMessages(messages.map(StoredMessage::id))
            messageRepository.delete(messages).getOrThrow()
        }
}
