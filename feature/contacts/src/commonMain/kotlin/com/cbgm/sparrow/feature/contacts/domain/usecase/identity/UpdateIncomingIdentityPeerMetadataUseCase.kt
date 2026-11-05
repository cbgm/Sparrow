package com.cbgm.sparrow.feature.contacts.domain.usecase.identity

import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityPeerRepository

/**
 * Shows a peer without a name by the phone number its invitation claims. The number is stored as the peer's own
 * only when its keys already belong to a known contact: a phone book import would otherwise link a stranger who
 * merely claimed the number.
 */
class UpdateIncomingIdentityPeerMetadataUseCase(
    private val repository: IdentityPeerRepository
) {
    suspend operator fun invoke(
        peerId: String,
        phoneNumber: String,
        knownByKeys: Boolean,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> =
        repository.updateIncomingMetadata(
            peerId = peerId,
            phoneNumber = phoneNumber,
            storePhoneNumber = knownByKeys,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
        )
}
