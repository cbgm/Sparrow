package com.cbgm.sparrow.feature.chats.data.repository

import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupIndicatorRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.ObserveConversationIndicatorUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.SendConversationIndicatorUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GroupIndicatorRepositoryImpl(
    private val sendConversationIndicator: SendConversationIndicatorUseCase,
    private val observeConversationIndicator: ObserveConversationIndicatorUseCase
) : GroupIndicatorRepository {
    override fun observeMember(groupId: String, contactId: String): Flow<IndicatorType> =
        observeConversationIndicator.forGroupMember(groupId, contactId).map(String::toIndicatorType)

    override suspend fun setIndicator(groupId: String, indicatorType: IndicatorType): Result<Unit> =
        sendConversationIndicator.toGroup(groupId, indicatorType.name)
}

private fun String.toIndicatorType(): IndicatorType =
    IndicatorType.entries.firstOrNull { it.name == this } ?: IndicatorType.NONE
