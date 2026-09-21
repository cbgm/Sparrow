package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import com.cbgm.sparrow.feature.messaging.runtime.indicator.MessagingIndicatorGateway
import kotlinx.coroutines.flow.Flow

class ObserveMessagingIndicatorsUseCase(
    private val gateway: MessagingIndicatorGateway
) {
    operator fun invoke(): Flow<MessagingIndicator> = gateway.incoming
}
