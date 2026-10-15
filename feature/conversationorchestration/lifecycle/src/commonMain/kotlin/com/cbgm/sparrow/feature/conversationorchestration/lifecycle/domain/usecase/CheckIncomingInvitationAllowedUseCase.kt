package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository

/**
 * Blocked contacts never reach the inbox; unknown senders only when the user allows invitations from them.
 * An explicit invitation is deliverable in both identity-setup modes.
 */
class CheckIncomingInvitationAllowedUseCase(
    private val blocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(peerId: String, wasKnownPeerAtReceive: Boolean?): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            val blocked = blocklistRepository.isBlocked(peerId).getOrThrow()
            val blockUnknown = blocklistRepository.getBlockUnknownContactInvites().getOrThrow()
            val unknown = wasKnownPeerAtReceive == false
            !(blocked || (blockUnknown && unknown))
        }
}
