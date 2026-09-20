package com.cbgm.sparrow.feature.contacts.domain.usecase.identity

import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityPeerRepository

class GetIdentityPeerDisplayNameUseCase(
    private val repository: IdentityPeerRepository
) {
    suspend operator fun invoke(peerId: String): String? = repository.getDisplayName(peerId)
}
