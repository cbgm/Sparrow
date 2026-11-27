package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.usecase.InspectIncomingGroupMembershipUseCase
import com.cbgm.sparrow.protocol.packet.GroupInvitePacket

/**
 * The verified group offer, or null when it is dropped: offers of blocked senders and offers that expired before
 * they arrived never enter the inbox or the retry queue.
 */
class InspectIncomingGroupOfferUseCase(
    private val inspectIncomingGroupMembership: InspectIncomingGroupMembershipUseCase,
    private val blocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long
    ): Result<IncomingMembershipOffer?> =
        safeSuspendCall(logFailure = false) {
            val offer = inspectIncomingGroupMembership(ownerContactId, packet).getOrThrow()
            offer.takeUnless {
                blocklistRepository.isBlocked(ownerContactId).getOrThrow() ||
                    offer.expiresAtEpochMilliseconds <= receivedAtEpochMilliseconds
            }
        }
}
