package com.cbgm.sparrow.feature.conversationorchestration.incoming.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.data.database.entity.ParkedEnvelopeEntity
import com.cbgm.sparrow.feature.conversationorchestration.incoming.data.datasource.ParkedEnvelopeDataSource
import com.cbgm.sparrow.feature.conversationorchestration.incoming.data.mapper.toDomain
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DeferredPacketWait
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ParkedEnvelope
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.repository.ParkedEnvelopeRepository

internal class ParkedEnvelopeRepositoryImpl(
    private val dataSource: ParkedEnvelopeDataSource
) : ParkedEnvelopeRepository {
    override suspend fun park(
        envelopeId: String,
        senderRoutingId: String,
        encodedTransportPayload: String,
        wait: DeferredPacketWait
    ): Result<Unit> =
        safeSuspendCall {
            val now = SystemClock.nowEpochMilliseconds()
            // Envelopes whose wait was never met expire like an undelivered mailbox envelope.
            dataSource.deleteParkedBefore(now - ParkedEnvelopeRepository.RETENTION_MILLISECONDS)
            dataSource.park(
                ParkedEnvelopeEntity(
                    envelopeId = envelopeId,
                    senderRoutingId = senderRoutingId,
                    encodedTransportPayload = encodedTransportPayload,
                    waitingKey = wait.key,
                    parkedAtEpochMilliseconds = now
                )
            )
        }

    override suspend fun findWaits(): Result<List<DeferredPacketWait>> =
        safeSuspendCall { dataSource.findWaitingKeys().mapNotNull(DeferredPacketWait::fromKey) }

    override suspend fun findWaitingFor(waits: List<DeferredPacketWait>): Result<List<ParkedEnvelope>> =
        safeSuspendCall {
            if (waits.isEmpty()) {
                emptyList()
            } else {
                dataSource.findByWaitingKeys(waits.map { it.key }).mapNotNull { it.toDomain() }
            }
        }

    override suspend fun remove(envelopeId: String): Result<Unit> = safeSuspendCall { dataSource.delete(envelopeId) }
}
