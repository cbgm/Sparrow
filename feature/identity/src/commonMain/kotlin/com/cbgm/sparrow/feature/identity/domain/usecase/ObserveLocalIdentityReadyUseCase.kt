package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest

class ObserveLocalIdentityReadyUseCase(
    private val identityRepository: IdentityRepository,
    private val localIdentityProfileRepository: LocalIdentityProfileRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Boolean> =
        combine(
            identityRepository.observeIdentity(),
            localIdentityProfileRepository.observePhoneNumber()
        ) { identity, phoneNumber ->
            identity to phoneNumber
        }.mapLatest { (identity, phoneNumber) ->
            if (identity == null || phoneNumber.isNullOrBlank()) {
                false
            } else {
                identityRepository
                    .getStatus()
                    .onFailure { failure ->
                        SparrowLog.error("ObserveLocalIdentityReadyUseCase", "Could not read identity status", failure)
                    }
                    .getOrNull() == IdentityStatus.READY
            }
        }.distinctUntilChanged()
}
