package com.cbgm.sparrow.feature.chats.domain.repository.direct

import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import kotlinx.coroutines.flow.Flow

interface DirectIndicatorRepository {
    fun observe(contactId: String): Flow<IndicatorType>

    suspend fun send(
        contactId: String,
        indicatorType: IndicatorType
    ): Result<Unit>
}
