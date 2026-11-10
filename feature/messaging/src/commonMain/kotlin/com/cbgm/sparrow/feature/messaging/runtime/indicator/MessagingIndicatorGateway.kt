package com.cbgm.sparrow.feature.messaging.runtime.indicator

import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import kotlinx.coroutines.flow.Flow

/** Transport boundary for generic, ephemeral indicator state. */
interface MessagingIndicatorGateway {
    val incoming: Flow<MessagingIndicator>

    /** Sends to one contact; nothing happens when no protected way to them exists, since a late indicator is useless. */
    suspend fun send(
        recipientContactId: String,
        groupId: String?,
        indicatorType: String
    ): Result<Unit>
}
