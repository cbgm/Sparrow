package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.feature.chats.domain.model.ForwardMessageContent
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SendGroupMessageUseCase

class ForwardToGroupConversationUseCase(
    private val sendGroupMessage: SendGroupMessageUseCase
) {
    suspend operator fun invoke(
        groupId: String,
        content: ForwardMessageContent
    ): Result<Unit> =
        sendGroupMessage(
            groupId = groupId,
            text = content.text,
            attachments = content.attachments
        )
}
