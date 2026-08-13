package com.cbgm.securechat.feature.messaging.data.relay

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.ContactRelayIdDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactRelayIdEntity
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.messaging.application.relay.ContactByRelayIdResolver
import com.cbgm.securechat.feature.messaging.application.relay.GroupRelayIdResolver
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator
import kotlinx.coroutines.flow.first

class DefaultContactByRelayIdResolver(
    private val contactRepository: ContactRepository,
    private val contactDao: ContactDao,
    private val contactRelayIdDao: ContactRelayIdDao,
    private val relayIdGenerator: RelayIdGenerator,
    private val groupRelayIdResolver: GroupRelayIdResolver
) : ContactByRelayIdResolver {
    override suspend fun resolveContactId(relayId: String): Result<String?> =
        runCatching {
            require(relayId.isNotBlank()) {
                "Relay ID must not be blank"
            }

            val bootstrap = relayId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)
            val contacts = contactRepository.observeContacts().first()
            if (bootstrap) {
                val mappedContactId = contactRelayIdDao.findContactIdByRelayId(relayId)
                if (mappedContactId != null) {
                    val matchingContactId = findMatchingContactId(relayId, contacts)
                    if (matchingContactId != null && matchingContactId != mappedContactId) {
                        persistBootstrapMapping(
                            contactId = matchingContactId,
                            relayId = relayId
                        )
                        deleteAnonymousBootstrapPlaceholder(mappedContactId)
                        return@runCatching matchingContactId
                    }
                    return@runCatching mappedContactId
                }
            }

            val matchingContactId = findMatchingContactId(relayId, contacts)
            if (matchingContactId != null) {
                persistBootstrapMapping(
                    contactId = matchingContactId,
                    relayId = relayId
                )
                return@runCatching matchingContactId
            }

            if (!relayId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)) {
                return@runCatching groupRelayIdResolver
                    .resolveContactId(relayId)
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
            contactRelayIdDao.upsert(ContactRelayIdEntity(contactId, relayId))

            contactId
        }

    override suspend fun reconcileKnownContacts(): Result<Unit> =
        runCatching {
            val contacts = contactRepository.observeContacts().first()
            val bootstrapRelayIds =
                contacts
                    .flatMap { contact ->
                        contact.phoneNumbers.mapNotNull { phoneNumber ->
                            relayIdGenerator
                                .deriveFromPhoneNumber(phoneNumber.value)
                                .getOrNull()
                        }
                    }.distinct()

            bootstrapRelayIds.forEach { relayId ->
                val mappedContactId =
                    contactRelayIdDao.findContactIdByRelayId(relayId)
                        ?: return@forEach
                val matchingContactId =
                    findMatchingContactId(relayId, contacts)
                        ?: return@forEach
                if (matchingContactId == mappedContactId) {
                    return@forEach
                }

                persistBootstrapMapping(
                    contactId = matchingContactId,
                    relayId = relayId
                )
                deleteAnonymousBootstrapPlaceholder(mappedContactId)
            }
        }

    private suspend fun findMatchingContactId(
        relayId: String,
        contacts: List<Contact>
    ): String? {
        val bootstrap = relayId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)
        val matches =
            contacts.filter { contact ->
                if (bootstrap) {
                    contact.phoneNumbers.any { phoneNumber ->
                        relayIdGenerator
                            .deriveFromPhoneNumber(phoneNumber.value)
                            .getOrNull() == relayId
                    }
                } else {
                    contact.secureChatIdentity
                        ?.signingPublicKey
                        ?.let(relayIdGenerator::deriveFromSigningPublicKey)
                        ?.getOrNull() == relayId
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
                ?: matches.firstOrNull { contact -> contact.secureChatIdentity != null }?.id
                ?: matches.first().id
        }

        return matches.firstOrNull { contact -> hasBootstrapMapping(contact.id) }?.id
            ?: matches.first().id
    }

    private suspend fun hasBootstrapMapping(contactId: String): Boolean =
        contactRelayIdDao
            .findRelayIdByContactId(contactId)
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
        relayId: String
    ) {
        if (!relayId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)) {
            return
        }
        if (contactRelayIdDao.findRelayIdByContactId(contactId) == relayId) {
            return
        }
        contactRelayIdDao.deleteOtherContactMapping(
            relayId = relayId,
            contactId = contactId
        )
        contactRelayIdDao.upsert(ContactRelayIdEntity(contactId, relayId))
    }

    private companion object {
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
    }
}
