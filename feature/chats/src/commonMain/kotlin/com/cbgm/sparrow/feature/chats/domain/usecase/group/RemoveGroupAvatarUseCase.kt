package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository

class RemoveGroupAvatarUseCase(
    private val repository: GroupAvatarRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = repository.remove(groupId)
}
