package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.protocol.invitation.ContactInvitationRequestProtocol
import com.cbgm.sparrow.protocol.packet.ContactInvitePacket

/**
 * Verifies the signature and time bounds of a contact invitation before any contact is looked up, and returns
 * the phone number the sender claims, or null. The sender's display name carries that number; a name that is
 * no phone number claims none.
 */
class VerifyIncomingContactInvitationUseCase(
    private val invitationRequestProtocol: ContactInvitationRequestProtocol,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) {
    suspend operator fun invoke(packet: ContactInvitePacket, receivedAtEpochMilliseconds: Long): Result<String?> =
        safeSuspendCall(logFailure = false) {
            invitationRequestProtocol.verifyPacket(packet, receivedAtEpochMilliseconds).getOrThrow()
            packet.claimedDisplayName()?.let { value -> phoneNumberNormalizer.normalize(value).getOrNull() }
        }
}

/** The display name the sender put into the invitation, if it is not blank. */
internal fun ContactInvitePacket.claimedDisplayName(): String? = displayName?.trim()?.takeIf(String::isNotBlank)
