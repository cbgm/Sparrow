package com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation

import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.provider.InvitationPeerMetadata
import com.cbgm.sparrow.feature.invite.domain.provider.InvitationPeerMetadataProvider

internal class GroupInvitationPeerMetadataProvider(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) : InvitationPeerMetadataProvider {
    override val payloadType: InvitationPayloadType = InvitationPayloadType.GROUP

    override suspend fun get(
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Result<InvitationPeerMetadata> =
        safeSuspendCall {
            val contact = contactDao.findById(peerId)
            val preferredPhoneNumberId = contact?.contact?.preferredPhoneNumberId
            val phoneNumber =
                contact?.phoneNumbers
                    ?.firstOrNull { number -> number.id == preferredPhoneNumberId }
                    ?.value
                    ?: contact?.phoneNumbers?.firstOrNull()?.value
            val contactName =
                contact?.contact?.displayName
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?.takeUnless { name ->
                        phoneNumber != null &&
                            phoneNumberNormalizer.normalize(name).getOrNull() ==
                            phoneNumberNormalizer.normalize(phoneNumber).getOrNull()
                    }
            val groupTitle =
                chatDao.findConversationById(payloadId)
                    ?.title
                    ?.trim()
                    ?.takeIf(String::isNotBlank)

            when (direction) {
                InvitationDirection.INCOMING ->
                    InvitationPeerMetadata(
                        displayName = groupTitle ?: contactName,
                        secondaryText = contactName ?: phoneNumber
                    )

                InvitationDirection.OUTGOING ->
                    InvitationPeerMetadata(
                        displayName = contactName,
                        secondaryText = groupTitle
                    )
            }
        }
}
