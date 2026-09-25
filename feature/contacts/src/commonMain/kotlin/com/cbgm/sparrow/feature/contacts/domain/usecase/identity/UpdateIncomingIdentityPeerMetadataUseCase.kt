package com.cbgm.sparrow.feature.contacts.domain.usecase.identity

import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityPeerRepository

class UpdateIncomingIdentityPeerMetadataUseCase(
    private val repository: IdentityPeerRepository
) {
    suspend operator fun invoke(
        peerId: String,
        phoneNumber: String,
        updatedAtEpochMilliseconds: Long
    ) = repository.updateIncomingMetadata(peerId, phoneNumber, updatedAtEpochMilliseconds)
}
