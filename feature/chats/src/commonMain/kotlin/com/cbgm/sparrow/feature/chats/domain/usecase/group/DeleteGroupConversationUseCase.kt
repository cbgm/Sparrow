package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMembershipRepository

class DeleteGroupConversationUseCase(
    private val repository: GroupMembershipRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> =
        runCatching {
            messageAttachmentRepository.deleteLocalAttachmentsForConversation(groupId).getOrThrow()
            repository.delete(groupId).getOrThrow()
        }
}
