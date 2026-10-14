package com.cbgm.sparrow.feature.contacts.domain.repository

import com.cbgm.sparrow.feature.contacts.domain.model.MailboxContactState

/** All transport-related contact lookups remain owned and implemented by Contacts. */
interface ContactTransportRepository {
    suspend fun resolveRoutingId(contactId: String): Result<String>

    suspend fun resolveBootstrapRoutingId(contactId: String): Result<String>

    /** Resolve a DIRECT invitation route; prefer phone bootstrap, allow an existing key-only peer. */
    suspend fun resolveInvitationRoutingId(contactId: String): Result<String>

    suspend fun resolveContactIdByRoutingId(routingId: String): Result<String?>

    suspend fun reconcileKnownContacts(): Result<Unit>

    suspend fun getMailboxContactStates(): Result<List<MailboxContactState>>

    suspend fun getMutualSigningPublicKey(contactId: String): Result<ByteArray?>
}
