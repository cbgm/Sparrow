package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository

class ObserveBlockUnknownContactInvitesUseCase(
    private val repository: ContactBlocklistRepository
) {
    operator fun invoke() = repository.observeBlockUnknownContactInvites()
}
