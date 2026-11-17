package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.indicator

import com.cbgm.sparrow.core.crypto.transport.TransportPayloadCodec
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.outbox.OutgoingTransportPayloadFactory
import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import com.cbgm.sparrow.feature.messaging.runtime.indicator.MessagingIndicatorGateway
import com.cbgm.sparrow.feature.transport.circuit.DirectSendingPolicy
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.protocol.codec.PacketCodec
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.protocol.packet.IndicatorPacket
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Indicators go into the contact's mailbox through the circuit, encrypted and padded like messages and without
 * routing IDs, so no node learns who is typing to whom. They are never queued: without a route to the contact, or
 * while sending directly is switched off and no circuit protects, the indicator is dropped. The envelope lives
 * [LIFETIME_MILLISECONDS], and the mailbox does not wake the recipient for it.
 */
class MailboxMessagingIndicatorGateway(
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val getContact: GetContactUseCase,
    private val packetCodec: PacketCodec,
    private val transportPayloadFactory: OutgoingTransportPayloadFactory,
    private val transportPayloadCodec: TransportPayloadCodec,
    private val mailboxGateway: MailboxGateway,
    private val sendingPolicy: DirectSendingPolicy
) : MessagingIndicatorGateway {
    private val logger = SparrowLog.withTag("MailboxMessagingIndicatorGateway")
    private val received = MutableSharedFlow<MessagingIndicator>(extraBufferCapacity = BUFFERED_INDICATORS, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val incoming: Flow<MessagingIndicator> = received.asSharedFlow()

    /** An indicator that arrived in a mailbox ([IndicatorPacketHandler]). */
    fun receive(indicator: MessagingIndicator) {
        received.tryEmit(indicator)
    }

    override suspend fun send(
        recipientContactId: String,
        groupId: String?,
        indicatorType: String
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            if (runCatching { sendingPolicy.checkUnprotectedSendAllowed() }.isFailure) {
                logger.info { "Indicator $indicatorType not sent to $recipientContactId: sending directly is off and no circuit protects" }
                return@safeSuspendCall
            }
            val now = SystemClock.nowEpochMilliseconds()
            val route =
                mailboxRouteRepository
                    .remoteForContact(recipientContactId)
                    .getOrThrow()
                    ?.takeIf { route -> route.expiresAtEpochMilliseconds > now + LIFETIME_MILLISECONDS }
            if (route == null) {
                logger.info { "Indicator $indicatorType not sent to $recipientContactId: no mailbox route to this contact yet" }
                return@safeSuspendCall
            }
            val contact =
                getContact(recipientContactId).getOrThrow() ?: run {
                    logger.info { "Indicator $indicatorType not sent: contact $recipientContactId not found" }
                    return@safeSuspendCall
                }
            val packet = IndicatorPacket(packetId = IdGenerator.generate(), indicatorType = indicatorType, groupId = groupId, sentAtEpochMilliseconds = now)
            val payload = transportPayloadFactory.create(packetCodec.encode(packet).getOrThrow(), packet, contact).getOrThrow()
            mailboxGateway
                .deliver(
                    FederatedEnvelope(
                        envelopeId = IdGenerator.generate(),
                        senderRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
                        recipientDeviceRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
                        mailboxRoute = route,
                        encryptedPayload = transportPayloadCodec.encode(payload),
                        createdAtEpochMilliseconds = now,
                        expiresAtEpochMilliseconds = now + LIFETIME_MILLISECONDS
                    )
                ).getOrThrow()
            logger.debug { "Indicator $indicatorType put into the mailbox of $recipientContactId" }
        }.onFailure { error -> logger.warn(error) { "Indicator $indicatorType to $recipientContactId failed" } }

    companion object {
        /** Long enough to pass through a circuit and a wait, short enough that the mailbox sends no wake for it. */
        const val LIFETIME_MILLISECONDS = 30_000L
        private const val BUFFERED_INDICATORS = 64
    }
}
