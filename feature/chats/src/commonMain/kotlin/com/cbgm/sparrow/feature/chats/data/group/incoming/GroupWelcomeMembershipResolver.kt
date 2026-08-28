package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus

internal class GroupWelcomeMembershipResolver(
    private val contactDao: ContactDao,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) {
    suspend fun resolve(
        packet: GroupCreatedPacket,
        senderContactId: String,
        welcome: GroupWelcomeContextDto
    ): ResolvedGroupMembershipDto {
        val localPhoneNumber = normalizedLocalPhoneNumber()
        val participants = mutableListOf<ConversationParticipantEntity>()
        val memberKeys = mutableListOf<GroupMemberKeyEntity>()

        packet.members.forEach { member ->
            if (member.isLocalMember(welcome.localIdentity.signingPublicKey, localPhoneNumber)) {
                return@forEach
            }

            val contactId =
                resolveMemberContact(
                    member = member,
                    senderContactId = senderContactId,
                    senderSigningPublicKey = welcome.authorityIdentity.signingPublicKey
                )
            participants += member.toConversationParticipantEntity(packet, contactId)
            memberKeys += member.toGroupMemberKeyEntity(packet, contactId)
        }

        return ResolvedGroupMembershipDto(participants, memberKeys)
    }

    private suspend fun normalizedLocalPhoneNumber(): String? =
        localPhoneNumberProvider
            .getLocalPhoneNumber()
            .getOrNull()
            ?.let { phoneNumber -> phoneNumberNormalizer.normalize(phoneNumber).getOrNull() }

    private suspend fun resolveMemberContact(
        member: GroupMemberPayload,
        senderContactId: String,
        senderSigningPublicKey: ByteArray
    ): String {
        if (member.signingPublicKey.contentEquals(senderSigningPublicKey)) {
            member.phoneNumber
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { phoneNumber -> updateContactPhoneNumber(senderContactId, phoneNumber) }
            return senderContactId
        }

        val phoneNumber = member.requirePhoneNumber()
        val normalizedPhoneNumber = phoneNumberNormalizer.normalize(phoneNumber).getOrThrow()
        val existing = contactDao.findByNormalizedPhoneNumber(normalizedPhoneNumber)
        return existing?.contact?.id ?: createContact(phoneNumber, normalizedPhoneNumber)
    }

    private suspend fun createContact(
        phoneNumber: String,
        normalizedPhoneNumber: String
    ): String {
        val now = SystemClock.nowEpochMilliseconds()
        val contactId = IdGenerator.generate()
        val phoneNumberId = IdGenerator.generate()

        contactDao.upsertContact(
            ContactEntity(
                id = contactId,
                displayName = phoneNumber,
                deviceContactId = null,
                deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED.name,
                preferredPhoneNumberId = phoneNumberId,
                createdAtEpochMilliseconds = now,
                updatedAtEpochMilliseconds = now
            )
        )
        contactDao.upsertPhoneNumbers(
            listOf(
                ContactPhoneNumberEntity(
                    id = phoneNumberId,
                    contactId = contactId,
                    value = phoneNumber,
                    normalizedValue = normalizedPhoneNumber,
                    type = ContactPhoneNumberType.MOBILE.name,
                    label = null,
                    updatedAtEpochMilliseconds = now
                )
            )
        )
        return contactId
    }

    private suspend fun updateContactPhoneNumber(
        contactId: String,
        phoneNumber: String
    ) {
        val normalizedPhoneNumber = phoneNumberNormalizer.normalize(phoneNumber).getOrThrow()
        val existing = contactDao.findById(contactId) ?: return
        val now = SystemClock.nowEpochMilliseconds()
        val phoneNumberId = existing.contact.preferredPhoneNumberId ?: IdGenerator.generate()

        contactDao.upsertContact(
            existing.contact.copy(
                preferredPhoneNumberId = phoneNumberId,
                updatedAtEpochMilliseconds = now
            )
        )
        contactDao.usePhoneNumberAsDisplayNameWhenMissing(
            contactId = contactId,
            phoneNumber = phoneNumber,
            updatedAtEpochMilliseconds = now
        )
        contactDao.upsertPhoneNumbers(
            listOf(
                ContactPhoneNumberEntity(
                    id = phoneNumberId,
                    contactId = contactId,
                    value = phoneNumber,
                    normalizedValue = normalizedPhoneNumber,
                    type = ContactPhoneNumberType.MOBILE.name,
                    label = null,
                    updatedAtEpochMilliseconds = now
                )
            )
        )
    }

    private fun GroupMemberPayload.toConversationParticipantEntity(
        packet: GroupCreatedPacket,
        contactId: String
    ): ConversationParticipantEntity =
        ConversationParticipantEntity(
            conversationId = packet.groupId,
            contactId = contactId,
            role = role,
            joinedAtEpochMilliseconds = packet.createdAtEpochMilliseconds
        )

    private fun GroupMemberPayload.toGroupMemberKeyEntity(
        packet: GroupCreatedPacket,
        contactId: String
    ): GroupMemberKeyEntity =
        GroupMemberKeyEntity(
            groupId = packet.groupId,
            epoch = packet.epoch,
            contactId = contactId,
            encryptionPublicKey = encryptionPublicKey.copyOf(),
            signingPublicKey = signingPublicKey.copyOf(),
            role = role
        )

    private fun GroupMemberPayload.requirePhoneNumber(): String =
        phoneNumber
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: error("Group member has no phone number")

    private fun GroupMemberPayload.isLocalMember(
        localSigningPublicKey: ByteArray?,
        normalizedLocalPhoneNumber: String?
    ): Boolean {
        if (
            localSigningPublicKey != null &&
            signingPublicKey.isNotEmpty() &&
            signingPublicKey.contentEquals(localSigningPublicKey)
        ) {
            return true
        }
        val localPhoneNumber = normalizedLocalPhoneNumber ?: return false
        val memberPhoneNumber =
            phoneNumber
                ?.let { value -> phoneNumberNormalizer.normalize(value).getOrNull() }
                ?: return false
        return memberPhoneNumber == localPhoneNumber
    }
}
