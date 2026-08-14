package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository

class UnblockContactUseCase(
    private val repository: ContactBlocklistRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        runCatching {
            repository.unblock(contactId)
        }
}
