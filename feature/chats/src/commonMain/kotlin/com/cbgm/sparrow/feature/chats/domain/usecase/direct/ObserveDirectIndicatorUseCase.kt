package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectIndicatorRepository
import kotlinx.coroutines.flow.Flow

class ObserveDirectIndicatorUseCase(
    private val repository: DirectIndicatorRepository
) {
    operator fun invoke(contactId: String): Flow<IndicatorType> = repository.observe(contactId)
}
