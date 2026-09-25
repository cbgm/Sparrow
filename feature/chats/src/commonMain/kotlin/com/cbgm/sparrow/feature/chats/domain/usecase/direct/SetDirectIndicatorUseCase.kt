package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectIndicatorRepository

class SetDirectIndicatorUseCase(
    private val repository: DirectIndicatorRepository
) {
    suspend operator fun invoke(
        contactId: String,
        indicatorType: IndicatorType
    ): Result<Unit> = repository.send(contactId, indicatorType)
}
