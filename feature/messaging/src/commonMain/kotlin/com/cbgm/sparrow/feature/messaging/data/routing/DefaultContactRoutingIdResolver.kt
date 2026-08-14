package com.cbgm.sparrow.feature.messaging.data.routing

import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.messaging.application.routing.ContactRoutingIdResolver
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator

class DefaultContactRoutingIdResolver(
    private val getContact: GetContactUseCase,
    private val contactRoutingIdDao: ContactRoutingIdDao,
    private val routingIdGenerator: RoutingIdGenerator
) : ContactRoutingIdResolver {
    override suspend fun resolve(contactId: String): Result<String> =
        runCatching {
            val contact = requireContact(contactId)
            if (contact.sparrowIdentity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
                return@runCatching contact.canonicalRoutingId()
            }

            persistAndReturnBootstrapRoutingId(
                contactId = contactId,
                contact = contact
            )
        }

    override suspend fun resolveBootstrap(contactId: String): Result<String> =
        runCatching {
            persistAndReturnBootstrapRoutingId(
                contactId = contactId,
                contact = requireContact(contactId)
            )
        }

    private suspend fun requireContact(contactId: String): Contact {
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        return getContact(contactId).getOrThrow() ?: error("Contact was not found")
    }

    private suspend fun persistAndReturnBootstrapRoutingId(
        contactId: String,
        contact: Contact
    ): String {
        val routingId = contact.bootstrapRoutingId(contactId)
        if (contactRoutingIdDao.findRoutingIdByContactId(contactId) != routingId) {
            contactRoutingIdDao.deleteOtherContactMapping(
                routingId = routingId,
                contactId = contactId
            )
            contactRoutingIdDao.upsert(ContactRoutingIdEntity(contactId, routingId))
        }
        return routingId
    }

    private fun Contact.canonicalRoutingId(): String =
        routingIdGenerator
            .deriveFromSigningPublicKey(checkNotNull(sparrowIdentity).signingPublicKey)
            .getOrThrow()

    private suspend fun Contact.bootstrapRoutingId(contactId: String): String {
        contactRoutingIdDao
            .findRoutingIdByContactId(contactId)
            ?.takeIf { routingId -> routingId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX) }
            ?.let { routingId -> return routingId }

        val phoneNumber =
            preferredPhoneNumber
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
