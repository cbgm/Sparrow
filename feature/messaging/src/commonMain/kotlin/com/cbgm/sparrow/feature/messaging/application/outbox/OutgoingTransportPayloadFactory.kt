package com.cbgm.sparrow.feature.messaging.application.outbox

import com.cbgm.sparrow.core.crypto.transport.EncryptedTransportPayload
import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.crypto.transport.TransportMessageCipher
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.messaging.application.routing.GroupTransportKeyResolver

interface OutgoingTransportPayloadFactory {
    suspend fun create(
        encodedPacket: ByteArray,
        packet: SparrowPacket,
        contact: Contact
    ): Result<EncryptedTransportPayload>
}

class DefaultOutgoingTransportPayloadFactory(
    private val transportMessageCipher: TransportMessageCipher,
    private val packetTransportPolicy: OutgoingPacketTransportPolicy,
    private val groupTransportKeyResolver: GroupTransportKeyResolver
) : OutgoingTransportPayloadFactory {
    override suspend fun create(
        encodedPacket: ByteArray,
        packet: SparrowPacket,
        contact: Contact
    ): Result<EncryptedTransportPayload> =
        runCatching {
            require(encodedPacket.isNotEmpty()) {
                "Encoded protocol packet must not be empty"
            }

            val requirement =
                packetTransportPolicy
                    .resolve(packet = packet, contact = contact)
                    .getOrThrow()
            if (requirement.forcePlaintext) {
                return@runCatching EncryptedTransportPayload(
                    version = TRANSPORT_VERSION,
                    mode = TransportEncryptionMode.PLAINTEXT,
                    payload = encodedPacket
                )
            }

            val groupEncryptionPublicKey =
                groupTransportKeyResolver
                    .resolveEncryptionPublicKey(packet, contact.id)
                    .getOrThrow()
            val directEncryptionPublicKey = contact.directEncryptionPublicKey(requirement)
            val recipientEncryptionPublicKey = groupEncryptionPublicKey ?: directEncryptionPublicKey

            if (recipientEncryptionPublicKey == null) {
                check(!requirement.requiresEncryption) {
                    requirement.encryptionUnavailableMessage
                }

                return@runCatching EncryptedTransportPayload(
                    version = TRANSPORT_VERSION,
                    mode = TransportEncryptionMode.PLAINTEXT,
                    payload = encodedPacket
                )
            }

            transportMessageCipher
                .encryptForRecipient(
                    plaintext = encodedPacket,
                    recipientPublicKey = recipientEncryptionPublicKey
                ).getOrThrow()
        }

    private fun Contact.directEncryptionPublicKey(
        requirement: OutgoingTransportRequirement
    ): ByteArray? {
        val identity = sparrowIdentity ?: return null
        if (identity.encryptionPublicKey.isEmpty()) return null
        val canUseIdentity =
            identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL ||
                requirement.allowsEncryptionBeforeMutualIdentity
        return identity.encryptionPublicKey.takeIf { canUseIdentity }
    }

    private companion object {
        const val TRANSPORT_VERSION = 1
    }
}
