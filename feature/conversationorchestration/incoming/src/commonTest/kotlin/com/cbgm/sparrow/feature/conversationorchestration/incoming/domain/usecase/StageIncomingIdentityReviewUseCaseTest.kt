package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.StagePendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.model.MembershipDeclineResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.model.MembershipJoinRequest
import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipSigningProof
import com.cbgm.sparrow.feature.membership.domain.model.MembershipStatus
import com.cbgm.sparrow.feature.membership.domain.model.StartedMembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase
import com.cbgm.sparrow.protocol.handler.IncomingMessageRejectedException
import com.cbgm.sparrow.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.protocol.packet.GroupJoinRequestPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StageIncomingIdentityReviewUseCaseTest {
    private val staged = FakePendingChanges()

    @Test
    fun changedKeysOfAJoinAreStagedForReview() = runBlocking {
        useCase(blocked = emptySet()).forGroupJoin(join(), receivedAtEpochMilliseconds = CREATED_AT + 1).getOrThrow()
        assertEquals(listOf(MEMBER), staged.changes.map { it.peerId })
    }

    @Test
    fun blockedMemberGetsNoReview() = runBlocking {
        useCase(blocked = setOf(MEMBER)).forGroupJoin(join(), receivedAtEpochMilliseconds = CREATED_AT + 1).getOrThrow()
        assertTrue(staged.changes.isEmpty())
    }

    @Test
    fun joinThatArrivedAfterTheInvitationExpiredIsRejected() = runBlocking {
        val arrivedLate = CREATED_AT + MembershipHandshake.VALIDITY_MILLISECONDS
        val error = useCase(blocked = emptySet()).forGroupJoin(join(), arrivedLate).exceptionOrNull()
        assertIs<IncomingMessageRejectedException>(error)
        assertTrue(staged.changes.isEmpty())
    }

    private fun useCase(blocked: Set<String>) =
        StageIncomingIdentityReviewUseCase(
            blocklistRepository = FakeBlocklist(blocked),
            getMembershipHandshake = GetMembershipHandshakeUseCase(FakeMemberships(handshake())),
            stagePendingRemoteIdentityChange = StagePendingRemoteIdentityChangeUseCase(staged)
        )

    private fun join() =
        MembershipJoinRequest(
            sourceId = SOURCE_ID,
            groupId = GROUP_ID,
            peerId = MEMBER,
            memberEncryptionPublicKey = ByteArray(32) { 1 },
            memberSigningPublicKey = ByteArray(32) { 2 },
            alreadyAccepted = false
        )

    private fun handshake() =
        MembershipHandshake(
            sourceId = SOURCE_ID,
            groupId = GROUP_ID,
            peerId = MEMBER,
            createdAtEpochMilliseconds = CREATED_AT,
            updatedAtEpochMilliseconds = CREATED_AT,
            status = MembershipStatus.STAGED
        )

    private class FakeBlocklist(
        private val blocked: Set<String>
    ) : ContactBlocklistRepository {
        override suspend fun isBlocked(contactId: String) = Result.success(contactId in blocked)

        override fun observeBlockUnknownContactInvites(): Flow<Boolean> = error(NOT_USED)

        override suspend fun getBlockUnknownContactInvites(): Result<Boolean> = error(NOT_USED)

        override suspend fun setBlockUnknownContactInvites(enabled: Boolean): Result<Unit> = error(NOT_USED)

        override fun observeBlockedContactIds(): Flow<Set<String>> = error(NOT_USED)

        override suspend fun getBlockedContactIds(): Result<Set<String>> = error(NOT_USED)

        override suspend fun block(contactId: String): Result<Unit> = error(NOT_USED)

        override suspend fun unblock(contactId: String): Result<Unit> = error(NOT_USED)
    }

    private class FakePendingChanges : PendingRemoteIdentityChangeRepository {
        val changes = mutableListOf<PendingRemoteIdentityChange>()

        override suspend fun stage(candidate: PendingRemoteIdentityChange): Result<Unit> {
            changes += candidate
            return Result.success(Unit)
        }

        override fun observeAll(): Flow<List<PendingRemoteIdentityChange>> = error(NOT_USED)

        override suspend fun discard(peerId: String, invitationId: String): Result<Unit> = error(NOT_USED)

        override suspend fun approveReplacement(
            peerId: String,
            invitationId: String,
            presentedSigningFingerprint: String,
            presentedEncryptionFingerprint: String
        ): Result<Unit> = error(NOT_USED)
    }

    private class FakeMemberships(
        private val handshake: MembershipHandshake
    ) : MembershipRepository {
        override suspend fun getHandshake(sourceId: String) = Result.success(handshake.takeIf { it.sourceId == sourceId })

        override fun observeResults(): Flow<List<MembershipResult>> = error(NOT_USED)

        override suspend fun startHandshake(
            groupId: String,
            title: String,
            peerId: String
        ): Result<StartedMembershipHandshake> = error(NOT_USED)

        override suspend fun inspectIncomingOffer(
            peerId: String,
            packet: GroupInvitePacket
        ): Result<IncomingMembershipOffer> = error(NOT_USED)

        override suspend fun receiveIncomingOffer(
            peerId: String,
            packet: GroupInvitePacket,
            receivedAtEpochMilliseconds: Long,
            shouldStage: Boolean
        ): Result<Unit> = error(NOT_USED)

        override suspend fun discardSupersededIncomingHandshakes(
            peerId: String,
            currentSourceId: String
        ): Result<Unit> = error(NOT_USED)

        override suspend fun acceptHandshake(sourceId: String): Result<Unit> = error(NOT_USED)

        override suspend fun declineHandshake(sourceId: String): Result<Unit> = error(NOT_USED)

        override suspend fun receiveOfferReceipt(
            peerId: String,
            packet: GroupInviteReceivedPacket
        ): Result<MembershipSigningProof?> = error(NOT_USED)

        override suspend fun receiveDecline(
            peerId: String,
            packet: GroupInviteDeclinedPacket
        ): Result<MembershipDeclineResult?> = error(NOT_USED)

        override suspend fun receiveJoinRequest(
            peerId: String,
            packet: GroupJoinRequestPacket
        ): Result<MembershipJoinRequest?> = error(NOT_USED)

        override suspend fun confirmJoinIdentity(
            sourceId: String,
            updatedAtEpochMilliseconds: Long,
            context: GroupMembershipContext,
            memberEncryptionPublicKey: ByteArray,
            memberSigningPublicKey: ByteArray,
            memberPhoneNumber: String
        ): Result<Unit> = error(NOT_USED)

        override suspend fun markRemoved(sourceId: String, updatedAtEpochMilliseconds: Long): Result<Unit> =
            error(NOT_USED)

        override suspend fun clearHandshake(sourceId: String): Result<Unit> = error(NOT_USED)
    }

    private companion object {
        const val NOT_USED = "Not used"
        const val SOURCE_ID = "group-membership-1"
        const val GROUP_ID = "group"
        const val MEMBER = "member"
        const val CREATED_AT = 1_000L
    }
}
