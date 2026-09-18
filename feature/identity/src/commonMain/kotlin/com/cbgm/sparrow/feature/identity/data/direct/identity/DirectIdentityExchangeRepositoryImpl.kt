package com.cbgm.sparrow.feature.identity.data.direct.identity

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentityResult
import com.cbgm.sparrow.feature.identity.domain.model.DirectInvitationRecord
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository
import kotlinx.coroutines.flow.Flow

internal class DirectIdentityExchangeRepositoryImpl(
    private val coordinator: DirectIdentityExchangeCoordinator
) : DirectIdentityExchangeRepository {
    override suspend fun start(contactId: String): Result<Unit> = coordinator.start(contactId)

    override suspend fun startInvitation(contactId: String): Result<DirectInvitationRecord?> =
        coordinator.startInvitation(contactId)

    override suspend fun getPendingIncomingInvitation(invitationId: String): Result<DirectInvitationRecord?> =
        coordinator.getPendingIncomingDirectInvitationRecord(invitationId)

    override suspend fun acceptInvitation(invitationId: String): Result<Unit> =
        coordinator.accept(invitationId)

    override suspend fun declineInvitation(invitationId: String): Result<Unit> =
        coordinator.decline(invitationId)

    override fun observeState(contactId: String): Flow<IdentityHandshakeState?> =
        coordinator.observeState(contactId)

    override fun observeResults(): Flow<List<DirectIdentityResult>> =
        coordinator.observeResults()

    override suspend fun cancelForManualSetup(contactId: String): Result<Unit> =
        coordinator.cancelForManualSetup(contactId)

    override suspend fun requireDirectChatAuthorization(
        contactId: String,
        mode: DirectIdentitySetupMode
    ): Result<Unit> = coordinator.requireDirectChatAuthorization(contactId, mode)

    override suspend fun revokeDirectChatAuthorization(contactId: String): Result<Unit> =
        coordinator.revokeDirectChatAuthorization(contactId)

    override suspend fun receiveInvite(
        context: IncomingPacketContext,
        packet: ContactInvitePacket
    ): Result<Unit> =
        coordinator.receiveInvite(context = context, packet = packet)

    override suspend fun receiveAccepted(
        context: IncomingPacketContext,
        packet: ContactInviteAcceptedPacket
    ): Result<Unit> = coordinator.receiveAccepted(context, packet)

    override suspend fun receiveDeclined(
        context: IncomingPacketContext,
        packet: ContactInviteDeclinedPacket
    ): Result<Unit> = coordinator.receiveDeclined(context, packet)

    override suspend fun receiveReady(
        context: IncomingPacketContext,
        packet: ContactReadyPacket
    ): Result<Unit> = coordinator.receiveReady(context, packet)

    override suspend fun receiveDirectChatAuthorizationRevoked(
        context: IncomingPacketContext,
        packet: DirectChatAuthorizationRevokedPacket
    ): Result<Unit> = coordinator.receiveDirectChatAuthorizationRevoked(context, packet)
}
