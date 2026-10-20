package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.packet.GroupVerificationPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.policy.GroupVerificationPacketPolicy
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotRequestPacket

/** The admin authenticates a current member's request for the verification rows. */
class AcceptVerificationSnapshotRequestUseCase(
    private val verificationInputs: GroupVerificationInputsUseCase,
    private val packetCodec: GroupVerificationPacketCodec
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: GroupVerificationSnapshotRequestPacket
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            GroupVerificationPacketPolicy.requireEncryptedTransport(context)
            GroupVerificationPacketPolicy.requireFreshTimestamp(
                packet.requestedAtEpochMilliseconds,
                context.receivedAtEpochMilliseconds
            )
            check(packet.packetId == "group-verification-snapshot-request-${packet.requestId}") {
                "Group verification snapshot request packet ID is invalid"
            }

            val membership = verificationInputs.membershipContext(packet.groupId).getOrThrow()
            val security = membership.security ?: error("Group security state was not found")
            check(security.isLocalAdmin) { "Only a group admin may answer verification snapshot requests" }
            val participantKey = membership.requireCurrentParticipant(context.contactId)
            check(participantKey.signingPublicKey.contentEquals(packet.requesterSigningPublicKey)) {
                "Snapshot requester signing key changed"
            }
            packetCodec.verifySnapshotRequest(packet, participantKey.signingPublicKey).getOrThrow()
        }
}
