package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository
import kotlinx.coroutines.flow.Flow

/** Whether the phone verifier's daily maximum keeps the user's number unverified right now. */
class ObservePhoneVerificationLimitReachedUseCase(
    private val localIdentityProfileRepository: LocalIdentityProfileRepository
) {
    operator fun invoke(): Flow<Boolean> = localIdentityProfileRepository.observePhoneVerificationLimitReached()
}
