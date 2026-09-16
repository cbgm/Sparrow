package com.cbgm.sparrow.feature.invite.domain.event

import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import kotlinx.coroutines.flow.Flow

interface InvitationResultStream {
    fun observeInvitationResults(): Flow<List<InvitationResult>>
}
