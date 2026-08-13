package com.cbgm.securechat.feature.chats.data.group.delivery

import com.cbgm.securechat.feature.chats.data.group.membership.GroupInvitationCoordinator
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent

class GroupOutboxDeliveryHandler internal constructor(
    private val deliveryCoordinator: GroupMessageDeliveryCoordinator,
    private val invitationCoordinator: GroupInvitationCoordinator
) {
    suspend fun canHandle(packetId: String): Boolean =
        packetId.isGroupInvitePacketId() || deliveryCoordinator.handlesPacket(packetId)

    suspend fun applyEvent(
        packetId: String,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ) {
        if (packetId.isGroupInvitePacketId()) {
            if (event == MessageDeliveryEvent.SEND_FAILED) {
                invitationCoordinator.markInvitationTransportFailed(packetId)
            }
            return
        }
        deliveryCoordinator.applyPacketEvent(packetId, event, errorMessage)
    }

    private fun String.isGroupInvitePacketId(): Boolean =
        startsWith(GROUP_INVITE_PACKET_ID_PREFIX) &&
            !startsWith(GROUP_INVITE_RECEIVED_PACKET_ID_PREFIX)

    private companion object {
        const val GROUP_INVITE_PACKET_ID_PREFIX = "group-invite-"
        const val GROUP_INVITE_RECEIVED_PACKET_ID_PREFIX = "group-invite-received-"
    }
}
