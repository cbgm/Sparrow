package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.indicator

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.protocol.packet.IndicatorPacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket

/** Shows an indicator that arrived in a mailbox; one sent too long ago would show a state that is over. */
class IndicatorPacketHandler(
    private val indicators: MailboxMessagingIndicatorGateway
) : TypedProtocolPacketHandler {
    private val logger = SparrowLog.withTag("IndicatorPacketHandler")

    override fun canHandle(packet: SparrowPacket): Boolean = packet is IndicatorPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val indicator = packet as IndicatorPacket
            val age = context.receivedAtEpochMilliseconds - indicator.sentAtEpochMilliseconds
            if (age in -MAXIMUM_CLOCK_SKEW_MILLISECONDS..MAXIMUM_AGE_MILLISECONDS) {
                logger.debug { "Indicator ${indicator.indicatorType} from ${context.contactId} arrived after $age ms" }
                indicators.receive(MessagingIndicator(context.contactId, indicator.groupId, indicator.indicatorType))
            } else {
                logger.info { "Indicator ${indicator.indicatorType} from ${context.contactId} ignored: $age ms old" }
            }
        }

    private companion object {
        const val MAXIMUM_AGE_MILLISECONDS = 10_000L
        const val MAXIMUM_CLOCK_SKEW_MILLISECONDS = 60_000L
    }
}
