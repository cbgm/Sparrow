package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

class PrepareConversationOpenUseCase internal constructor(
    private val conversationPort: ConversationPort,
    private val ensureIdentityExchangeStarted: EnsureIdentityExchangeStartedUseCase,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val flowHandler: ConversationFlowHandler,
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
                        flowHandler.startDirectInvitation(peerId).getOrThrow()
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
