package com.cbgm.sparrow.feature.messaging.runtime.outbox

import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.sparrow.core.protocol.packet.MailboxRoutePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.domain.model.Contact

class OutgoingPacketTransportPolicy {
    fun resolve(
        packet: SparrowPacket,
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
                    validateContactReadyIdentity(packet, contact)
                    OutgoingTransportRequirement(
                        requiresEncryption = true,
                        allowsEncryptionBeforeMutualIdentity = true,
                        encryptionUnavailableMessage =
                            "Contact ready packet requires an encrypted Sparrow transport"
                    )
                }

                is ContactVerificationReceiptPacket -> {
                    validateVerificationReceiptIdentity(packet, contact)
                    OutgoingTransportRequirement(
                        requiresEncryption = true,
                        encryptionUnavailableMessage =
                            "Contact verification receipt requires an encrypted Sparrow transport"
                    )
                }

                is ChatMessagePacket ->
                    OutgoingTransportRequirement(
                        requiresEncryption = packet.attachments.isNotEmpty(),
                        encryptionUnavailableMessage =
                            "Direct message attachments require an encrypted Sparrow transport"
                    )

                is GroupAvatarUpdatedPacket,
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
                            "Protocol packet requires a mutual Sparrow key exchange"
                    )

                else -> OutgoingTransportRequirement(requiresEncryption = false)
            }
        }

    private fun validateContactReadyIdentity(
        packet: ContactReadyPacket,
        contact: Contact
    ) {
        val identity =
            checkNotNull(contact.sparrowIdentity) {
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
            checkNotNull(contact.sparrowIdentity) {
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
