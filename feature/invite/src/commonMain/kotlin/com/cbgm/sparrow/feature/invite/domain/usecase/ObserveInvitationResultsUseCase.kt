package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow

class ObserveInvitationResultsUseCase(
    private val repository: InvitationRepository
) {
    operator fun invoke(): Flow<List<InvitationResult>> =
        repository.observeInvitationResults()
}
