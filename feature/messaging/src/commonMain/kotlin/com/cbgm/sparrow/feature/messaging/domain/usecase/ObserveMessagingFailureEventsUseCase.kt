package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.feature.messaging.domain.model.MessagingFailureEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Unlike mutable outbox snapshots, each failure remains available until acknowledged. */
class ObserveMessagingFailureEventsUseCase(
    private val outbox: ProtocolOutbox
) {
    operator fun invoke(): Flow<List<MessagingFailureEvent>> =
        outbox.observeUnacknowledgedFailures().map { events ->
            events.map { event ->
                MessagingFailureEvent(
                    eventId = event.eventId,
                    packetId = event.packetId,
                    encodedPacket = event.encodedPacket.copyOf(),
                    attemptCount = event.attemptCount,
                    errorMessage = event.errorMessage,
                    occurredAtEpochMilliseconds = event.occurredAtEpochMilliseconds
                )
            }
        }
}
