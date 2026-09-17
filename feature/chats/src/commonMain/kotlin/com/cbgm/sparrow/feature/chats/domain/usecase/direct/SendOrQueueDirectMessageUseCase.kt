package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessageDispatchResult
import com.cbgm.sparrow.feature.conversationorchestration.domain.model.ConversationMessagePlan
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.PrepareConversationMessageUseCase

class SendOrQueueDirectMessageUseCase(
    private val sendDirectMessage: SendDirectMessageUseCase,
    private val queueDirectMessageUntilAuthorized: QueueDirectMessageUntilAuthorizedUseCase,
    private val getOrCreateDirectConversation: GetOrCreateDirectConversationUseCase,
    private val prepareConversationMessage: PrepareConversationMessageUseCase
) {
    suspend operator fun invoke(
        contactId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment> = emptyList(),
        replyToMessageId: String? = null,
        conversationId: String? = null
    ): Result<DirectMessageDispatchResult> =
        safeSuspendCall {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val plan =
                prepareConversationMessage(
                    peerId = contactId,
                    hasConversation = conversationId != null
                ).getOrThrow()
            val resolvedConversationId =
                conversationId
                    ?: getOrCreateDirectConversation(contactId).getOrThrow()

            when (plan) {
                ConversationMessagePlan.Send -> {
                    sendDirectMessage(
                        conversationId = resolvedConversationId,
                        text = text,
                        attachments = attachments,
                        replyToMessageId = replyToMessageId
                    ).getOrThrow()
                    DirectMessageDispatchResult.Sent
                }

                ConversationMessagePlan.Queue -> {
                    queueDirectMessageUntilAuthorized(
                        conversationId = resolvedConversationId,
                        text = text,
                        attachments = attachments,
                        replyToMessageId = replyToMessageId
                    ).getOrThrow()
                    DirectMessageDispatchResult.Queued
                }

                is ConversationMessagePlan.QueueWithAuthorizationFailure -> {
                    queueDirectMessageUntilAuthorized(
                        conversationId = resolvedConversationId,
                        text = text,
                        attachments = attachments,
                        replyToMessageId = replyToMessageId
                    ).getOrThrow()
                    DirectMessageDispatchResult.QueuedWithIdentityExchangeFailure(plan.throwable)
                }
            }
        }
}
