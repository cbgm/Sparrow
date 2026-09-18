package com.cbgm.sparrow.feature.invite.data.protocol

import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket

interface GroupInvitationPacketProcessor {
    suspend fun receiveInvite(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun receiveInviteReceived(
        memberContactId: String,
        packet: GroupInviteReceivedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun receiveDeclined(
        memberContactId: String,
        packet: GroupInviteDeclinedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun receiveJoinRequest(
        memberContactId: String,
        packet: GroupJoinRequestPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit>
}
