package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.core.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.feature.messaging.domain.model.MessagingTransportResult
import com.cbgm.sparrow.feature.messaging.domain.model.MessagingTransportState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Generic, database-backed outgoing transport status feed for orchestration. */
class ObserveMessagingTransportResultsUseCase(
    private val protocolOutbox: ProtocolOutbox
) {
    operator fun invoke(): Flow<List<MessagingTransportResult>> =
        protocolOutbox.observeTransportStates().map { items ->
            items.mapNotNull { item ->
                val state = when (item.status) {
                    OutboxStatus.SENT -> MessagingTransportState.ACCEPTED_BY_RELAY
                    OutboxStatus.FAILED -> MessagingTransportState.FAILED
                    OutboxStatus.EXPIRED -> MessagingTransportState.EXPIRED
                    OutboxStatus.PENDING, OutboxStatus.PROCESSING,
                    OutboxStatus.QUARANTINED -> null
                }
                state?.let {
                    MessagingTransportResult(
                        packetId = item.packetId,
                        attemptCount = item.attemptCount,
                        state = it,
                        errorMessage = item.lastError,
                        expiresAtEpochMilliseconds = item.expiresAtEpochMilliseconds,
                        updatedAtEpochMilliseconds = item.updatedAtEpochMilliseconds
                    )
                }
            }
        }
}
