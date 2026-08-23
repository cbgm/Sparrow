package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.attachment.OutgoingMediaAttachment
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository

class SendGroupMessageUseCase(
    private val repository: GroupMessageRepository
) {
    suspend operator fun invoke(
        groupId: String,
        text: String,
        media: List<OutgoingMediaAttachment> = emptyList()
    ): Result<Unit> =
        repository.send(groupId, text, media)
}
