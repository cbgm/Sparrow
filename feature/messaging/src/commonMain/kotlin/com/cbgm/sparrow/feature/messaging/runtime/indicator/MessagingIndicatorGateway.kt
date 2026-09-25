package com.cbgm.sparrow.feature.messaging.runtime.indicator

import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import kotlinx.coroutines.flow.Flow

/** Transport boundary for generic, ephemeral indicator state. */
interface MessagingIndicatorGateway {
    val incoming: Flow<MessagingIndicator>

    suspend fun send(recipientRoutingId: String, indicatorType: String): Result<Unit>
}
