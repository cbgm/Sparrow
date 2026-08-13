package com.cbgm.securechat.feature.contacts.data.exchange

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.IdentityPacket
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.feature.contacts.domain.repository.IdentityInvitationRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ManualIdentityExchange(
    private val contactDao: ContactDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val identityInvitationRepository: IdentityInvitationRepository
) {
    private val mutex = Mutex()

    private val currentlyStarting = mutableSetOf<String>()

    suspend fun ensureStarted(contactId: String): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val mayStart =
                mutex.withLock {
                    currentlyStarting.add(contactId)
                }

            if (!mayStart) {
                return@runCatching
            }

            try {
                identityInvitationRepository.cancelForManualSetup(contactId).getOrThrow()

                val contact =
                    contactDao.findById(contactId = contactId)
                        ?: error("Contact was not found: $contactId")

                val remoteIdentity =
                    contact.publicIdentity
                        ?: error("Import or scan the contact identity before starting manual setup")

                check(remoteIdentity.locallyImported) {
                    "Import or scan the contact identity before starting manual setup"
                }

                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()

                val packet =
                    IdentityPacket(
                        packetId = IdGenerator.generate(),
                        displayName = null,
                        encryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        signingPublicKey = localIdentity.signingPublicKey.copyOf()
                    )

                protocolOutbox
                    .enqueue(
                        contactId = contactId,
                        packet = packet
                    ).getOrThrow()
            } finally {
                mutex.withLock {
                    currentlyStarting.remove(contactId)
                }
            }
        }
}
