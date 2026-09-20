package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.IdentityPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.identity.domain.usecase.HandleIdentityVerificationReceiptUseCase

internal class IdentityExchangePacketObserver(
    private val flowHandler: ConversationFlowHandler,
    private val handleContactVerificationReceipt: HandleIdentityVerificationReceiptUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean =
        packet is ContactInvitePacket ||
            packet is ContactInviteAcceptedPacket ||
            packet is ContactInviteDeclinedPacket ||
            packet is ContactReadyPacket ||
            packet is DirectChatAuthorizationRevokedPacket ||
            packet is IdentityPacket ||
            packet is IdentityAcknowledgementPacket ||
            packet is ContactVerificationReceiptPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        if (packet is ContactVerificationReceiptPacket) {
            handleContactVerificationReceipt(context, packet)
        } else {
            flowHandler.onIdentityPacket(context, packet)
        }
}
