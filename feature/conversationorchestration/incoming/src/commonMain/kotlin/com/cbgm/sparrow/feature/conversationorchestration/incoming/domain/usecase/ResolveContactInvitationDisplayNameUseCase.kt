package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.GetIdentityPeerDisplayNameUseCase
import com.cbgm.sparrow.protocol.packet.ContactInvitePacket

/** The name an incoming contact invitation is shown with: the known peer's name, else the name it claims. */
class ResolveContactInvitationDisplayNameUseCase(
    private val getIdentityPeerDisplayName: GetIdentityPeerDisplayNameUseCase
) {
    suspend operator fun invoke(peerId: String, packet: ContactInvitePacket): Result<String?> =
        safeSuspendCall(logFailure = false) {
            getIdentityPeerDisplayName(peerId).getOrThrow() ?: packet.claimedDisplayName()
        }
}
