package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository

class SetBlockUnknownContactInvitesUseCase(
    private val repository: ContactBlocklistRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setBlockUnknownContactInvites(enabled)
    }
}
