package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.packet.GroupVerificationPacketCodec
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox

/** An active member asks the admin for the current verification rows. Inactive members stay silent. */
class RequestGroupVerificationSnapshotUseCase(
    private val verificationInputs: GroupVerificationInputsUseCase,
    private val loadSigner: LoadGroupVerificationSignerUseCase,
    private val packetCodec: GroupVerificationPacketCodec,
    private val protocolOutbox: ProtocolOutbox
) {
    suspend operator fun invoke(groupId: String, adminContactId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val membership =
                verificationInputs.membershipContext(groupId).getOrThrow()
                    .memberships.singleOrNull() ?: return@safeSuspendCall
            if (!membership.isActive) return@safeSuspendCall
            val signer = loadSigner().getOrThrow()
            val packet = packetCodec.createSnapshotRequest(groupId, membership.sourceInvitationId, signer).getOrThrow()
            protocolOutbox.enqueue(contactId = adminContactId, packet = packet).getOrThrow()
        }
}
