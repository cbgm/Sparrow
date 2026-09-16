package com.cbgm.sparrow.feature.invite.data.event

import com.cbgm.sparrow.feature.invite.domain.event.InvitationResultStream
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow

class InvitationResultStreamImpl(
    private val repository: InvitationRepository
) : InvitationResultStream {
    override fun observeInvitationResults(): Flow<List<InvitationResult>> =
        repository.observeInvitationResults()
}
