package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.model.ConversationMessagePlan
import com.cbgm.sparrow.feature.identity.domain.model.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationLifecycleStatusUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.SendInvitationUseCase
import kotlinx.coroutines.flow.first

class PrepareConversationMessageUseCase(
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val sendInvitation: SendInvitationUseCase,
    private val observeInvitationLifecycleStatus: ObserveInvitationLifecycleStatusUseCase,
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(
        peerId: String,
        hasConversation: Boolean
    ): Result<ConversationMessagePlan> =
        safeSuspendCall {
            require(peerId.isNotBlank()) {
                "Peer ID must not be blank"
            }

            if (contactBlocklistRepository.isBlocked(peerId)) {
                throw DirectChatAuthorizationRequiredException(
                    "Blocked contacts cannot send or receive direct messages"
                )
            }

            val authorizationError =
                requireDirectChatAuthorization(peerId)
                    .exceptionOrNull()
                    ?: return@safeSuspendCall ConversationMessagePlan.Send

            if (
                identitySetupModeRepository.getMode() != DirectIdentitySetupMode.AUTOMATIC_INVITATION ||
                authorizationError !is DirectChatAuthorizationRequiredException
            ) {
                throw authorizationError
            }

            val invitationStatus =
                observeInvitationLifecycleStatus(
                    payloadType = InvitationPayloadType.DIRECT,
                    payloadId = peerId,
                    peerId = peerId,
                    direction = InvitationDirection.OUTGOING
                ).first()
            val handshake = directIdentityExchangeRepository.observeState(peerId).first()

            when {
                invitationStatus == InvitationLifecycleStatus.PENDING ->
                    ConversationMessagePlan.Queue

                invitationStatus in RETRYABLE_INVITATION_STATUSES ||
                    handshake in RETRYABLE_IDENTITY_STATES ||
                    (!hasConversation && handshake == null) ->
                    sendInvitation(
                        payloadType = InvitationPayloadType.DIRECT,
                        payloadId = peerId,
                        peerIds = setOf(peerId)
                    ).fold(
                        onSuccess = { ConversationMessagePlan.Queue },
                        onFailure = { error ->
                            ConversationMessagePlan.QueueWithAuthorizationFailure(error)
                        }
                    )

                else -> throw authorizationError
            }
        }

    private companion object {
        val RETRYABLE_INVITATION_STATUSES =
            setOf(
                InvitationLifecycleStatus.DECLINED,
                InvitationLifecycleStatus.EXPIRED,
                InvitationLifecycleStatus.FAILED
            )

        val RETRYABLE_IDENTITY_STATES =
            setOf(
                IdentityHandshakeState.AUTHORIZATION_REVOKED,
                IdentityHandshakeState.FAILED
            )
    }
}
