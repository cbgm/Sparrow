package com.cbgm.securechat.feature.messaging.data.relay

import com.cbgm.securechat.data.database.dao.ContactRelayIdDao
import com.cbgm.securechat.data.database.entity.ContactRelayIdEntity
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.messaging.domain.relay.ContactRelayIdResolver
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator

class DefaultContactRelayIdResolver(
    private val getContact: GetContact,
    private val contactRelayIdDao: ContactRelayIdDao,
    private val relayIdGenerator: RelayIdGenerator
) : ContactRelayIdResolver {
    override suspend fun resolve(contactId: String): Result<String> =
        runCatching {
            val contact = requireContact(contactId)
            if (contact.secureChatIdentity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
                return@runCatching contact.canonicalRelayId()
            }

            persistAndReturnBootstrapRelayId(
                contactId = contactId,
                contact = contact
            )
        }

    override suspend fun resolveBootstrap(contactId: String): Result<String> =
        runCatching {
            persistAndReturnBootstrapRelayId(
                contactId = contactId,
                contact = requireContact(contactId)
            )
        }

    private suspend fun requireContact(contactId: String): Contact {
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        return getContact(contactId).getOrThrow() ?: error("Contact was not found")
    }

    private suspend fun persistAndReturnBootstrapRelayId(
        contactId: String,
        contact: Contact
    ): String {
        val relayId = contact.bootstrapRelayId(contactId)
        if (contactRelayIdDao.findRelayIdByContactId(contactId) != relayId) {
            contactRelayIdDao.deleteOtherContactMapping(
                relayId = relayId,
                contactId = contactId
            )
            contactRelayIdDao.upsert(ContactRelayIdEntity(contactId, relayId))
        }
        return relayId
    }

    private fun Contact.canonicalRelayId(): String =
        relayIdGenerator
            .deriveFromSigningPublicKey(checkNotNull(secureChatIdentity).signingPublicKey)
            .getOrThrow()

    private suspend fun Contact.bootstrapRelayId(contactId: String): String {
        contactRelayIdDao
            .findRelayIdByContactId(contactId)
            ?.takeIf { relayId -> relayId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX) }
            ?.let { relayId -> return relayId }

        val phoneNumber =
            preferredPhoneNumber
                ?.value
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: error("Contact has no phone number or bootstrap relay mapping")

        return relayIdGenerator.deriveFromPhoneNumber(phoneNumber).getOrThrow()
    }

    private companion object {
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
    }
}
