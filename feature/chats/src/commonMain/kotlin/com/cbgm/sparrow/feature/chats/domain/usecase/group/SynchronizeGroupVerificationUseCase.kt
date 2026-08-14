package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationActionRepository

class SynchronizeGroupVerificationUseCase(
    private val repository: GroupVerificationActionRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = repository.synchronize(groupId)
}
