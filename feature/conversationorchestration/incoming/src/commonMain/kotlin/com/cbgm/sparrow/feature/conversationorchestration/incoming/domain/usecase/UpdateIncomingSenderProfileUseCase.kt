package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase.ApplyPeerProfilePictureUseCase
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket

/**
 * Applies the sender profile a new direct message carries: the profile picture (best effort), and the claimed
 * phone number as display name for a contact that has none.
 */
class UpdateIncomingSenderProfileUseCase(
    private val applyPeerProfilePicture: ApplyPeerProfilePictureUseCase,
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(
        contactId: String,
        packet: ChatMessagePacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            applyPeerProfilePicture(contactId, packet.profilePicture)
            packet.senderPhoneNumber?.trim()?.takeIf(String::isNotBlank)?.let { phoneNumber ->
                contactRepository
                    .usePhoneNumberAsDisplayNameWhenMissing(contactId, phoneNumber, receivedAtEpochMilliseconds)
                    .getOrThrow()
            }
        }
}
