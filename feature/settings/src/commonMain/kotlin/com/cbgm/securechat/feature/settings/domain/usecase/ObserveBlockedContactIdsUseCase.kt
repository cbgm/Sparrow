package com.cbgm.securechat.feature.settings.domain.usecase

import com.cbgm.securechat.core.security.ContactBlocklistRepository

class ObserveBlockedContactIdsUseCase(
    private val repository: ContactBlocklistRepository
) {
    operator fun invoke() = repository.observeBlockedContactIds()
}
