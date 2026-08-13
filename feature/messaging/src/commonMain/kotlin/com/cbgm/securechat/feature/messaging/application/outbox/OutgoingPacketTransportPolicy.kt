package com.cbgm.securechat.feature.messaging.application.outbox

import com.cbgm.securechat.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.securechat.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.ContactInvitePacket
import com.cbgm.securechat.core.protocol.packet.ContactReadyPacket
import com.cbgm.securechat.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.securechat.core.protocol.packet.MailboxRoutePacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.contacts.domain.model.Contact

data class OutgoingTransportRequirement(
    val requiresEncryption: Boolean,
    val allowsEncryptionBeforeMutualIdentity: Boolean = false,
    val forcePlaintext: Boolean = false,
    val encryptionUnavailableMessage: String = "This protocol packet requires an encrypted SecureChat transport"
)

interface OutgoingPacketTransportPolicy {
    fun resolve(
        packet: SecureChatPacket,
        contact: Contact
    ): Result<OutgoingTransportRequirement>
}

class DefaultOutgoingPacketTransportPolicy : OutgoingPacketTransportPolicy {
    override fun resolve(
        packet: SecureChatPacket,
        contact: Contact
    ): Result<OutgoingTransportRequirement> =
        runCatching {
            when (packet) {
                is ContactInvitePacket,
                is ContactInviteAcceptedPacket,
                is ContactInviteDeclinedPacket,
                is GroupInvitePacket,
                is GroupInviteReceivedPacket,
                is GroupJoinRequestPacket,
                is GroupInviteDeclinedPacket ->
                    OutgoingTransportRequirement(
                        requiresEncryption = false,
                        forcePlaintext = true
                    )

                is ContactReadyPacket -> {
                    validateContactReadyIdentity(packet = packet, contact = contact)
                    OutgoingTransportRequirement(
                        requiresEncryption = true,
                        allowsEncryptionBeforeMutualIdentity = true,
                        encryptionUnavailableMessage =
                            "Contact ready packet requires an encrypted SecureChat transport"
                    )
                }

                is ContactVerificationReceiptPacket -> {
                    validateVerificationReceiptIdentity(packet = packet, contact = contact)
                    OutgoingTransportRequirement(
                        requiresEncryption = true,
                        encryptionUnavailableMessage =
                            "Contact verification receipt requires an encrypted SecureChat transport"
                    )
                }

                is GroupCreatedPacket,
                is GroupLeaveRequestPacket,
                is GroupMemberActivatedPacket,
                is GroupMemberActivationAcknowledgementPacket,
                is GroupVerificationReceiptPacket,
                is GroupVerificationSnapshotRequestPacket,
                is GroupVerificationSnapshotPacket,
                is MailboxRoutePacket ->
                    OutgoingTransportRequirement(
                        requiresEncryption = true,
                        encryptionUnavailableMessage =
                            "Protocol packet requires a mutual SecureChat key exchange"
                    )

                else -> OutgoingTransportRequirement(requiresEncryption = false)
            }
        }

    private fun validateContactReadyIdentity(
        packet: ContactReadyPacket,
        contact: Contact
    ) {
        val identity =
            checkNotNull(contact.secureChatIdentity) {
                "Contact ready packet requires a stored recipient identity"
            }

        check(identity.encryptionPublicKey.contentEquals(packet.acceptedResponderEncryptionPublicKey)) {
            "Contact identity changed before the ready packet was encrypted"
        }
        check(identity.signingPublicKey.contentEquals(packet.acceptedResponderSigningPublicKey)) {
            "Contact signing identity changed before the ready packet was encrypted"
        }
    }

    private fun validateVerificationReceiptIdentity(
        packet: ContactVerificationReceiptPacket,
        contact: Contact
    ) {
        val identity =
            checkNotNull(contact.secureChatIdentity) {
                "Contact verification receipt requires a stored recipient identity"
            }

        check(identity.encryptionPublicKey.contentEquals(packet.verifiedEncryptionPublicKey)) {
            "Contact identity changed before the verification receipt was encrypted"
        }
        check(identity.signingPublicKey.contentEquals(packet.verifiedSigningPublicKey)) {
            "Contact signing identity changed before the verification receipt was encrypted"
        }
    }
}
