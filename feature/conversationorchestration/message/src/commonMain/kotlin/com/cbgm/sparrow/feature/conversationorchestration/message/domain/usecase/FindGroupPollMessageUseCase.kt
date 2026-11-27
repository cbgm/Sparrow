package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage

/** Loads the group message a local poll vote or close targets; polls only live in user messages. */
class FindGroupPollMessageUseCase(
    private val findGroupMessage: FindGroupMessageUseCase
) {
    suspend operator fun invoke(groupId: String, messageId: String): Result<StoredMessage> =
        safeSuspendCall(logFailure = false) {
            val message = findGroupMessage(groupId, messageId).getOrThrow()
            check(message.isGroupUserMessage) { "Poll target is not a user message" }
            message
        }
}
