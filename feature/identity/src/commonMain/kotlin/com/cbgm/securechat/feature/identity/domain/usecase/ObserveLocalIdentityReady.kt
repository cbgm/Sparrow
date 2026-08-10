package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository
import com.cbgm.securechat.feature.identity.domain.repository.storage.LocalPhoneNameStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveLocalIdentityReady(
    private val identityRepository: IdentityRepository,
    private val localPhoneNameStorage: LocalPhoneNameStorage
) {
    operator fun invoke(): Flow<Boolean> =
        combine(
            identityRepository.observeIdentity(),
            localPhoneNameStorage.observePhoneNumber()
        ) { identity, phoneNumber ->
            identity != null && !phoneNumber.isNullOrBlank()
        }.distinctUntilChanged()
}
