package com.cbgm.sparrow.feature.invite.domain.repository

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import kotlinx.coroutines.flow.Flow

interface InvitationRepository {
    fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>>

    suspend fun accept(invitationId: String): Result<Unit>

    suspend fun decline(invitationId: String): Result<Unit>

    suspend fun markViewed(direction: InvitationDirection): Result<Unit>

    suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit>
}
