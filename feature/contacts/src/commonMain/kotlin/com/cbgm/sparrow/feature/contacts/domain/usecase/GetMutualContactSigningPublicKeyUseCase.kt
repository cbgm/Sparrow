package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactTransportRepository

class GetMutualContactSigningPublicKeyUseCase(
    private val repository: ContactTransportRepository
) {
    suspend operator fun invoke(contactId: String): ByteArray? = repository.getMutualSigningPublicKey(contactId)
}
