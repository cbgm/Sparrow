package com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.identity.DirectIdentityExchangeCoordinator
import com.cbgm.sparrow.feature.invite.data.lifecycle.InvitationLifecycleEffects
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction

internal class DirectInvitationLifecycleEffects(
    private val coordinator: DirectIdentityExchangeCoordinator
) : InvitationLifecycleEffects {
    override val payloadType: InvitationPayloadType = InvitationPayloadType.DIRECT

    override suspend fun send(
        payloadId: String,
        peerIds: Set<String>
    ): Result<List<InvitationLifecycleRecord>> =
        safeSuspendCall {
            require(peerIds.size == 1) { "A direct invitation requires exactly one peer" }
            val peerId = peerIds.single()
            require(payloadId == peerId) { "Direct invitation payload ID must match its peer ID" }
            listOfNotNull(coordinator.startInvitation(peerId).getOrThrow())
        }

    override suspend fun accept(invitationId: String): Result<Unit> =
        coordinator.accept(invitationId)

    override suspend fun decline(
        invitationId: String,
        action: InvitationResultAction?
    ): Result<Unit> = coordinator.decline(invitationId)
}
