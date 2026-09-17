package com.cbgm.sparrow.feature.invite.data.outbox

import com.cbgm.sparrow.feature.invite.data.group.GroupInvitationLifecycleCoordinator

class InvitationOutboxDeliveryHandler internal constructor(
    private val groupCoordinator: GroupInvitationLifecycleCoordinator
) {
    fun canHandle(packetId: String): Boolean =
        packetId.startsWith(GROUP_INVITE_PACKET_ID_PREFIX) &&
            !packetId.startsWith(GROUP_INVITE_RECEIVED_PACKET_ID_PREFIX)

    suspend fun onFailed(packetId: String) {
        if (!canHandle(packetId)) return
        groupCoordinator.markTransportFailed(packetId)
    }

    private companion object {
        const val GROUP_INVITE_PACKET_ID_PREFIX = "group-invite-"
        const val GROUP_INVITE_RECEIVED_PACKET_ID_PREFIX = "group-invite-received-"
    }
}
