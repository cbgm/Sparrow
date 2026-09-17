package com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository
import kotlinx.coroutines.flow.Flow

internal class DirectIdentityExchangeRepositoryImpl(
    private val coordinator: DirectIdentityExchangeCoordinator
) : DirectIdentityExchangeRepository {
    override suspend fun start(contactId: String): Result<Unit> =
        coordinator.start(contactId)

    override fun observeState(contactId: String): Flow<IdentityHandshakeState?> =
        coordinator.observeState(contactId)

    override suspend fun cancelForManualSetup(contactId: String): Result<Unit> =
        coordinator.cancelForManualSetup(contactId)

    override suspend fun requireDirectChatAuthorization(
        contactId: String,
        mode: DirectIdentitySetupMode
    ): Result<Unit> = coordinator.requireDirectChatAuthorization(contactId, mode)

    override suspend fun revokeDirectChatAuthorization(contactId: String): Result<Unit> =
        coordinator.revokeDirectChatAuthorization(contactId)

    override suspend fun receiveReady(
        context: IncomingPacketContext,
        packet: ContactReadyPacket
    ): Result<Unit> = coordinator.receiveReady(context, packet)

    override suspend fun receiveDirectChatAuthorizationRevoked(
        context: IncomingPacketContext,
        packet: DirectChatAuthorizationRevokedPacket
    ): Result<Unit> = coordinator.receiveDirectChatAuthorizationRevoked(context, packet)
}
