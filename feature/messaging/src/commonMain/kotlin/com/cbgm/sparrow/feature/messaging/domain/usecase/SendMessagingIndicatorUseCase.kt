package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.feature.messaging.domain.port.MessagingIndicatorPort

class SendMessagingIndicatorUseCase(
    private val indicatorPort: MessagingIndicatorPort
) {
    suspend operator fun invoke(
        recipientContactId: String,
        groupId: String?,
        indicatorType: String
    ): Result<Unit> = indicatorPort.send(recipientContactId, groupId, indicatorType)
}
