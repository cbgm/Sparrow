package com.cbgm.sparrow.feature.messaging.data.routing

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.messaging.application.routing.ContactByRoutingIdResolver
import com.cbgm.sparrow.feature.messaging.application.routing.GroupRoutingIdResolver
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultContactByRoutingIdResolver(
    private val contactRepository: ContactRepository,
    private val contactDao: ContactDao,
    private val contactRoutingIdDao: ContactRoutingIdDao,
    private val routingIdGenerator: RoutingIdGenerator,
    private val groupRoutingIdResolver: GroupRoutingIdResolver
) : ContactByRoutingIdResolver {
    private val reconcileMutex = Mutex()
    private var lastReconcileAtEpochMilliseconds: Long = 0L

    override suspend fun resolveContactId(routingId: String): Result<String?> =
        runCatching {
            require(routingId.isNotBlank()) {
                "Routing ID must not be blank"
            }

            val bootstrap = routingId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)

            if (bootstrap) {
                val mappedContactId = contactRoutingIdDao.findContactIdByRoutingId(routingId)
                if (mappedContactId != null) {
                    /*
                     * Trust the cached mapping instead of re-scanning every
                     * contact (and re-hashing every phone number) on every
                     * message. If the mapping has drifted - e.g. this
                     * bootstrap contact just got linked to a real device
                     * contact - the periodic reconcileKnownContacts() sweep
                     * repairs it within its throttle window instead of doing
                     * that work inline on the hot path.
                     */
                    return@runCatching mappedContactId
                }
            }

            val contacts = contactRepository.observeContacts().first()

            val matchingContactId = findMatchingContactId(routingId, contacts)
            if (matchingContactId != null) {
                persistBootstrapMapping(
                    contactId = matchingContactId,
                    routingId = routingId
                )
                return@runCatching matchingContactId
            }

            if (!routingId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)) {
                return@runCatching groupRoutingIdResolver
                    .resolveContactId(routingId)
                    .getOrThrow()
            }

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

            contactId
        }

    override suspend fun reconcileKnownContacts(): Result<Unit> =
        runCatching {
            /*
             * This walks every known contact's phone numbers, re-derives their
             * routing id (SHA-256) and does a DB lookup for each one. It exists
             * to repair stale bootstrap routing mappings (e.g. after a contact
             * is imported or merged), not to gate delivery of the message that
             * just arrived. Running it on every single incoming envelope made
             * receiving messages O(messages * contacts) and serialized behind
             * envelope processing. Throttle it to a periodic background sweep
             * instead - callers can keep invoking it per-message for free.
             */
            if (!shouldReconcileNow()) {
                return@runCatching
            }

            val contacts = contactRepository.observeContacts().first()
            val bootstrapRoutingIds =
                contacts
                    .flatMap { contact ->
                        contact.phoneNumbers.mapNotNull { phoneNumber ->
                            routingIdGenerator
                                .deriveFromPhoneNumber(phoneNumber.value)
                                .getOrNull()
                        }
                    }.distinct()

            bootstrapRoutingIds.forEach { routingId ->
                val mappedContactId =
                    contactRoutingIdDao.findContactIdByRoutingId(routingId)
                        ?: return@forEach
                val matchingContactId =
                    findMatchingContactId(routingId, contacts)
                        ?: return@forEach
                if (matchingContactId == mappedContactId) {
                    return@forEach
                }

                persistBootstrapMapping(
                    contactId = matchingContactId,
                    routingId = routingId
                )
                deleteAnonymousBootstrapPlaceholder(mappedContactId)
            }
        }

    private suspend fun shouldReconcileNow(): Boolean =
        reconcileMutex.withLock {
            val now = SystemClock.nowEpochMilliseconds()
            if (now - lastReconcileAtEpochMilliseconds < RECONCILE_THROTTLE_MILLISECONDS) {
                false
            } else {
                lastReconcileAtEpochMilliseconds = now
                true
            }
        }

    private suspend fun findMatchingContactId(
        routingId: String,
        contacts: List<Contact>
    ): String? {
        val bootstrap = routingId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)
        val matches =
            contacts.filter { contact ->
                if (bootstrap) {
                    contact.phoneNumbers.any { phoneNumber ->
                        routingIdGenerator
                            .deriveFromPhoneNumber(phoneNumber.value)
                            .getOrNull() == routingId
                    }
                } else {
                    contact.sparrowIdentity
                        ?.signingPublicKey
                        ?.let(routingIdGenerator::deriveFromSigningPublicKey)
                        ?.getOrNull() == routingId
                }
            }

        if (matches.isEmpty()) {
            return null
        }
        if (bootstrap) {
            return matches
                .firstOrNull { contact ->
                    contact.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
                }?.id
                ?: matches.firstOrNull { contact -> contact.sparrowIdentity != null }?.id
                ?: matches.first().id
        }

        return matches.firstOrNull { contact -> hasBootstrapMapping(contact.id) }?.id
            ?: matches.first().id
    }

    private suspend fun hasBootstrapMapping(contactId: String): Boolean =
        contactRoutingIdDao
            .findRoutingIdByContactId(contactId)
            ?.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX) == true

    private suspend fun deleteAnonymousBootstrapPlaceholder(contactId: String) {
        val contact = contactDao.findById(contactId) ?: return
        val isAnonymousPlaceholder =
            contact.contact.displayName.isNullOrBlank() &&
                contact.contact.deviceContactId == null &&
                contact.phoneNumbers.isEmpty() &&
                contact.publicIdentity == null
        if (!isAnonymousPlaceholder) return

        contactDao.deleteById(contactId)
    }

    private suspend fun persistBootstrapMapping(
        contactId: String,
        routingId: String
    ) {
        if (!routingId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)) {
            return
        }
        if (contactRoutingIdDao.findRoutingIdByContactId(contactId) == routingId) {
            return
        }
        contactRoutingIdDao.deleteOtherContactMapping(
            routingId = routingId,
            contactId = contactId
        )
        contactRoutingIdDao.upsert(ContactRoutingIdEntity(contactId, routingId))
    }

    private companion object {
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
        const val RECONCILE_THROTTLE_MILLISECONDS = 30_000L
    }
}
