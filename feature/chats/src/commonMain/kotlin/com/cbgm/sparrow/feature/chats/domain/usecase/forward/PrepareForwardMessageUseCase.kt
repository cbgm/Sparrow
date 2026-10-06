package com.cbgm.sparrow.feature.chats.domain.usecase.forward

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.model.ForwardMessageContent

class PrepareForwardMessageUseCase {
    suspend operator fun invoke(parts: List<MessagePart>): Result<ForwardMessageContent> =
        safeSuspendCall {
            val text =
                parts
                    .filterIsInstance<Text>()
                    .joinToString(separator = "\n", transform = Text::text)

            val forwarded =
                parts.filterNot { part -> part is Text }
                    .onEach { part -> require(part !is Poll) { "Polls cannot be forwarded" } }

            require(text.isNotBlank() || forwarded.isNotEmpty()) {
                "Message has no forwardable content"
            }

            ForwardMessageContent(
                text = text,
                parts = forwarded
            )
        }
}
