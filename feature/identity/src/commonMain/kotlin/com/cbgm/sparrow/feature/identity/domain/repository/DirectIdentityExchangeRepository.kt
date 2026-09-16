package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import kotlinx.coroutines.flow.Flow

interface DirectIdentityExchangeRepository {
    suspend fun start(contactId: String): Result<Unit>

    fun observeAcceptedContactIds(): Flow<Set<String>>

    fun observeDeclinedOutgoingContactIds(): Flow<Set<String>>

    fun observeState(contactId: String): Flow<IdentityHandshakeState?>

    suspend fun cancelForManualSetup(contactId: String): Result<Unit>

    suspend fun requireDirectChatAuthorization(
        contactId: String,
        mode: DirectIdentitySetupMode
    ): Result<Unit>

    suspend fun revokeDirectChatAuthorization(contactId: String): Result<Unit>

    suspend fun receiveReady(
        context: IncomingPacketContext,
        packet: ContactReadyPacket
    ): Result<Unit>

    suspend fun receiveDirectChatAuthorizationRevoked(
        context: IncomingPacketContext,
        packet: DirectChatAuthorizationRevokedPacket
    ): Result<Unit>
}
