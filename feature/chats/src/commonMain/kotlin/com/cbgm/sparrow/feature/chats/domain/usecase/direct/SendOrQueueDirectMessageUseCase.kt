package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessageDispatchResult
import com.cbgm.sparrow.feature.chats.domain.model.direct.isDirectReinvitePending
import com.cbgm.sparrow.feature.chats.domain.model.direct.isDirectReinviteRetryState
import com.cbgm.sparrow.feature.contacts.domain.model.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase
import kotlinx.coroutines.flow.first

class SendOrQueueDirectMessageUseCase(
    private val sendDirectMessage: SendDirectMessageUseCase,
    private val queueDirectMessageUntilAuthorized: QueueDirectMessageUntilAuthorizedUseCase,
    private val getOrCreateDirectConversation: GetOrCreateDirectConversationUseCase,
    private val ensureIdentityExchangeStarted: EnsureIdentityExchangeStartedUseCase,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val observeIdentityHandshakeState: ObserveIdentityHandshakeStateUseCase,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
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

            if (contactBlocklistRepository.isBlocked(contactId)) {
                throw DirectChatAuthorizationRequiredException(
                    "Blocked contacts cannot send or receive direct messages"
                )
            }

            val authorizationError =
                requireDirectChatAuthorization(contactId)
                    .exceptionOrNull()
            val delivery =
                resolveDelivery(
                    authorizationError = authorizationError,
                    contactId = contactId,
                    hasConversation = conversationId != null
                )
            val resolvedConversationId =
                conversationId
                    ?: getOrCreateDirectConversation(contactId).getOrThrow()

            when (delivery) {
                DirectDelivery.SEND -> {
                    sendDirectMessage(
                        conversationId = resolvedConversationId,
                        text = text,
                        attachments = attachments,
                        replyToMessageId = replyToMessageId
                    ).getOrThrow()
                    DirectMessageDispatchResult.Sent
                }

                DirectDelivery.QUEUE -> {
                    queueDirectMessageUntilAuthorized(
                        conversationId = resolvedConversationId,
                        text = text,
                        attachments = attachments,
                        replyToMessageId = replyToMessageId
                    ).getOrThrow()
                    DirectMessageDispatchResult.Queued
                }

                DirectDelivery.QUEUE_AND_START_IDENTITY_EXCHANGE -> {
                    queueDirectMessageUntilAuthorized(
                        conversationId = resolvedConversationId,
                        text = text,
                        attachments = attachments,
                        replyToMessageId = replyToMessageId
                    ).getOrThrow()

                    ensureIdentityExchangeStarted(contactId)
                        .fold(
                            onSuccess = { DirectMessageDispatchResult.Queued },
                            onFailure = { error ->
                                DirectMessageDispatchResult.QueuedWithIdentityExchangeFailure(error)
                            }
                        )
                }
            }
        }

    private suspend fun resolveDelivery(
        authorizationError: Throwable?,
        contactId: String,
        hasConversation: Boolean
    ): DirectDelivery {
        if (authorizationError == null) return DirectDelivery.SEND
        if (
            identitySetupModeRepository.getMode() != DirectIdentitySetupMode.AUTOMATIC_INVITATION ||
            authorizationError !is DirectChatAuthorizationRequiredException
        ) {
            throw authorizationError
        }

        val handshake = observeIdentityHandshakeState(contactId).first()
        return when {
            handshake.isDirectReinvitePending() -> DirectDelivery.QUEUE
            handshake.isDirectReinviteRetryState() -> DirectDelivery.QUEUE_AND_START_IDENTITY_EXCHANGE
            !hasConversation && handshake == null -> DirectDelivery.QUEUE_AND_START_IDENTITY_EXCHANGE
            else -> throw authorizationError
        }
    }

    private enum class DirectDelivery {
        SEND,
        QUEUE,
        QUEUE_AND_START_IDENTITY_EXCHANGE
    }
}
