package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DeferredPacketWait
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupPacketEpochPolicy
import com.cbgm.sparrow.feature.membership.domain.usecase.FindGroupCurrentEpochUseCase
import com.cbgm.sparrow.protocol.packet.SparrowPacket

/**
 * What a group packet still waits for: null when it can be handled now, otherwise the epoch whose welcome has
 * not been installed yet ([GroupPacketEpochPolicy]).
 */
class CheckGroupPacketReadyUseCase(
    private val findGroupCurrentEpoch: FindGroupCurrentEpochUseCase
) {
    suspend operator fun invoke(groupId: String, packet: SparrowPacket): Result<DeferredPacketWait.GroupEpoch?> =
        safeSuspendCall(logFailure = false) {
            val requiredEpoch = GroupPacketEpochPolicy.requiredEpoch(packet) ?: return@safeSuspendCall null
            val currentEpoch = findGroupCurrentEpoch(groupId).getOrThrow()
            if (GroupPacketEpochPolicy.isInstalled(currentEpoch, requiredEpoch)) {
                null
            } else {
                DeferredPacketWait.GroupEpoch(groupId, requiredEpoch)
            }
        }
}
