package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.WasGroupMembershipDeletedUseCase
import com.cbgm.sparrow.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket

/**
 * Packets of a group the user deleted locally are dropped. A deletion by the owner is still accepted for a
 * group whose membership is gone, so its cleanup can finish.
 */
class CheckGroupPacketAcceptedUseCase(
    private val messageRepository: GroupMessageRepository,
    private val wasGroupMembershipDeleted: WasGroupMembershipDeletedUseCase
) {
    suspend operator fun invoke(groupId: String, packet: SparrowPacket): Result<Boolean> =
        safeSuspendCall(logFailure = false) {
            when {
                messageRepository.hasMessageWithTransportMode(
                    groupId,
                    MessageTransportMode.LOCAL_CONVERSATION_DELETED
                ).getOrThrow() ->
                    false
                packet is GroupConversationDeletedPacket -> true
                else -> !wasGroupMembershipDeleted(groupId).getOrThrow()
            }
        }
}
