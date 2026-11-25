package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository

/** The bytes of an attachment of the message pinned in a group; Attachments checks that it belongs to the pin. */
class LoadGroupPinnedAttachmentUseCase(
    private val repository: MessageAttachmentRepository
) {
    suspend operator fun invoke(
        groupId: String,
        attachmentId: String
    ): Result<ByteArray> =
        repository.loadBytes(partId = attachmentId, groupId = groupId)
}
