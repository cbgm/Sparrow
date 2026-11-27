package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupMessageUseCase

/** Loads a group message that may be pinned: a readable user message, never a system message. */
class FindPinnableGroupMessageUseCase(
    private val findGroupMessage: FindGroupMessageUseCase
) {
    suspend operator fun invoke(groupId: String, messageId: String): Result<StoredMessage> =
        safeSuspendCall(logFailure = false) {
            val message = findGroupMessage(groupId, messageId).getOrThrow()
            check(message.isGroupUserMessage) { "Only group user messages can be pinned" }
            check(message.contentStatus == MessageContentStatus.READABLE) { "Only readable messages can be pinned" }
            message
        }
}
