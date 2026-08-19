package com.cbgm.sparrow.feature.contacts.domain.repository

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

    suspend fun accept(invitationId: String): Result<Unit>

    suspend fun decline(invitationId: String): Result<Unit>

    suspend fun declineAndBlock(invitationId: String): Result<Unit>

    suspend fun markViewed(direction: IdentityInvitationDirection): Result<Unit>

    suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit>

    suspend fun cancelForManualSetup(contactId: String): Result<Unit>

    suspend fun requireDirectChatAuthorization(contactId: String): Result<Unit>

    suspend fun revokeDirectChatAuthorization(contactId: String): Result<Unit>
}
