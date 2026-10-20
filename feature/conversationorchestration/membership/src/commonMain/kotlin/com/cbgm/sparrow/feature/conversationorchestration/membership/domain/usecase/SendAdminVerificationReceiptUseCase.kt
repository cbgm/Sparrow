package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationStatus
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.packet.GroupVerificationPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupVerificationRepository
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox

/** A member tells the admin that it verified the admin's current keys. */
class SendAdminVerificationReceiptUseCase(
    private val verificationRepository: GroupVerificationRepository,
    private val verificationInputs: GroupVerificationInputsUseCase,
    private val loadSigner: LoadGroupVerificationSignerUseCase,
    private val packetCodec: GroupVerificationPacketCodec,
    private val protocolOutbox: ProtocolOutbox
) {
    suspend operator fun invoke(groupId: String, adminContactId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val context = verificationInputs.membershipContext(groupId).getOrThrow()
            val membership = context.memberships.singleOrNull() ?: error("Local group membership was not found")
            check(membership.isActive) { "The group membership must be active before verification" }
            val row =
                verificationRepository.findRow(groupId, membership.sourceInvitationId).getOrThrow()
                    ?: error("Open the group once to synchronize its verification state")
            check(row.status == GroupVerificationStatus.ACTIVE) {
                "The group membership must be active before verification"
            }
            val adminKey = context.requireCurrentRemoteAdmin(adminContactId)
            val signer = loadSigner().getOrThrow()
            val packet =
                packetCodec.createReceipt(groupId, membership.sourceInvitationId, adminKey, signer).getOrThrow()
            protocolOutbox.enqueue(contactId = adminContactId, packet = packet).getOrThrow()
        }
}
