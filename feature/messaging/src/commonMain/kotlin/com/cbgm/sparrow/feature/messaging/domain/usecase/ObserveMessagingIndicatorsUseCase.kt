package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import com.cbgm.sparrow.feature.messaging.domain.port.MessagingIndicatorPort
import kotlinx.coroutines.flow.Flow

class ObserveMessagingIndicatorsUseCase(
    private val indicatorPort: MessagingIndicatorPort
) {
    operator fun invoke(): Flow<MessagingIndicator> = indicatorPort.incoming
}
