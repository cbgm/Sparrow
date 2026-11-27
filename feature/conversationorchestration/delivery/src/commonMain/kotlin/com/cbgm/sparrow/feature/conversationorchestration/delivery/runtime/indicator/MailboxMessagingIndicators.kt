package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.indicator

import com.cbgm.sparrow.core.crypto.transport.TransportPayloadCodec
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.indicator.MailboxMessagingIndicators.Companion.LIFETIME_MILLISECONDS
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.outbox.OutgoingTransportPayloadFactory
import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import com.cbgm.sparrow.feature.messaging.domain.port.MessagingIndicatorPort
import com.cbgm.sparrow.feature.transport.domain.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.domain.model.MailboxNodeUnavailableException
import com.cbgm.sparrow.feature.transport.domain.port.DirectSendingPort
import com.cbgm.sparrow.feature.transport.domain.repository.MailboxRepository
import com.cbgm.sparrow.protocol.codec.PacketCodec
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.protocol.packet.IndicatorPacket
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Indicators go into the contact's mailbox through the circuit, encrypted and padded like messages and without
 * routing IDs, so no node learns who is typing to whom. They are never queued: without a route to the contact, while
 * sending directly is switched off and no circuit protects, or while the contact's mailbox node is down, the indicator
 * is dropped. The envelope lives
 * [LIFETIME_MILLISECONDS], and the mailbox does not wake the recipient for it.
 */
class MailboxMessagingIndicators(
    private val mailboxRouteRepository: MailboxRouteRepository,
    private val getContact: GetContactUseCase,
    private val packetCodec: PacketCodec,
    private val transportPayloadFactory: OutgoingTransportPayloadFactory,
    private val transportPayloadCodec: TransportPayloadCodec,
    private val mailboxRepository: MailboxRepository,
    private val sendingPolicy: DirectSendingPort
) : MessagingIndicatorPort {
    private val logger = SparrowLog.withTag("MailboxMessagingIndicators")
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
            val delivered =
                mailboxRepository.deliver(
                    FederatedEnvelope(
                        envelopeId = IdGenerator.generate(),
                        senderRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
                        recipientDeviceRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
                        mailboxRoute = route,
                        encryptedPayload = transportPayloadCodec.encode(payload),
                        createdAtEpochMilliseconds = now,
                        expiresAtEpochMilliseconds = now + LIFETIME_MILLISECONDS
                    )
                )
            if (delivered.exceptionOrNull() is MailboxNodeUnavailableException) {
                logger.info { "Indicator $indicatorType not sent to $recipientContactId: their mailbox node is unavailable" }
                return@safeSuspendCall
            }
            delivered.getOrThrow()
            logger.debug { "Indicator $indicatorType put into the mailbox of $recipientContactId" }
        }.onFailure { error -> logger.warn(error) { "Indicator $indicatorType to $recipientContactId failed" } }

    companion object {
        /** Long enough to pass through a circuit and a wait, short enough that the mailbox sends no wake for it. */
        const val LIFETIME_MILLISECONDS = 30_000L
        private const val BUFFERED_INDICATORS = 64
    }
}
