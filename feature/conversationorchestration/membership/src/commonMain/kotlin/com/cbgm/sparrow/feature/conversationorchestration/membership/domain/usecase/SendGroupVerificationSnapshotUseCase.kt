package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.packet.GroupVerificationPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupVerificationRepository
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox

/** The admin sends its signed verification rows to one current member. */
class SendGroupVerificationSnapshotUseCase(
    private val verificationRepository: GroupVerificationRepository,
    private val loadSigner: LoadGroupVerificationSignerUseCase,
    private val packetCodec: GroupVerificationPacketCodec,
    private val protocolOutbox: ProtocolOutbox
) {
    suspend operator fun invoke(groupId: String, recipientContactId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val signer = loadSigner().getOrThrow()
            val rows = verificationRepository.findRows(groupId).getOrThrow()
            val packet = packetCodec.createSnapshot(groupId, recipientContactId, rows, signer).getOrThrow()
            protocolOutbox.enqueue(contactId = recipientContactId, packet = packet).getOrThrow()
        }
}
