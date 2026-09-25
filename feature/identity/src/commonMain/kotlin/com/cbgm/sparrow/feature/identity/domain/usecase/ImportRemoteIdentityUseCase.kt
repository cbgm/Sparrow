package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityUpdate
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityImportRepository

class ImportRemoteIdentityUseCase(
    private val repository: RemoteIdentityImportRepository
) {
    suspend operator fun invoke(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray,
        origin: RemoteIdentityOrigin
    ): Result<RemoteIdentityUpdate> = repository.storeRemoteIdentity(
        contactId = peerId,
        encryptionPublicKey = encryptionPublicKey,
        signingPublicKey = signingPublicKey,
        origin = origin
    )
}
