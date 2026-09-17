package com.cbgm.sparrow.feature.conversationorchestration.data.outbox

import com.cbgm.sparrow.feature.conversationorchestration.domain.port.OrchestratedOutboxDeliveryPort
import com.cbgm.sparrow.feature.invite.data.outbox.InvitationOutboxDeliveryHandler

internal class InvitationOutboxDeliveryPort(
    private val invitationOutboxDeliveryHandler: InvitationOutboxDeliveryHandler
) : OrchestratedOutboxDeliveryPort {
    override fun canHandle(packetId: String): Boolean =
        invitationOutboxDeliveryHandler.canHandle(packetId)

    override suspend fun onFailed(packetId: String) {
        invitationOutboxDeliveryHandler.onFailed(packetId)
    }
}
