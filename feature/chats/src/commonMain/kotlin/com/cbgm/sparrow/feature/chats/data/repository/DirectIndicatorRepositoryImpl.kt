package com.cbgm.sparrow.feature.chats.data.repository

import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectIndicatorRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ObserveConversationIndicatorUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.SendConversationIndicatorUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DirectIndicatorRepositoryImpl(
    private val sendConversationIndicator: SendConversationIndicatorUseCase,
    private val observeConversationIndicator: ObserveConversationIndicatorUseCase
) : DirectIndicatorRepository {
    override fun observe(contactId: String): Flow<IndicatorType> =
        observeConversationIndicator.forContact(contactId).map(String::toIndicatorType)

    override suspend fun send(contactId: String, indicatorType: IndicatorType): Result<Unit> =
        sendConversationIndicator.toContact(contactId, indicatorType.name)
}

private fun String.toIndicatorType(): IndicatorType =
    IndicatorType.entries.firstOrNull { it.name == this } ?: IndicatorType.NONE
