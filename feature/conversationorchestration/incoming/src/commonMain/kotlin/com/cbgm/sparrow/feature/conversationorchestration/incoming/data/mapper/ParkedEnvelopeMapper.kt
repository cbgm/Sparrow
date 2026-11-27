package com.cbgm.sparrow.feature.conversationorchestration.incoming.data.mapper

import com.cbgm.sparrow.data.database.entity.ParkedEnvelopeEntity
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DeferredPacketWait
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ParkedEnvelope

/** Null for a row whose wait this build does not know; it stays until it expires. */
internal fun ParkedEnvelopeEntity.toDomain(): ParkedEnvelope? =
    DeferredPacketWait.fromKey(waitingKey)?.let { wait ->
        ParkedEnvelope(
            envelopeId = envelopeId,
            senderRoutingId = senderRoutingId,
            encodedTransportPayload = encodedTransportPayload,
            wait = wait,
            parkedAtEpochMilliseconds = parkedAtEpochMilliseconds
        )
    }
