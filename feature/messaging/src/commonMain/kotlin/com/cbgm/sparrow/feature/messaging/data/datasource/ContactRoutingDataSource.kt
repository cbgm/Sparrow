package com.cbgm.sparrow.feature.messaging.data.datasource

import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity
import com.cbgm.sparrow.data.database.model.ContactWithPublicIdentityDto
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator

class ContactRoutingDataSource(
    private val contactDao: ContactDao,
    private val contactRoutingIdDao: ContactRoutingIdDao,
    private val routingIdGenerator: RoutingIdGenerator
) {
    suspend fun resolve(contactId: String): Result<String> =
        runCatching {
            val contact = requireContact(contactId)
            if (contact.publicIdentity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name) {
                return@runCatching contact.canonicalRoutingId()
            }

            persistAndReturnBootstrapRoutingId(contact)
        }

    suspend fun resolveBootstrap(contactId: String): Result<String> =
        runCatching {
            persistAndReturnBootstrapRoutingId(requireContact(contactId))
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
