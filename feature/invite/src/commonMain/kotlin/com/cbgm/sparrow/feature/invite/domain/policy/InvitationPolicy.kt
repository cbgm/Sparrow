package com.cbgm.sparrow.feature.invite.domain.policy

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import kotlinx.coroutines.flow.Flow

interface InvitationPolicy {
    fun applyVisibility(
        direction: InvitationDirection,
        invitations: Flow<List<Invitation>>
    ): Flow<List<Invitation>>

    fun observePendingEnabled(): Flow<Boolean>

    suspend fun validateAcceptance(invitationId: String): Result<Unit>
}
