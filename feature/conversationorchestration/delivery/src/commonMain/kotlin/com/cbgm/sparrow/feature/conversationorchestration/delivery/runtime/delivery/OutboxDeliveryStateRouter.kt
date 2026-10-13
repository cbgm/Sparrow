package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.delivery

import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.command.DeliveryCommand
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.DeliveryEventTarget
import com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.protocol.outbox.OutboxDeliveryStateListener

/** Translates Messaging's outbox progress into delivery workflows. */
internal class OutboxDeliveryStateRouter(
    private val engine: WorkflowEngine
) : OutboxDeliveryStateListener {
    override suspend fun onProcessing(packetId: String): Result<Unit> =
        applyEvent(packetId, MessageDeliveryEvent.SEND_STARTED)

    override suspend fun onPrepared(
        packetId: String,
        encodedTransportPayload: String,
        transportMode: String
    ): Result<Unit> =
        engine.run(
            DeliveryCommand.RecordPreparedTransport(
                packetId = packetId,
                encodedTransportPayload = encodedTransportPayload,
                transportMode = transportMode
            )
        ).map { }

    override suspend fun onSent(packetId: String): Result<Unit> =
        applyEvent(packetId, MessageDeliveryEvent.SEND_SUCCEEDED)

    override suspend fun onExpired(packetId: String): Result<Unit> =
        applyEvent(packetId, MessageDeliveryEvent.DELIVERY_EXPIRED)

    override suspend fun onFailed(
        packetId: String,
        errorMessage: String
    ): Result<Unit> =
        applyEvent(
            packetId = packetId,
            // The durable outbox still owns this packet and will retry a temporary
            // relay/network failure. Do not show a terminal failure for that attempt.
            // Identity, encryption or routing-preparation failures remain FAILED.
            event = if (errorMessage.startsWith(TRANSIENT_WIRE_FAILURE_PREFIX)) {
                MessageDeliveryEvent.TRANSPORT_RETRY_PENDING
            } else {
                MessageDeliveryEvent.SEND_FAILED
            },
            errorMessage = errorMessage
        )

    private suspend fun applyEvent(
        packetId: String,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ): Result<Unit> =
        engine.run(
            DeliveryCommand.ApplyDeliveryEvent(
                target = DeliveryEventTarget.Packet(packetId),
                event = event,
                errorMessage = errorMessage
            )
        ).map { }

    private companion object {
        const val TRANSIENT_WIRE_FAILURE_PREFIX = "TRANSIENT_WIRE:"
    }
}
