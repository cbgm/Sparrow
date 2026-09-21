package com.cbgm.sparrow.feature.contacts.domain.repository

import com.cbgm.sparrow.feature.contacts.domain.model.MailboxContactState

/** All transport-related contact lookups remain owned and implemented by Contacts. */
interface ContactTransportRepository {
    suspend fun resolveRoutingId(contactId: String): String

    suspend fun resolveBootstrapRoutingId(contactId: String): String

    suspend fun resolveContactIdByRoutingId(routingId: String): String?

    suspend fun reconcileKnownContacts()

    suspend fun getMailboxContactStates(): List<MailboxContactState>

    suspend fun getMutualSigningPublicKey(contactId: String): ByteArray?
}
