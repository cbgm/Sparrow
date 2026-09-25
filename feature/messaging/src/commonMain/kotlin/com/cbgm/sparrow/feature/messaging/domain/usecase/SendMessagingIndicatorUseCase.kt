package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.feature.messaging.runtime.indicator.MessagingIndicatorGateway

class SendMessagingIndicatorUseCase(
    private val gateway: MessagingIndicatorGateway
) {
    suspend operator fun invoke(recipientRoutingId: String, indicatorType: String): Result<Unit> =
        gateway.send(recipientRoutingId, indicatorType)
}
