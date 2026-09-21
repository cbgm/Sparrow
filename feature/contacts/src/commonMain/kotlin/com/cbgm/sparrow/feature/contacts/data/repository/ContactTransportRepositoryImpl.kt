package com.cbgm.sparrow.feature.contacts.data.repository

import com.cbgm.sparrow.feature.contacts.data.datasource.ContactByRoutingIdDataSource
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactRoutingDataSource
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactRoutingReconciliationDataSource
import com.cbgm.sparrow.feature.contacts.data.datasource.MailboxContactDataSource
import com.cbgm.sparrow.feature.contacts.domain.model.MailboxContactState
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactTransportRepository

class ContactTransportRepositoryImpl(
    private val contactRouting: ContactRoutingDataSource,
    private val contactByRoutingId: ContactByRoutingIdDataSource,
    private val reconciliation: ContactRoutingReconciliationDataSource,
    private val mailboxContacts: MailboxContactDataSource
) : ContactTransportRepository {
    override suspend fun resolveRoutingId(contactId: String): String = contactRouting.resolve(contactId)

    override suspend fun resolveBootstrapRoutingId(contactId: String): String = contactRouting.resolveBootstrap(contactId)

    override suspend fun resolveContactIdByRoutingId(routingId: String): String? = contactByRoutingId.resolveContactId(routingId)

    override suspend fun reconcileKnownContacts() = reconciliation.reconcileKnownContacts()

    override suspend fun getMailboxContactStates(): List<MailboxContactState> =
        mailboxContacts.findContactStates().map { dto ->
            MailboxContactState(dto.contactId, dto.isProvisioningEligible)
        }

    override suspend fun getMutualSigningPublicKey(contactId: String): ByteArray? =
        mailboxContacts.findMutualSigningPublicKey(contactId)
}
