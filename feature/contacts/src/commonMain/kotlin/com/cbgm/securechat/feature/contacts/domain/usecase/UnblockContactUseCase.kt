package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.core.security.ContactBlocklistRepository

class UnblockContactUseCase(
    private val repository: ContactBlocklistRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        runCatching {
            repository.unblock(contactId)
        }
}
