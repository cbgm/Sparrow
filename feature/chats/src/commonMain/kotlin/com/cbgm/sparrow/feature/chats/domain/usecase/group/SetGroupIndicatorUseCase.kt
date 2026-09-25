package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupIndicatorRepository

class SetGroupIndicatorUseCase(
    private val repository: GroupIndicatorRepository
) {
    suspend operator fun invoke(
        groupId: String,
        indicatorType: IndicatorType
    ): Result<Unit> =
        repository.setIndicator(
            groupId = groupId,
            indicatorType = indicatorType
        )
}
