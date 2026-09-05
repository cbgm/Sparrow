package com.cbgm.sparrow.feature.messaging.runtime.outbox

import com.cbgm.sparrow.core.crypto.transport.EncryptedTransportPayload
import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.crypto.transport.TransportMessageCipher
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.messaging.data.datasource.GroupTransportKeyDataSource

class OutgoingTransportPayloadFactory(
    private val transportMessageCipher: TransportMessageCipher,
    private val packetTransportPolicy: OutgoingPacketTransportPolicy,
    private val groupTransportKeyDataSource: GroupTransportKeyDataSource
) {
    suspend fun create(
        encodedPacket: ByteArray,
        packet: SparrowPacket,
        contact: Contact
    ): Result<EncryptedTransportPayload> =
        safeSuspendCall {
            require(encodedPacket.isNotEmpty()) {
                "Encoded protocol packet must not be empty"
            }

            val requirement = packetTransportPolicy.resolve(packet, contact).getOrThrow()
            if (requirement.forcePlaintext) {
                return@safeSuspendCall plaintext(encodedPacket)
            }

            val groupEncryptionPublicKey =
                groupTransportKeyDataSource.resolveEncryptionPublicKey(packet, contact.id)
            val recipientEncryptionPublicKey =
                groupEncryptionPublicKey ?: contact.directEncryptionPublicKey(requirement)

            if (recipientEncryptionPublicKey == null) {
                check(!requirement.requiresEncryption) {
                    requirement.encryptionUnavailableMessage
                }
                return@safeSuspendCall plaintext(encodedPacket)
            }

            transportMessageCipher
                .encryptForRecipient(encodedPacket, recipientEncryptionPublicKey)
                .getOrThrow()
        }

    private fun plaintext(encodedPacket: ByteArray): EncryptedTransportPayload =
        EncryptedTransportPayload(
            version = TRANSPORT_VERSION,
            mode = TransportEncryptionMode.PLAINTEXT,
            payload = encodedPacket
        )

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
