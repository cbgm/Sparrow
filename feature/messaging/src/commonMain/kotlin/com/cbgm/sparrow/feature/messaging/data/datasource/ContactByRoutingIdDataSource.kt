package com.cbgm.sparrow.feature.messaging.data.datasource

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity
import com.cbgm.sparrow.data.database.model.ContactWithPublicIdentityDto
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator
import kotlinx.coroutines.flow.first

class ContactByRoutingIdDataSource(
    private val contactDao: ContactDao,
    private val contactRoutingIdDao: ContactRoutingIdDao,
    private val routingIdGenerator: RoutingIdGenerator,
    private val groupRoutingDataSource: GroupRoutingDataSource
) {
    suspend fun resolveContactId(routingId: String): String? {
        require(routingId.isNotBlank()) { "Routing ID must not be blank" }

        if (routingId.isBootstrap()) {
            contactRoutingIdDao.findContactIdByRoutingId(routingId)?.let { contactId ->
                return contactId
            }
        }

        val contacts = contactDao.observeAll().first()
        findMatchingContactId(routingId, contacts)?.let { contactId ->
            persistBootstrapMapping(contactId, routingId)
            return contactId
        }

        if (!routingId.isBootstrap()) {
            return groupRoutingDataSource.resolveContactId(routingId)
        }

        return createBootstrapPlaceholder(routingId)
    }

    private suspend fun findMatchingContactId(
        routingId: String,
        contacts: List<ContactWithPublicIdentityDto>
    ): String? {
        val bootstrap = routingId.isBootstrap()
        val matches =
            contacts.filter { contact ->
                if (bootstrap) {
                    contact.phoneNumbers.any { phoneNumber ->
                        routingIdGenerator
                            .deriveFromPhoneNumber(phoneNumber.value)
                            .getOrNull() == routingId
                    }
                } else {
                    contact.publicIdentity
                        ?.signingPublicKey
                        ?.let(routingIdGenerator::deriveFromSigningPublicKey)
                        ?.getOrNull() == routingId
                }
            }

        if (matches.isEmpty()) return null
        if (bootstrap) {
            return matches
                .firstOrNull { contact ->
                    contact.contact.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED.name
                }?.contact?.id
                ?: matches.firstOrNull { contact -> contact.publicIdentity != null }?.contact?.id
                ?: matches.first().contact.id
        }

        return matches
            .firstOrNull { contact -> hasBootstrapMapping(contact.contact.id) }
            ?.contact
            ?.id
            ?: matches.first().contact.id
    }

    private suspend fun createBootstrapPlaceholder(routingId: String): String {
        val now = SystemClock.nowEpochMilliseconds()
        val contactId = IdGenerator.generate()
        contactDao.upsertContact(
            ContactEntity(
                id = contactId,
                displayName = null,
                deviceContactId = null,
                deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED.name,
                preferredPhoneNumberId = null,
                createdAtEpochMilliseconds = now,
                updatedAtEpochMilliseconds = now
            )
        )
        contactRoutingIdDao.upsert(ContactRoutingIdEntity(contactId, routingId))
        return contactId
    }

    private suspend fun hasBootstrapMapping(contactId: String): Boolean =
        contactRoutingIdDao
            .findRoutingIdByContactId(contactId)
            ?.isBootstrap() == true

    private suspend fun persistBootstrapMapping(
        contactId: String,
        routingId: String
    ) {
        if (!routingId.isBootstrap()) return
        if (contactRoutingIdDao.findRoutingIdByContactId(contactId) == routingId) return
        contactRoutingIdDao.deleteOtherContactMapping(routingId, contactId)
        contactRoutingIdDao.upsert(ContactRoutingIdEntity(contactId, routingId))
    }

    private fun String.isBootstrap(): Boolean = startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)

    private companion object {
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
    }
}
