package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.packet.SparrowPacket

/**
 * Writes one packet per recipient to the outbox. A failed recipient does not stop the others;
 * its message recipient is marked failed and the combined failure is reported afterwards.
 */
class EnqueueGroupPacketsUseCase(
    private val protocolOutbox: ProtocolOutbox,
    private val recordPacketSendFailure: RecordPacketSendFailureUseCase
) {
    private val logger = SparrowLog.withTag("EnqueueGroupPacketsUseCase")

    suspend operator fun invoke(
        packetsByContactId: Map<String, SparrowPacket>,
        recordFailedRecipients: Boolean
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val failures = mutableListOf<String>()
            packetsByContactId.forEach { (contactId, packet) ->
                val error = protocolOutbox.enqueue(contactId, packet).exceptionOrNull() ?: return@forEach
                logger.error(error) { "Could not queue group packet for $contactId" }
                if (recordFailedRecipients) {
                    recordPacketSendFailure(packet.packetId, error.message)
                        .onFailure { stateError ->
                            logger.error(stateError) {
                                "Could not persist failed group recipient state: packetId=${packet.packetId}"
                            }
                        }
                }
                failures += "$contactId: ${error.message ?: error::class.simpleName.orEmpty()}"
            }
            check(failures.isEmpty()) { "Could not queue group packets for ${failures.joinToString()}" }
        }
}
