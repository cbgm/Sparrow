package com.cbgm.sparrow.feature.contacts.data.datasource

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.contacts.util.ContactVerificationPayloadEncoder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ContactVerificationDataSource(
    private val contactDao: ContactDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val detachedSignatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: ContactVerificationPayloadEncoder,
    private val protocolOutbox: ProtocolOutbox
) {
    private val mutex = Mutex()

    suspend fun verify(contactId: String) {
        require(contactId.isNotBlank()) {
            "Contact ID must not be blank"
        }

        mutex.withLock {
            val contact = contactDao.findById(contactId) ?: error("Contact not found: $contactId")
            val identity = contact.publicIdentity ?: error("Contact has no Sparrow identity")
            val updatedAt = SystemClock.nowEpochMilliseconds()
            val updatedRows =
                contactDao.updateVerificationStatusIfKeysMatch(
                    contactId = contactId,
                    expectedEncryptionPublicKey = identity.encryptionPublicKey,
                    expectedSigningPublicKey = identity.signingPublicKey,
                    verificationStatus = ContactVerificationStatus.VERIFIED.name,
                    updatedAtEpochMilliseconds = updatedAt
                )

            check(updatedRows == 1) {
                "Contact identity changed before verification was saved"
            }

            sendReceiptIfLocallyVerifiedLocked(contactId)
        }
    }

    suspend fun sendReceiptIfLocallyVerified(contactId: String) {
        require(contactId.isNotBlank()) {
            "Contact ID must not be blank"
        }

        mutex.withLock {
            sendReceiptIfLocallyVerifiedLocked(contactId)
        }
    }

    suspend fun receiveReceipt(
        context: IncomingPacketContext,
        packet: ContactVerificationReceiptPacket
    ) {
        mutex.withLock {
            check(context.transportMode == SEALED_BOX_TRANSPORT_MODE) {
                "Contact verification receipt must be received through encrypted transport"
            }
            check(packet.packetId == "contact-verification-receipt-${packet.receiptId}") {
                "Verification receipt packet ID does not match its receipt ID"
            }
            require(
                packet.verifiedAtEpochMilliseconds <=
                    context.receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS
            ) {
                "Verification receipt was created too far in the future"
            }

            val contact = contactDao.findById(context.contactId) ?: error("Contact was not found")
            val identity = contact.publicIdentity ?: error("Contact has no Sparrow identity")

            check(identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name) {
                "Verification receipt requires mutual key exchange"
            }
            check(identity.encryptionPublicKey.contentEquals(packet.senderEncryptionPublicKey)) {
                "Verification receipt sender encryption key does not match the contact"
            }
            check(identity.signingPublicKey.contentEquals(packet.senderSigningPublicKey)) {
                "Verification receipt sender signing key does not match the contact"
            }

            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            check(localIdentity.encryptionPublicKey.contentEquals(packet.verifiedEncryptionPublicKey)) {
                "Verification receipt refers to a different local encryption key"
            }
            check(localIdentity.signingPublicKey.contentEquals(packet.verifiedSigningPublicKey)) {
                "Verification receipt refers to a different local signing key"
            }

            val payload =
                payloadEncoder.encodeReceipt(
                    packetId = packet.packetId,
                    version = packet.version,
                    receiptId = packet.receiptId,
                    verifiedAtEpochMilliseconds = packet.verifiedAtEpochMilliseconds,
                    senderEncryptionPublicKey = packet.senderEncryptionPublicKey,
                    senderSigningPublicKey = packet.senderSigningPublicKey,
                    verifiedEncryptionPublicKey = packet.verifiedEncryptionPublicKey,
                    verifiedSigningPublicKey = packet.verifiedSigningPublicKey
                )

            detachedSignatureCrypto
                .verify(payload, identity.signingPublicKey, packet.signature)
                .getOrThrow()

            val updatedRows =
                contactDao.markVerifiedByContactIfKeysMatch(
                    contactId = context.contactId,
                    expectedEncryptionPublicKey = packet.senderEncryptionPublicKey,
                    expectedSigningPublicKey = packet.senderSigningPublicKey,
                    mutualStatus = KeyExchangeStatus.MUTUAL.name,
                    updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                )

            check(updatedRows == 1) {
                "Contact identity changed before verification receipt was applied"
            }
        }
    }

    private suspend fun sendReceiptIfLocallyVerifiedLocked(contactId: String) {
        val contact = contactDao.findById(contactId) ?: error("Contact not found: $contactId")
        val identity = contact.publicIdentity ?: error("Contact has no Sparrow identity")

        if (
            identity.verificationStatus != ContactVerificationStatus.VERIFIED.name ||
            identity.keyExchangeStatus != KeyExchangeStatus.MUTUAL.name
        ) {
            return
        }

        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        requireLocalKeysMatch(localIdentity, signingKeyPair)

        val receiptId = IdGenerator.generate()
        val packetId = "contact-verification-receipt-$receiptId"
        val verifiedAt = SystemClock.nowEpochMilliseconds()
        val payload =
            payloadEncoder.encodeReceipt(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                receiptId = receiptId,
                verifiedAtEpochMilliseconds = verifiedAt,
                senderEncryptionPublicKey = localIdentity.encryptionPublicKey,
                senderSigningPublicKey = localIdentity.signingPublicKey,
                verifiedEncryptionPublicKey = identity.encryptionPublicKey,
                verifiedSigningPublicKey = identity.signingPublicKey
            )
        val signature = detachedSignatureCrypto.sign(payload, signingKeyPair.privateKey).getOrThrow()

        protocolOutbox
            .enqueue(
                contactId = contactId,
                packet =
                    ContactVerificationReceiptPacket(
                        packetId = packetId,
                        receiptId = receiptId,
                        verifiedAtEpochMilliseconds = verifiedAt,
                        senderEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        senderSigningPublicKey = localIdentity.signingPublicKey.copyOf(),
                        verifiedEncryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                        verifiedSigningPublicKey = identity.signingPublicKey.copyOf(),
                        signature = signature.copyOf()
                    )
            ).getOrThrow()
    }

    private fun requireLocalKeysMatch(
        identity: LocalPublicIdentity,
        signingKeyPair: LocalSigningKeyPair
    ) {
        check(identity.signingPublicKey.contentEquals(signingKeyPair.publicKey)) {
            "Local signing key pair does not match the public identity"
        }
    }

    private companion object {
        const val MAX_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
        const val SEALED_BOX_TRANSPORT_MODE = "SEALED_BOX"
    }
}
