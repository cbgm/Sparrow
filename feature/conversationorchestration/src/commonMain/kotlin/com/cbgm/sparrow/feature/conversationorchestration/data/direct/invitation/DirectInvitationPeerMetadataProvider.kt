package com.cbgm.sparrow.feature.conversationorchestration.data.direct.invitation

import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.provider.InvitationPeerMetadata
import com.cbgm.sparrow.feature.invite.domain.provider.InvitationPeerMetadataProvider

internal class DirectInvitationPeerMetadataProvider(
    private val contactDao: ContactDao,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) : InvitationPeerMetadataProvider {
    override val payloadType: InvitationPayloadType = InvitationPayloadType.DIRECT

    override suspend fun get(
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Result<InvitationPeerMetadata> =
        safeSuspendCall {
            require(peerId.isNotBlank()) { "Invitation peer ID must not be blank" }
            val contact = contactDao.findById(peerId) ?: return@safeSuspendCall InvitationPeerMetadata()
            val phoneNumber =
                contact.phoneNumbers
                    .firstOrNull { number -> number.id == contact.contact.preferredPhoneNumberId }
                    ?.value
                    ?: contact.phoneNumbers.firstOrNull()?.value
            val displayName =
                contact.contact.displayName
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?.takeUnless { name ->
                        phoneNumber != null &&
                            phoneNumberNormalizer.normalize(name).getOrNull() ==
                            phoneNumberNormalizer.normalize(phoneNumber).getOrNull()
                    }

            InvitationPeerMetadata(
                displayName = displayName,
                secondaryText = phoneNumber
            )
        }
}
