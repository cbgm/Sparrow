package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository

class SetBlockUnknownContactInvitesUseCase(
    private val repository: ContactBlocklistRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setBlockUnknownContactInvites(enabled)
    }
}
