package com.cbgm.sparrow.feature.membership.domain.repository

import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.model.MembershipDeclineResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.model.MembershipJoinRequest
import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipSigningProof
import com.cbgm.sparrow.feature.membership.domain.model.StartedMembershipHandshake
import kotlinx.coroutines.flow.Flow

interface MembershipRepository {
    fun observeResults(): Flow<List<MembershipResult>>

    suspend fun startHandshake(
        groupId: String,
        title: String,
        peerId: String
    ): Result<StartedMembershipHandshake>

    suspend fun inspectIncomingOffer(
        peerId: String,
        packet: GroupInvitePacket
    ): Result<IncomingMembershipOffer>

    suspend fun receiveIncomingOffer(
        peerId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long,
        shouldStage: Boolean
    ): Result<Unit>

    suspend fun discardSupersededIncomingHandshakes(
        peerId: String,
        currentSourceId: String
    ): Result<Unit>

    suspend fun getHandshake(sourceId: String): Result<MembershipHandshake?>

    suspend fun acceptHandshake(sourceId: String): Result<Unit>

    suspend fun declineHandshake(sourceId: String): Result<Unit>

    suspend fun receiveOfferReceipt(
        peerId: String,
        packet: GroupInviteReceivedPacket
    ): Result<MembershipSigningProof?>

    suspend fun receiveDecline(
        peerId: String,
        packet: GroupInviteDeclinedPacket
    ): Result<MembershipDeclineResult?>

    suspend fun receiveJoinRequest(
        peerId: String,
        packet: GroupJoinRequestPacket
    ): Result<MembershipJoinRequest?>

    suspend fun confirmJoinIdentity(
        sourceId: String,
        updatedAtEpochMilliseconds: Long,
        context: com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext,
        memberEncryptionPublicKey: ByteArray,
        memberSigningPublicKey: ByteArray,
        memberPhoneNumber: String
    ): Result<Unit>

    suspend fun markRemoved(
        sourceId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun clearHandshake(sourceId: String): Result<Unit>
}
