package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.model.ForwardingTarget
import com.cbgm.sparrow.feature.chats.domain.model.MessagePart

class ForwardMessageUseCase(
    private val prepareForwardMessage: PrepareForwardMessageUseCase,
    private val forwardToDirectConversation: ForwardToDirectConversationUseCase,
    private val forwardToGroupConversation: ForwardToGroupConversationUseCase,
    private val forwardToContact: ForwardToContactUseCase
) {
    suspend operator fun invoke(
        parts: List<MessagePart>,
        target: ForwardingTarget
    ): Result<Unit> =
        safeSuspendCall {
            val content = prepareForwardMessage(parts).getOrThrow()

            when (target) {
                is ForwardingTarget.Direct ->
                    forwardToDirectConversation(
                        conversationId = target.conversationId,
                        content = content
                    ).getOrThrow()

                is ForwardingTarget.Group ->
                    forwardToGroupConversation(
                        groupId = target.groupId,
                        content = content
                    ).getOrThrow()

                is ForwardingTarget.Contact ->
                    forwardToContact(
                        contactId = target.contactId,
                        content = content
                    ).getOrThrow()
            }
        }
}
