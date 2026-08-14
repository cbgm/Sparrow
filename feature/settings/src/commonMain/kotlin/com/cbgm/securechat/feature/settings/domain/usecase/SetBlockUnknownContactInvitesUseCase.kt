package com.cbgm.securechat.feature.settings.domain.usecase

import com.cbgm.securechat.core.security.ContactBlocklistRepository

class SetBlockUnknownContactInvitesUseCase(
    private val repository: ContactBlocklistRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setBlockUnknownContactInvites(enabled)
    }
}
