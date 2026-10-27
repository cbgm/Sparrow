package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.rejectIfNull
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.rejectUnless
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.usecase.StagePendingRemoteIdentityChangeUseCase
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.model.MembershipJoinRequest
import com.cbgm.sparrow.feature.membership.domain.usecase.GetMembershipHandshakeUseCase

/**
 * Stages keys that differ from a known contact's for the user's review. A verified packet proves only the
 * proposed keys, so they never replace the trusted ones here.
 */
class StageIncomingIdentityReviewUseCase(
    private val blocklistRepository: ContactBlocklistRepository,
    private val getMembershipHandshake: GetMembershipHandshakeUseCase,
    private val stagePendingRemoteIdentityChange: StagePendingRemoteIdentityChangeUseCase
) {
    /**
     * A contact invitation or acceptance: a blocked peer gets no review. With [requireSenderPeer] the review must
     * concern the sender itself; this is only checked for peers that are not blocked.
     */
    suspend fun forContact(change: PendingRemoteIdentityChange, requireSenderPeer: Boolean = false): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            if (blocklistRepository.isBlocked(change.peerId).getOrThrow()) return@safeSuspendCall
            if (requireSenderPeer) {
                check(change.peerId == change.sourcePeerId) { "Identity review does not match the invitation contact" }
            }
            stagePendingRemoteIdentityChange(change).getOrThrow()
        }

    /** Changed owner keys of a group offer. Blocked senders were dropped before the offer was staged. */
    suspend fun forGroupOffer(
        offer: IncomingMembershipOffer,
        ownerContactId: String,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        stagePendingRemoteIdentityChange(
            PendingRemoteIdentityChange(
                peerId = ownerContactId,
                sourcePeerId = ownerContactId,
                invitationId = offer.sourceId,
                proposedEncryptionPublicKey = offer.ownerEncryptionPublicKey.copyOf(),
                proposedSigningPublicKey = offer.ownerSigningPublicKey.copyOf(),
                receivedAtEpochMilliseconds = receivedAtEpochMilliseconds,
                expiresAtEpochMilliseconds = offer.expiresAtEpochMilliseconds
            )
        )

    /**
     * Changed member keys of a verified JOIN. Like a contact invitation, a blocked peer gets no review. The JOIN is
     * the evidence for the review, so it must belong to our own pending invitation, and that invitation must still
     * have been valid when the JOIN arrived.
     */
    suspend fun forGroupJoin(join: MembershipJoinRequest, receivedAtEpochMilliseconds: Long): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            if (blocklistRepository.isBlocked(join.peerId).getOrThrow()) return@safeSuspendCall
            val handshake =
                rejectIfNull(getMembershipHandshake(join.sourceId).getOrThrow()) {
                    "Verified JOIN has no pending group membership"
                }
            rejectUnless(handshake.isOwnerSideInvitationOf(join.groupId, join.peerId)) {
                "Verified JOIN does not belong to the owner-side group invitation"
            }
            val expiresAt = handshake.expiresAtEpochMilliseconds
            rejectUnless(expiresAt > receivedAtEpochMilliseconds) {
                "Group invitation expired before identity change could be approved"
            }
            stagePendingRemoteIdentityChange(
                PendingRemoteIdentityChange(
                    peerId = join.peerId,
                    sourcePeerId = join.peerId,
                    invitationId = join.sourceId,
                    proposedEncryptionPublicKey = join.memberEncryptionPublicKey.copyOf(),
                    proposedSigningPublicKey = join.memberSigningPublicKey.copyOf(),
                    receivedAtEpochMilliseconds = receivedAtEpochMilliseconds,
                    expiresAtEpochMilliseconds = expiresAt
                )
            ).getOrThrow()
        }
}
