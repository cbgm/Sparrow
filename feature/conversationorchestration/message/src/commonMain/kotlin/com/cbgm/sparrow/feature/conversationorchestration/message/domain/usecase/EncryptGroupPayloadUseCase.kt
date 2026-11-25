package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairSource
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.protocol.profile.LocalProfilePictureMetadataSource
import com.cbgm.sparrow.protocol.profile.ProfilePictureMetadata

/**
 * Encrypts one group plaintext once with the current epoch and addresses a copy to every recipient.
 * Message and operation packets differ only in their IDs, which the caller provides.
 */
class EncryptGroupPayloadUseCase(
    private val groupSecurityRepository: GroupSecurityRepository,
    private val localSigningKeyPairSource: LocalSigningKeyPairSource,
    private val localProfilePictureMetadataSource: LocalProfilePictureMetadataSource
) {
    suspend operator fun invoke(
        groupId: String,
        messageId: String,
        sentAtEpochMilliseconds: Long,
        plaintext: String,
        recipients: List<String>,
        packetIdFor: (contactId: String) -> String
    ): Result<Map<String, GroupChatMessagePacket>> =
        safeSuspendCall(logFailure = false) {
            val localSigningKeyPair = localSigningKeyPairSource.getSigningKeyPair().getOrThrow()
            val profilePicture = localProfilePictureMetadataSource.forMessage().getOrElse { ProfilePictureMetadata() }
            val secured =
                groupSecurityRepository.encryptMessage(
                    groupId = groupId,
                    messageId = messageId,
                    sentAtEpochMilliseconds = sentAtEpochMilliseconds,
                    plaintext = plaintext,
                    localSigningKeyPair = localSigningKeyPair,
                    profilePicture = profilePicture
                ).getOrThrow()

            recipients.associateWith { contactId ->
                GroupChatMessagePacket(
                    packetId = packetIdFor(contactId),
                    groupId = groupId,
                    epoch = secured.epoch,
                    messageId = messageId,
                    sentAtEpochMilliseconds = sentAtEpochMilliseconds,
                    profilePicture = profilePicture,
                    nonce = secured.nonce.copyOf(),
                    ciphertext = secured.ciphertext.copyOf(),
                    senderSignature = secured.senderSignature.copyOf()
                )
            }
        }
}
