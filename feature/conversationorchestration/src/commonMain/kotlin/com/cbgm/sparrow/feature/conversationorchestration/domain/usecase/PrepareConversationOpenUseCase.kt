package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.SendInvitationUseCase

class PrepareConversationOpenUseCase(
    private val conversationPort: ConversationPort,
    private val ensureIdentityExchangeStarted: EnsureIdentityExchangeStartedUseCase,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val sendInvitation: SendInvitationUseCase,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository
) {
    suspend operator fun invoke(peerId: String): Result<String?> =
        safeSuspendCall {
            require(peerId.isNotBlank()) {
                "Peer ID must not be blank"
            }

            when (identitySetupModeRepository.getMode()) {
                DirectIdentitySetupMode.AUTOMATIC_INVITATION -> {
                    if (requireDirectChatAuthorization(peerId).isSuccess) {
                        conversationPort.getOrCreateConversation(peerId).getOrThrow()
                    } else {
                        sendInvitation(
                            payloadType = InvitationPayloadType.DIRECT,
                            payloadId = peerId,
                            peerIds = setOf(peerId)
                        )
                        null
                    }
                }

                DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING -> {
                    val conversationId =
                        conversationPort
                            .getOrCreateConversation(peerId)
                            .getOrThrow()
                    ensureIdentityExchangeStarted(peerId)
                    conversationId
                }
            }
        }
}
