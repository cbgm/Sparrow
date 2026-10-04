package com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.invite.data.outbox.InvitationOutboxDeliveryHandler
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.protocol.codec.PacketCodec
import com.cbgm.sparrow.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.protocol.packet.GroupInvitePacket

/** The application interprets failed packets; Messaging only manages transport state. */
class InvitationTransportFailureHandler(
    private val packetCodec: PacketCodec,
    private val invitationOutboxDeliveryHandler: InvitationOutboxDeliveryHandler
) {
    suspend fun onFailed(encodedPacket: ByteArray): Result<Unit> =
        runCatching {
            when (
                val packet = packetCodec.decode(encodedPacket)
                    .onFailure { failure -> SparrowLog.error("InvitationTransportFailureHandler", "Could not decode failed invitation packet", failure) }
                    .getOrNull()
            ) {
                is ContactInvitePacket ->
                    invitationOutboxDeliveryHandler.onFailed(
                        payloadType = InvitationPayloadType.DIRECT,
                        invitationId = packet.invitationId
                    )

                is GroupInvitePacket ->
                    invitationOutboxDeliveryHandler.onFailed(
                        payloadType = InvitationPayloadType.GROUP,
                        invitationId = packet.invitationId
                    )

                else -> Unit
            }
        }
}
