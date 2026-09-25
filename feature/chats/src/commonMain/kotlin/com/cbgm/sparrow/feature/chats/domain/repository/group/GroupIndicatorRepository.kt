package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import kotlinx.coroutines.flow.Flow

interface GroupIndicatorRepository {
    fun observeMember(
        groupId: String,
        contactId: String
    ): Flow<IndicatorType>

    suspend fun setIndicator(
        groupId: String,
        indicatorType: IndicatorType
    ): Result<Unit>
}
