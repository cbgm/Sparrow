package com.cbgm.sparrow.notification.device

import com.cbgm.sparrow.core.protocol.outbox.OutboxRunner
import com.cbgm.sparrow.core.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionManager
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.notification.domain.model.AppVisibilityState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

/**
 * FCM wakes Sparrow without starting AppViewModel's foreground connection/outbox
 * lifecycle. DirectMessagePacketHandler has already persisted delivery receipts;
 * this bounded session sends them using the SAME encrypted protocol outbox and
 * normal node connection used in the foreground.
 */
class BackgroundDeliveryReceiptSender(
    private val protocolOutbox: ProtocolOutbox,
    private val transportConnectionManager: TransportConnectionManager,
    private val outboxRunner: OutboxRunner,
    private val appVisibilityState: AppVisibilityState
) {
    suspend fun flush() {
        // Only target delivery receipts; FCM notification delivery itself is
        // never sufficient to claim that a message was delivered.
        val receiptIds =
            (
                protocolOutbox.getPending(MAX_OUTBOX_SNAPSHOT).getOrThrow() +
                    protocolOutbox.observeTransportStates().first().filter {
                        it.status == OutboxStatus.FAILED
                    }
            )
                .filter { it.packetId.startsWith(DELIVERY_RECEIPT_PREFIX) }
                .map { it.packetId }
                .distinct()
        if (receiptIds.isEmpty() || appVisibilityState.isVisible.value) return

        val ownedBackgroundConnection =
            transportConnectionManager.connectionState.value !is TransportConnectionState.Connected
        if (ownedBackgroundConnection) transportConnectionManager.start()

        try {
            withTimeout(CONNECTION_TIMEOUT_MILLISECONDS.milliseconds) {
                transportConnectionManager.connectionState.first {
                    it is TransportConnectionState.Connected
                }
            }
            outboxRunner.start()
            // Do not infer success from a worker SUCCESS, empty pending queue,
            // or an item entering PROCESSING: wait for server acceptance.
            withTimeout(RECEIPT_ACCEPTANCE_TIMEOUT_MILLISECONDS.milliseconds) {
                while (receiptIds.any { packetId ->
                        protocolOutbox.findByPacketId(packetId).getOrThrow()?.status != OutboxStatus.SENT
                    }
                ) {
                    delay(RECEIPT_POLL_INTERVAL_MILLISECONDS.milliseconds)
                }
            }
        } finally {
            // Do not stop a connection adopted by an app that became visible
            // while the FCM worker was finishing.
            if (ownedBackgroundConnection && !appVisibilityState.isVisible.value) {
                outboxRunner.stop()
                transportConnectionManager.stop()
            }
        }
    }

    private companion object {
        const val DELIVERY_RECEIPT_PREFIX = "delivery-receipt-"
        const val MAX_OUTBOX_SNAPSHOT = 500
        const val CONNECTION_TIMEOUT_MILLISECONDS = 20_000L
        const val RECEIPT_ACCEPTANCE_TIMEOUT_MILLISECONDS = 20_000L
        const val RECEIPT_POLL_INTERVAL_MILLISECONDS = 250L
    }
}
