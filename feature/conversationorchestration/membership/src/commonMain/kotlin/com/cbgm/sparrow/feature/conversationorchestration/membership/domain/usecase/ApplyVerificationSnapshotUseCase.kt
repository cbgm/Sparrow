package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationStatus
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.packet.GroupVerificationPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.policy.GroupVerificationPacketPolicy
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupVerificationRepository
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotPacket

/**
 * Stores the admin's newest snapshot. A device that is admin itself keeps its own authoritative rows and
 * only adopts pending invitations it does not know yet.
 */
class ApplyVerificationSnapshotUseCase(
    private val verificationRepository: GroupVerificationRepository,
    private val verificationInputs: GroupVerificationInputsUseCase,
    private val packetCodec: GroupVerificationPacketCodec
) {
    suspend operator fun invoke(context: IncomingPacketContext, packet: GroupVerificationSnapshotPacket): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            GroupVerificationPacketPolicy.requireEncryptedTransport(context)
            GroupVerificationPacketPolicy.requireFreshTimestamp(
                packet.generatedAtEpochMilliseconds,
                context.receivedAtEpochMilliseconds
            )
            check(packet.packetId.startsWith("group-verification-snapshot-${packet.snapshotId}-")) {
                "Group verification snapshot packet ID is invalid"
            }

            val membership = verificationInputs.membershipContext(packet.groupId).getOrThrow()
            val security = membership.security ?: error("Group security state was not found")
            val adminKey = membership.requireCurrentRemoteAdmin(context.contactId)
            check(adminKey.encryptionPublicKey.contentEquals(packet.ownerEncryptionPublicKey)) {
                "Group verification snapshot admin encryption key changed"
            }
            check(adminKey.signingPublicKey.contentEquals(packet.ownerSigningPublicKey)) {
                "Group verification snapshot admin signing key changed"
            }
            packetCodec.verifySnapshot(packet, adminKey.signingPublicKey).getOrThrow()

            val latest = verificationRepository.findLatestUpdatedAt(packet.groupId).getOrThrow()
            if (latest != null && packet.generatedAtEpochMilliseconds < latest) {
                return@safeSuspendCall
            }

            val snapshotRows = packetCodec.readSnapshotRows(packet)
            val rows =
                if (security.isLocalAdmin) {
                    val localRows =
                        verificationRepository.findRows(packet.groupId).getOrThrow().filter { it.contactId != null }
                    val localInvitationIds = localRows.mapTo(mutableSetOf()) { it.invitationId }
                    localRows + snapshotRows.filter { row ->
                        row.status == GroupVerificationStatus.PENDING && row.invitationId !in localInvitationIds
                    }
                } else {
                    snapshotRows
                }
            verificationRepository.replaceGroup(packet.groupId, rows).getOrThrow()
        }
}
