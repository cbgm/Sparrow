package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.feature.messaging.runtime.indicator.MessagingIndicatorGateway

class SendMessagingIndicatorUseCase(
    private val gateway: MessagingIndicatorGateway
) {
    suspend operator fun invoke(
        recipientContactId: String,
        groupId: String?,
        indicatorType: String
    ): Result<Unit> = gateway.send(recipientContactId, groupId, indicatorType)
}
