package com.cbgm.sparrow.feature.messaging.data.datasource

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity
import com.cbgm.sparrow.data.database.model.ContactWithPublicIdentity
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.transport.routing.RoutingIdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ContactRoutingReconciliationDataSource(
    private val contactDao: ContactDao,
    private val contactRoutingIdDao: ContactRoutingIdDao,
    private val routingIdGenerator: RoutingIdGenerator
) {
    private val reconcileMutex = Mutex()
    private var lastReconcileAtEpochMilliseconds: Long = 0L

    suspend fun reconcileKnownContacts(): Result<Unit> =
        runCatching {
            if (!shouldReconcileNow()) return@runCatching

            val contacts = contactDao.observeAll().first()
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
                if (matchingContactId == mappedContactId) return@forEach

                persistBootstrapMapping(matchingContactId, routingId)
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

    private fun findMatchingContactId(
        routingId: String,
        contacts: List<ContactWithPublicIdentity>
    ): String? {
        val matches =
            contacts.filter { contact ->
                contact.phoneNumbers.any { phoneNumber ->
                    routingIdGenerator
                        .deriveFromPhoneNumber(phoneNumber.value)
                        .getOrNull() == routingId
                }
            }

        return matches
            .firstOrNull { contact ->
                contact.contact.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED.name
            }?.contact?.id
            ?: matches.firstOrNull { contact -> contact.publicIdentity != null }?.contact?.id
            ?: matches.firstOrNull()?.contact?.id
    }

    private suspend fun deleteAnonymousBootstrapPlaceholder(contactId: String) {
        val contact = contactDao.findById(contactId) ?: return
        val anonymous =
            contact.contact.displayName.isNullOrBlank() &&
                contact.contact.deviceContactId == null &&
                contact.phoneNumbers.isEmpty() &&
                contact.publicIdentity == null
        if (anonymous) contactDao.deleteById(contactId)
    }

    private suspend fun persistBootstrapMapping(
        contactId: String,
        routingId: String
    ) {
        if (contactRoutingIdDao.findRoutingIdByContactId(contactId) == routingId) return
        contactRoutingIdDao.deleteOtherContactMapping(routingId, contactId)
        contactRoutingIdDao.upsert(ContactRoutingIdEntity(contactId, routingId))
    }

    private companion object {
        const val RECONCILE_THROTTLE_MILLISECONDS = 30_000L
    }
}
