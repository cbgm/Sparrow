package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupDescriptionRepository

class SetGroupDescriptionUseCase(
    private val repository: GroupDescriptionRepository
) {
    suspend operator fun invoke(
        groupId: String,
        description: String
    ): Result<Unit> = repository.set(groupId, description)
}
