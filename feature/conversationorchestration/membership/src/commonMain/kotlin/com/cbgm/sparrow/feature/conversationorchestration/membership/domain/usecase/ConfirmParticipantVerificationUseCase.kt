package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupVerificationRepository

/** The admin marks a current member as verified; the member's keys must not have changed meanwhile. */
class ConfirmParticipantVerificationUseCase(
    private val verificationRepository: GroupVerificationRepository,
    private val verificationInputs: GroupVerificationInputsUseCase,
    private val refreshOwnedGroupVerification: RefreshOwnedGroupVerificationUseCase
) {
    suspend operator fun invoke(groupId: String, participantContactId: String): Result<Unit> =
        runCatching {
            refreshOwnedGroupVerification(groupId).getOrThrow()
            val memberKey =
                verificationInputs.membershipContext(groupId).requireCurrentParticipant(participantContactId)
            val row =
                verificationRepository.findRows(groupId).getOrThrow()
                    .firstOrNull { candidate -> candidate.contactId == participantContactId }
                    ?: error("Participant verification state was not found")
            check(row.hasIdentity(memberKey.encryptionPublicKey, memberKey.signingPublicKey)) {
                "Participant identity changed before group verification"
            }
            val updated =
                verificationRepository
                    .markAdminVerifiedParticipant(groupId, row.invitationId, SystemClock.nowEpochMilliseconds())
                    .getOrThrow()
            check(updated) { "Participant is no longer an active verification target" }
        }
}
