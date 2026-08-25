package com.cbgm.sparrow.feature.contacts.domain.repository

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitation
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityInvitationDirection
import com.cbgm.sparrow.feature.contacts.domain.model.PendingContactInvitation
import kotlinx.coroutines.flow.Flow

interface IdentityInvitationRepository {
    suspend fun start(contactId: String): Result<Unit>

    fun observePendingIncoming(): Flow<List<PendingContactInvitation>>

    fun observeInvitations(direction: IdentityInvitationDirection): Flow<List<ContactInvitation>>

    fun observeAcceptedContactIds(): Flow<Set<String>>

    fun observeDeclinedOutgoingContactIds(): Flow<Set<String>>

    fun observeState(contactId: String): Flow<IdentityHandshakeState?>

    suspend fun getContactId(invitationId: String): Result<String>

    suspend fun accept(invitationId: String): Result<Unit>

    suspend fun decline(invitationId: String): Result<Unit>

    suspend fun markViewed(direction: IdentityInvitationDirection): Result<Unit>

    suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit>

    suspend fun cancelForManualSetup(contactId: String): Result<Unit>

    suspend fun requireDirectChatAuthorization(
        contactId: String,
        mode: DirectIdentitySetupMode
    ): Result<Unit>

    suspend fun revokeDirectChatAuthorization(contactId: String): Result<Unit>

    suspend fun receiveInvite(
        context: IncomingPacketContext,
        packet: ContactInvitePacket,
        setupMode: DirectIdentitySetupMode,
        blockedContactIds: Set<String>,
        blockUnknownContactInvites: Boolean
    ): Result<Unit>

    suspend fun receiveAccepted(
        context: IncomingPacketContext,
        packet: ContactInviteAcceptedPacket
    ): Result<Unit>

    suspend fun receiveReady(
        context: IncomingPacketContext,
        packet: ContactReadyPacket
    ): Result<Unit>

    suspend fun receiveDeclined(
        context: IncomingPacketContext,
        packet: ContactInviteDeclinedPacket
    ): Result<Unit>

    suspend fun receiveDirectChatAuthorizationRevoked(
        context: IncomingPacketContext,
        packet: DirectChatAuthorizationRevokedPacket
    ): Result<Unit>
}
