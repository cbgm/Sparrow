package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.packet.GroupVerificationPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.policy.GroupVerificationPacketPolicy
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupVerificationRepository
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.protocol.packet.GroupVerificationReceiptPacket

/** The admin records that a current member verified the admin's current keys. */
class AcceptParticipantVerificationReceiptUseCase(
    private val verificationRepository: GroupVerificationRepository,
    private val verificationInputs: GroupVerificationInputsUseCase,
    private val refreshOwnedGroupVerification: RefreshOwnedGroupVerificationUseCase,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val packetCodec: GroupVerificationPacketCodec
) {
    suspend operator fun invoke(context: IncomingPacketContext, packet: GroupVerificationReceiptPacket): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            GroupVerificationPacketPolicy.requireEncryptedTransport(context)
            GroupVerificationPacketPolicy.requireFreshTimestamp(
                packet.verifiedAtEpochMilliseconds,
                context.receivedAtEpochMilliseconds
            )
            check(packet.packetId == "group-verification-receipt-${packet.receiptId}") {
                "Group verification receipt packet ID is invalid"
            }

            val membership = verificationInputs.membershipContext(packet.groupId).getOrThrow()
            val security = membership.security ?: error("Group security state was not found")
            check(security.isLocalAdmin) { "Only a group admin may receive participant verification receipts" }
            val participantKey = membership.requireCurrentParticipant(context.contactId)
            check(participantKey.encryptionPublicKey.contentEquals(packet.participantEncryptionPublicKey)) {
                "Participant encryption key changed before group verification"
            }
            check(participantKey.signingPublicKey.contentEquals(packet.participantSigningPublicKey)) {
                "Participant signing key changed before group verification"
            }
            val adminIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            check(adminIdentity.encryptionPublicKey.contentEquals(packet.ownerEncryptionPublicKey)) {
                "Group verification receipt targets a different admin encryption key"
            }
            check(adminIdentity.signingPublicKey.contentEquals(packet.ownerSigningPublicKey)) {
                "Group verification receipt targets a different admin signing key"
            }
            packetCodec.verifyReceipt(packet, participantKey.signingPublicKey).getOrThrow()

            refreshOwnedGroupVerification(packet.groupId).getOrThrow()
            val row =
                verificationRepository.findRows(packet.groupId).getOrThrow()
                    .firstOrNull { candidate -> candidate.contactId == context.contactId }
                    ?: error("Participant verification state was not found")
            val updated =
                verificationRepository.markParticipantVerifiedAdmin(
                    groupId = packet.groupId,
                    invitationId = row.invitationId,
                    updatedAtEpochMilliseconds =
                        maxOf(packet.verifiedAtEpochMilliseconds, context.receivedAtEpochMilliseconds)
                ).getOrThrow()
            check(updated) { "Participant is no longer an active verification target" }
        }
}
