package com.cbgm.securechat.feature.messaging.application.outbox

import com.cbgm.securechat.core.crypto.transport.EncryptedTransportPayload
import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.crypto.transport.TransportMessageCipher
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus

interface OutgoingTransportPayloadFactory {
    suspend fun create(
        encodedPacket: ByteArray,
        packet: SecureChatPacket,
        contact: Contact
    ): Result<EncryptedTransportPayload>
}

class DefaultOutgoingTransportPayloadFactory(
    private val transportMessageCipher: TransportMessageCipher,
    private val packetTransportPolicy: OutgoingPacketTransportPolicy
) : OutgoingTransportPayloadFactory {
    override suspend fun create(
        encodedPacket: ByteArray,
        packet: SecureChatPacket,
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

            val identity = contact.secureChatIdentity
            val canEncrypt =
                identity != null &&
                    identity.encryptionPublicKey.isNotEmpty() &&
                    (
                        identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL ||
                            requirement.allowsEncryptionBeforeMutualIdentity
                    )

            if (!canEncrypt) {
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
                    recipientPublicKey = checkNotNull(identity).encryptionPublicKey
                ).getOrThrow()
        }

    private companion object {
        const val TRANSPORT_VERSION = 1
    }
}
