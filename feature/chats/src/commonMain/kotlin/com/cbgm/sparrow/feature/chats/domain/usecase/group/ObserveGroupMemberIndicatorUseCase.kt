package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupIndicatorRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupMemberIndicatorUseCase(
    private val repository: GroupIndicatorRepository
) {
    operator fun invoke(
        groupId: String,
        contactId: String
    ): Flow<IndicatorType> = repository.observeMember(groupId, contactId)
}
