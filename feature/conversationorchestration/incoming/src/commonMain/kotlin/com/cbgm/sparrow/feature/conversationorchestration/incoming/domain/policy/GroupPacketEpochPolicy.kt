package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy

import com.cbgm.sparrow.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.protocol.packet.GroupDescriptionUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.GroupTitleUpdatedPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket

/**
 * Which group epoch must be installed before a group packet can be handled. Live and mailbox copies overtake
 * each other, so a message of epoch 3 can arrive before the welcome that installs epoch 3, or before the first
 * welcome of a group the user just joined. Such packets wait instead of failing.
 *
 * Welcomes install epochs themselves. Deletions and removals are handled without an installed epoch (a
 * removal even names the next one), so they never wait.
 */
object GroupPacketEpochPolicy {
    private const val ANY_INSTALLED_EPOCH = 1

    /** The epoch the packet needs, or null when it is handled whatever is installed. */
    @Suppress("CyclomaticComplexMethod")
    fun requiredEpoch(packet: SparrowPacket): Int? =
        when (packet) {
            is GroupChatMessagePacket -> packet.epoch
            is GroupTitleUpdatedPacket -> packet.epoch
            is GroupDescriptionUpdatedPacket -> packet.epoch
            is GroupAvatarUpdatedPacket -> packet.epoch
            is GroupPinUpdatedPacket -> packet.epoch
            is GroupMemberActivatedPacket -> packet.epoch
            is GroupMemberActivationAcknowledgementPacket -> packet.epoch
            is GroupReadyAcknowledgementPacket -> packet.epoch
            is GroupLeaveRequestPacket -> packet.epoch
            is GroupVerificationReceiptPacket,
            is GroupVerificationSnapshotRequestPacket,
            is GroupVerificationSnapshotPacket -> ANY_INSTALLED_EPOCH
            else -> null
        }

    /** Older epochs are installed too: packets of an epoch that was replaced are not deferred. */
    fun isInstalled(currentEpoch: Int?, requiredEpoch: Int): Boolean =
        currentEpoch != null && currentEpoch >= requiredEpoch
}
