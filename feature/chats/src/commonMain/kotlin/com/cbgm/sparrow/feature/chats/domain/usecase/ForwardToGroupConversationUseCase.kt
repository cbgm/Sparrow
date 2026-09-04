package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.model.ForwardMessageContent
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SendGroupMessageUseCase

class ForwardToGroupConversationUseCase(
    private val sendGroupMessage: SendGroupMessageUseCase,
    private val conversationOverviewRepository: ConversationOverviewRepository
) {
    suspend operator fun invoke(
        groupId: String,
        content: ForwardMessageContent
    ): Result<Unit> =
        safeSuspendCall {
            sendGroupMessage(
                groupId = groupId,
                text = content.text,
                attachments = content.attachments
            ).getOrThrow()

            conversationOverviewRepository
                .incrementUnseenLocalMessageCount(groupId)
                .getOrThrow()
        }
}
