package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.GroupVerificationActionRepository

class SynchronizeGroupVerificationUseCase(
    private val repository: GroupVerificationActionRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = repository.synchronize(groupId)
}
