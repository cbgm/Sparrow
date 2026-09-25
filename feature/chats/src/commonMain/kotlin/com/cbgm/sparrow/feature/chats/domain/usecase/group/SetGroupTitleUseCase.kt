package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupTitleRepository

class SetGroupTitleUseCase(
    private val repository: GroupTitleRepository
) {
    suspend operator fun invoke(
        groupId: String,
        title: String
    ): Result<Unit> = repository.set(groupId, title)
}
