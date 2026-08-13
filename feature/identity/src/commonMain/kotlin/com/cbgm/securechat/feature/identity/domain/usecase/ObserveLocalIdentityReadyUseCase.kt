package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.feature.identity.domain.model.IdentityStatus
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository
import com.cbgm.securechat.feature.identity.domain.repository.LocalIdentityProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest

class ObserveLocalIdentityReadyUseCase(
    private val identityRepository: IdentityRepository,
    private val localIdentityProfileRepository: LocalIdentityProfileRepository
) {
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
                    .getOrNull() == IdentityStatus.READY
            }
        }.distinctUntilChanged()
}
