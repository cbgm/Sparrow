package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox

class AcknowledgeMessagingFailureUseCase(
    private val outbox: ProtocolOutbox
) {
    suspend operator fun invoke(eventId: String): Result<Unit> = outbox.acknowledgeFailure(eventId)
}
