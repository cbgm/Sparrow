package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinReadRepository

class LoadGroupPinnedAttachmentUseCase(
    private val repository: GroupPinReadRepository
) {
    suspend operator fun invoke(
        groupId: String,
        attachmentId: String
    ): Result<ByteArray> =
        repository.loadAttachment(groupId, attachmentId)
}
