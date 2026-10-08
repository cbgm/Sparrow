package com.cbgm.sparrow.feature.chats.domain.usecase.forward

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.model.ForwardMessageContent

class PrepareForwardMessageUseCase {
    suspend operator fun invoke(parts: List<MessagePart>): Result<ForwardMessageContent> =
        safeSuspendCall {
            val forwarded =
                parts.onEach { part ->
                    require(part !is Poll) { "Polls cannot be forwarded" }
                }

            require(forwarded.isNotEmpty()) {
                "Message has no forwardable content"
            }

            ForwardMessageContent(parts = forwarded)
        }
}
