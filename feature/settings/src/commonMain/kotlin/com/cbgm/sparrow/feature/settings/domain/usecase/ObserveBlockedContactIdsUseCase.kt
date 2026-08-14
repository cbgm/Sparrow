package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository

class ObserveBlockedContactIdsUseCase(
    private val repository: ContactBlocklistRepository
) {
    operator fun invoke() = repository.observeBlockedContactIds()
}
