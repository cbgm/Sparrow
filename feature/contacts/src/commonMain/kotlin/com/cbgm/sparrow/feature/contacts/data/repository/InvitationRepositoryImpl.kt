package com.cbgm.sparrow.feature.contacts.data.repository

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow

internal class InvitationRepositoryImpl(
    private val coordinator: DirectIdentityExchangeCoordinator
) : InvitationRepository {
    override fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>> =
        coordinator.observeInvitations(direction)

    override fun observeInvitationResults(): Flow<List<InvitationResult>> =
        coordinator.observeInvitationResults()

    override suspend fun getPeerId(invitationId: String): Result<String> =
        coordinator.getPeerId(invitationId)

    override suspend fun accept(invitationId: String): Result<Unit> =
        coordinator.accept(invitationId)

    override suspend fun decline(
        invitationId: String,
        action: InvitationResultAction?
    ): Result<Unit> =
        coordinator.decline(invitationId, action)

    override suspend fun applyResponse(
        invitationId: String,
        response: InvitationResponse
    ): Result<Unit> = coordinator.applyResponse(invitationId, response)

    override suspend fun markViewed(direction: InvitationDirection): Result<Unit> =
        coordinator.markViewed(direction)

    override suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> =
        coordinator.deleteDeclinedOutgoing(invitationId)
}
