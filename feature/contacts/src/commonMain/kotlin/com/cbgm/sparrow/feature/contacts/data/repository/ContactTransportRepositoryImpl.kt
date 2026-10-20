package com.cbgm.sparrow.feature.contacts.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
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
    override suspend fun resolveRoutingId(contactId: String): Result<String> =
        safeSuspendCall { contactRouting.resolve(contactId) }

    override suspend fun resolveBootstrapRoutingId(contactId: String): Result<String> =
        safeSuspendCall { contactRouting.resolveBootstrap(contactId) }

    override suspend fun resolveInvitationRoutingId(contactId: String): Result<String> =
        safeSuspendCall { contactRouting.resolveInvitation(contactId) }

    override suspend fun resolveContactIdByRoutingId(routingId: String): Result<String?> =
        safeSuspendCall { contactByRoutingId.resolveContactId(routingId) }

    override suspend fun reconcileKnownContacts(): Result<Unit> =
        safeSuspendCall { reconciliation.reconcileKnownContacts() }

    override suspend fun getMailboxContactStates(): Result<List<MailboxContactState>> =
        safeSuspendCall {
            mailboxContacts.findContactStates().map { dto ->
                MailboxContactState(dto.contactId, dto.isProvisioningEligible)
            }
        }

    override suspend fun getMutualSigningPublicKey(contactId: String): Result<ByteArray?> =
        safeSuspendCall { mailboxContacts.findMutualSigningPublicKey(contactId) }
}
