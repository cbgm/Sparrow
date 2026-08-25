package com.cbgm.sparrow.feature.messaging.data.datasource

import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import kotlinx.coroutines.flow.first

data class MailboxContactState(
    val contactId: String,
    val isProvisioningEligible: Boolean
)

class MailboxContactDataSource(
    private val contactDao: ContactDao,
    private val contactRoutingIdDao: ContactRoutingIdDao
) {
    suspend fun findContactStates(): List<MailboxContactState> =
        contactDao
            .observeAll()
            .first()
            .map { contact ->
                val contactId = contact.contact.id
                val hasMutualIdentity =
                    contact.publicIdentity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name
                val hasRoutingId =
                    !contactRoutingIdDao.findRoutingIdByContactId(contactId).isNullOrBlank()
                MailboxContactState(
                    contactId = contactId,
                    isProvisioningEligible = hasMutualIdentity && hasRoutingId
                )
            }

    suspend fun findMutualSigningPublicKey(contactId: String): ByteArray? {
        val identity = contactDao.findPublicIdentityByContactId(contactId) ?: return null
        if (identity.keyExchangeStatus != KeyExchangeStatus.MUTUAL.name) return null
        return identity.signingPublicKey.copyOf()
    }
}
