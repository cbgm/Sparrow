package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository

class SetGroupAvatarUseCase(
    private val repository: GroupAvatarRepository
) {
    suspend operator fun invoke(
        groupId: String,
        bytes: ByteArray
    ): Result<Unit> = repository.set(groupId, bytes)
}
