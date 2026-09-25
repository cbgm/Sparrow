package com.cbgm.sparrow.feature.contacts.domain.usecase.identity

import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerMerge
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityPeerRepository

class ApplyIdentityPeerMergeUseCase(
    private val repository: IdentityPeerRepository
) {
    suspend operator fun invoke(merge: IdentityPeerMerge) = repository.applyMerge(merge)
}
