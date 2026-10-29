package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import kotlinx.coroutines.flow.Flow

/** False while the running app has no usable keys for its identity; the app then goes back to startup. */
class ObserveIdentityKeysUsableUseCase(
    private val identityRepository: IdentityRepository
) {
    operator fun invoke(): Flow<Boolean> = identityRepository.observeKeysUsable()
}
