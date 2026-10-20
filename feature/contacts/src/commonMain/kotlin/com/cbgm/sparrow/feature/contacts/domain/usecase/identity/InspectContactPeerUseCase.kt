package com.cbgm.sparrow.feature.contacts.domain.usecase.identity

import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityPeerRepository

/** Exposes Contacts-owned facts to cross-feature lifecycle orchestration. */
class InspectContactPeerUseCase(
    private val repository: IdentityPeerRepository
) {
    suspend fun containsPeer(peerId: String): Result<Boolean> = repository.containsPeer(peerId)

    suspend fun isKnownContact(peerId: String): Result<Boolean> = repository.isKnownContact(peerId)

    suspend fun findEquivalentPhonePeerId(phoneNumber: String): Result<String?> =
        repository.findEquivalentPhonePeerId(phoneNumber)

    suspend fun canMergeRoutingDuplicate(
        peerId: String,
        remotePhoneNumber: String?
    ): Result<Boolean> = repository.canMergeRoutingDuplicate(peerId, remotePhoneNumber)
}
