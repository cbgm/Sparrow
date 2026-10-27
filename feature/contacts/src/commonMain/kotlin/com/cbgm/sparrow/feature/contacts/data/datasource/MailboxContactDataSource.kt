package com.cbgm.sparrow.feature.contacts.data.datasource

import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import kotlinx.coroutines.flow.first

data class MailboxContactStateDto(
    val contactId: String,
    val hasMutualIdentity: Boolean,
    val hasRoutingId: Boolean,
    val hasDirectConversation: Boolean,
    val sharesActiveGroup: Boolean
)

class MailboxContactDataSource(
    private val contactDao: ContactDao,
    private val contactRoutingIdDao: ContactRoutingIdDao
) {
    suspend fun findContactStates(): List<MailboxContactStateDto> {
        val directContactIds = contactDao.findDirectConversationContactIds().toSet()
        val groupContactIds = contactDao.findGroupParticipantContactIds().toSet()
        return contactDao
            .observeAll()
            .first()
            .map { contact ->
                val contactId = contact.contact.id
                MailboxContactStateDto(
                    contactId = contactId,
                    hasMutualIdentity = contact.publicIdentity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name,
                    hasRoutingId = !contactRoutingIdDao.findRoutingIdByContactId(contactId).isNullOrBlank(),
                    hasDirectConversation = contactId in directContactIds,
                    sharesActiveGroup = contactId in groupContactIds
                )
            }
    }

    suspend fun findMutualSigningPublicKey(contactId: String): ByteArray? {
        val identity = contactDao.findPublicIdentityByContactId(contactId) ?: return null
        if (identity.keyExchangeStatus != KeyExchangeStatus.MUTUAL.name) return null
        return identity.signingPublicKey.copyOf()
    }
}
