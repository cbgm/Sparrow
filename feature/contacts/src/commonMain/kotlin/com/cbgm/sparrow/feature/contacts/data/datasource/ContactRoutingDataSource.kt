package com.cbgm.sparrow.feature.contacts.data.datasource

import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity
import com.cbgm.sparrow.data.database.model.ContactWithPublicIdentityDto
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator

class ContactRoutingDataSource(
    private val contactDao: ContactDao,
    private val contactRoutingIdDao: ContactRoutingIdDao,
    private val routingIdGenerator: RoutingIdGenerator
) {
    suspend fun resolve(contactId: String): String {
        val contact = requireContact(contactId)
        if (contact.publicIdentity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name) {
            return contact.canonicalRoutingId()
        }

        return persistAndReturnBootstrapRoutingId(contact)
    }

    suspend fun resolveBootstrap(contactId: String): String = persistAndReturnBootstrapRoutingId(requireContact(contactId))

    /**
     * Direct invitation/response packets are signed, explicit handshake traffic,
     * not encrypted chat messages. Prefer phone bootstrap as before, including
     * when the remote installation has different keys. If a known peer has NO
     * phone/bootstrap address (e.g. a key-only contact who restored the ORIGINAL
     * signing identity), the already-recorded public key supplies its canonical
     * route. Never use this fallback for ordinary messages or groups.
     *
     * The fallback is delivery to that SAME signing identity, not an assertion
     * that an installation with different keys owns the old address.
     */
    suspend fun resolveInvitation(contactId: String): String {
        val contact = requireContact(contactId)
        val savedBootstrap = contactRoutingIdDao.findRoutingIdByContactId(contactId)
            ?.takeIf { it.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX) }
        val preferredPhone = contact.phoneNumbers
            .firstOrNull { it.id == contact.contact.preferredPhoneNumberId }
            ?.value?.trim()?.takeIf(String::isNotEmpty)
        if (savedBootstrap != null || preferredPhone != null) {
            return persistAndReturnBootstrapRoutingId(contact)
        }
        val signingKey = contact.publicIdentity?.signingPublicKey
            ?.takeIf { it.isNotEmpty() }
            ?: error("Contact has no phone/bootstrap address or known signing identity for an invitation")
        return routingIdGenerator.deriveFromSigningPublicKey(signingKey).getOrThrow()
    }

    private suspend fun requireContact(contactId: String): ContactWithPublicIdentityDto {
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        return contactDao.findById(contactId) ?: error("Contact was not found")
    }

    private suspend fun persistAndReturnBootstrapRoutingId(
        contact: ContactWithPublicIdentityDto
    ): String {
        val contactId = contact.contact.id
        val routingId = contact.bootstrapRoutingId()
        if (contactRoutingIdDao.findRoutingIdByContactId(contactId) != routingId) {
            contactRoutingIdDao.deleteOtherContactMapping(
                routingId = routingId,
                contactId = contactId
            )
            contactRoutingIdDao.upsert(ContactRoutingIdEntity(contactId, routingId))
        }
        return routingId
    }

    private fun ContactWithPublicIdentityDto.canonicalRoutingId(): String =
        routingIdGenerator
            .deriveFromSigningPublicKey(checkNotNull(publicIdentity).signingPublicKey)
            .getOrThrow()

    private suspend fun ContactWithPublicIdentityDto.bootstrapRoutingId(): String {
        contactRoutingIdDao
            .findRoutingIdByContactId(contact.id)
            ?.takeIf { routingId -> routingId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX) }
            ?.let { routingId -> return routingId }

        val phoneNumber =
            phoneNumbers
                .firstOrNull { phoneNumber -> phoneNumber.id == contact.preferredPhoneNumberId }
                ?.value
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: error("Contact has no phone number or bootstrap routing mapping")

        return routingIdGenerator.deriveFromPhoneNumber(phoneNumber).getOrThrow()
    }

    private companion object {
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
    }
}
