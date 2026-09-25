package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.error.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.conversationorchestration.domain.model.ConversationMessagePlan
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationLifecycleStatusUseCase
import kotlinx.coroutines.flow.first

class PrepareConversationMessageUseCase internal constructor(
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val flowHandler: ConversationFlowHandler,
    private val observeInvitationLifecycleStatus: ObserveInvitationLifecycleStatusUseCase,
    private val observeIdentityHandshakeState: ObserveIdentityHandshakeStateUseCase,
    private val observePendingRemoteIdentityChanges: ObservePendingRemoteIdentityChangesUseCase,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(peerId: String): Result<ConversationMessagePlan> =
        safeSuspendCall {
            require(peerId.isNotBlank()) {
                "Peer ID must not be blank"
            }

            if (contactBlocklistRepository.isBlocked(peerId)) {
                throw DirectChatAuthorizationRequiredException(
                    "Blocked contacts cannot send or receive direct messages"
                )
            }

            // The previous exchange can remain MUTUAL until a user reviews a new
            // identity. Do not encrypt a fresh message for those obsolete keys or
            // automatically start another invitation while the review is pending.
            // Keep the user's text/attachments in WAITING_FOR_AUTHORIZATION instead.
            if (observePendingRemoteIdentityChanges().first().any { it.peerId == peerId }) {
                return@safeSuspendCall ConversationMessagePlan.Queue
            }

            val authorizationError =
                requireDirectChatAuthorization(peerId)
                    .exceptionOrNull()
                    ?: return@safeSuspendCall ConversationMessagePlan.Send

            if (authorizationError !is DirectChatAuthorizationRequiredException) {
                throw authorizationError
            }

            when (identitySetupModeRepository.getMode()) {
                DirectIdentitySetupMode.AUTOMATIC_INVITATION -> {
                    val invitationStatus =
                        observeInvitationLifecycleStatus(
                            payloadType = InvitationPayloadType.DIRECT,
                            payloadId = peerId,
                            peerId = peerId,
                            direction = InvitationDirection.OUTGOING
                        ).first()
                    val handshake = observeIdentityHandshakeState(peerId).first()

                    when {
                        invitationStatus == InvitationLifecycleStatus.PENDING ->
                            ConversationMessagePlan.Queue

                        invitationStatus in RETRYABLE_INVITATION_STATUSES ||
                            handshake in RETRYABLE_IDENTITY_STATES ||
                            handshake == null ->
                            flowHandler.requestReauthorization(peerId).fold(
                                onSuccess = { ConversationMessagePlan.Queue },
                                onFailure = { error ->
                                    ConversationMessagePlan.QueueWithAuthorizationFailure(error)
                                }
                            )

                        else -> throw authorizationError
                    }
                }

                // Sending a message is an explicit user action and may start a
                // SIGNED INVITATION in manual mode, too. The invite advertises
                // autoSharesIdentity=false: this does not silently import keys or
                // send an IdentityPacket. Such manual sharing stays user-controlled.
                DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING -> {
                    val invitationStatus =
                        observeInvitationLifecycleStatus(
                            payloadType = InvitationPayloadType.DIRECT,
                            payloadId = peerId,
                            peerId = peerId,
                            direction = InvitationDirection.OUTGOING
                        ).first()
                    if (invitationStatus == InvitationLifecycleStatus.PENDING) {
                        ConversationMessagePlan.Queue
                    } else {
                        flowHandler.requestReauthorization(peerId).fold(
                            onSuccess = { ConversationMessagePlan.Queue },
                            onFailure = { error ->
                                ConversationMessagePlan.QueueWithAuthorizationFailure(error)
                            }
                        )
                    }
                }
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
                IdentityHandshakeState.EXCHANGE_INVALIDATED,
                IdentityHandshakeState.FAILED
            )
    }
}
