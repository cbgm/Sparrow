package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.message.GroupMembershipMessageFactory
import com.cbgm.securechat.feature.chats.data.security.GroupInvitationManager
import com.cbgm.securechat.feature.chats.data.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.data.security.isGroupAdminRole
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore

class GroupCreatedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localEncryptionKeyPairProvider: LocalEncryptionKeyPairProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupSecurityDao: GroupSecurityDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupInvitationManager: GroupInvitationManager,
    private val protocolOutbox: ProtocolOutbox,
    private val contactKeyExchangeStore: ContactKeyExchangeStore
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupCreatedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val groupPacket =
                packet as? GroupCreatedPacket
                    ?: error("GroupCreatedPacketHandler received an incompatible packet")
            val existingSecurityState = groupSecurityDao.findState(groupPacket.groupId)
            val invitation =
                groupInvitationDao.findByGroupAndContact(groupPacket.groupId, context.contactId)
            if (existingSecurityState == null) {
                val acceptedInvitation = invitation ?: error("Accepted group invitation was not found")
                check(
                    acceptedInvitation.status == GroupInvitationStatus.JOIN_SENT.name ||
                        acceptedInvitation.status == GroupInvitationStatus.WAITING_FOR_ACTIVATION.name ||
                        acceptedInvitation.status == GroupInvitationStatus.ACTIVE.name ||
                        acceptedInvitation.status == GroupInvitationStatus.LEAVE_SENT.name
                ) {
                    "Group welcome arrived before the invitation was accepted"
                }
                check(
                    groupPacket.packetId ==
                        groupSecurityManager.welcomePacketId(
                            groupId = groupPacket.groupId,
                            invitationId = acceptedInvitation.invitationId,
                            epoch = groupPacket.epoch
                        )
                ) {
                    "Group welcome does not belong to the current invitation"
                }
            }
            val persistedAtEpochMilliseconds =
                maxOf(
                    groupPacket.createdAtEpochMilliseconds,
                    context.receivedAtEpochMilliseconds
                )
            val authorityIdentity =
                contactDao.findPublicIdentityByContactId(context.contactId)
                    ?: error("Group admin has no SecureChat identity")
            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val localEncryptionKeyPair =
                localEncryptionKeyPairProvider.getEncryptionKeyPair().getOrThrow()
            val openedWelcome =
                groupSecurityManager
                    .openWelcome(
                        packet = groupPacket,
                        senderContactId = context.contactId,
                        expectedOwnerEncryptionPublicKey = authorityIdentity.encryptionPublicKey,
                        expectedOwnerSigningPublicKey = authorityIdentity.signingPublicKey,
                        localEncryptionKeyPair = localEncryptionKeyPair,
                        localSigningPublicKey = localIdentity.signingPublicKey
                    ).getOrThrow()

            contactKeyExchangeStore
                .markMutual(
                    contactId = context.contactId,
                    expectedRemoteEncryptionPublicKey = authorityIdentity.encryptionPublicKey,
                    expectedRemoteSigningPublicKey = authorityIdentity.signingPublicKey
                ).getOrThrow()

            val previousParticipants =
                chatDao.findConversationParticipants(groupPacket.groupId)
            val previousEpoch = groupSecurityDao.findState(groupPacket.groupId)?.currentEpoch
            val previousSigningKeysByContactId =
                if (previousEpoch == null) {
                    emptyMap()
                } else {
                    previousParticipants.associate { participant ->
                        participant.contactId to
                            groupSecurityDao
                                .findMemberKey(
                                    groupId = groupPacket.groupId,
                                    epoch = previousEpoch,
                                    contactId = participant.contactId
                                )?.signingPublicKey
                    }
                }

            chatDao.upsertConversation(
                ConversationEntity(
                    id = groupPacket.groupId,
                    contactId = null,
                    type = GROUP_CONVERSATION_TYPE,
                    title = groupPacket.title,
                    createdAtEpochMilliseconds = groupPacket.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = persistedAtEpochMilliseconds
                )
            )

            val normalizedLocalPhoneNumber =
                localPhoneNumberProvider
                    .getLocalPhoneNumber()
                    .getOrNull()
                    ?.let { phoneNumber -> phoneNumberNormalizer.normalize(phoneNumber).getOrNull() }

            val memberKeys = mutableListOf<GroupMemberKeyEntity>()
            val participants = mutableListOf<ConversationParticipantEntity>()

            groupPacket.members.forEach { member ->
                if (member.isLocalMember(localIdentity.signingPublicKey, normalizedLocalPhoneNumber)) {
                    return@forEach
                }

                val contactId =
                    resolveMemberContact(
                        member = member,
                        senderContactId = context.contactId,
                        senderSigningPublicKey = authorityIdentity.signingPublicKey
                    )

                participants +=
                    ConversationParticipantEntity(
                        conversationId = groupPacket.groupId,
                        contactId = contactId,
                        role = member.role,
                        joinedAtEpochMilliseconds = groupPacket.createdAtEpochMilliseconds
                    )
                memberKeys +=
                    GroupMemberKeyEntity(
                        groupId = groupPacket.groupId,
                        epoch = groupPacket.epoch,
                        contactId = contactId,
                        encryptionPublicKey = member.encryptionPublicKey.copyOf(),
                        signingPublicKey = member.signingPublicKey.copyOf(),
                        role = member.role
                    )
            }

            val authorityLeft =
                groupPacket.membershipChange?.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT &&
                    groupPacket.membershipChange?.memberSigningPublicKey.contentEquals(authorityIdentity.signingPublicKey)
            check(
                authorityLeft ||
                    memberKeys.any { member ->
                        member.contactId == context.contactId &&
                            member.role.isGroupAdminRole()
                    }
            ) {
                "Authenticated sender is not a group admin"
            }

            val continuingRemoteAdmin =
                memberKeys.firstOrNull { member -> member.role.isGroupAdminRole() }
            val referenceAdminContactId =
                if (authorityLeft) {
                    continuingRemoteAdmin?.contactId ?: context.contactId
                } else {
                    context.contactId
                }
            val referenceAdminSigningPublicKey =
                if (authorityLeft) {
                    continuingRemoteAdmin?.signingPublicKey ?: authorityIdentity.signingPublicKey
                } else {
                    authorityIdentity.signingPublicKey
                }

            groupSecurityManager
                .persistJoinedGroup(
                    openedWelcome = openedWelcome,
                    ownerContactId = referenceAdminContactId,
                    authoritySigningPublicKey = referenceAdminSigningPublicKey,
                    localSigningPublicKey = localIdentity.signingPublicKey,
                    memberKeys = memberKeys,
                    receivedAtEpochMilliseconds = persistedAtEpochMilliseconds
                ).getOrThrow()

            val currentParticipantIds =
                participants.mapTo(mutableSetOf(), ConversationParticipantEntity::contactId)
            val previousParticipantIds =
                previousParticipants.mapTo(mutableSetOf(), ConversationParticipantEntity::contactId)
            val removedMembershipMessages =
                previousParticipants
                    .filterNot { participant -> participant.contactId in currentParticipantIds }
                    .map { removedParticipant ->
                        val memberLeft =
                            groupPacket.membershipChange
                                ?.takeIf { change ->
                                    change.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT &&
                                        previousSigningKeysByContactId[removedParticipant.contactId]
                                            ?.contentEquals(change.memberSigningPublicKey) == true
                                } != null
                        if (memberLeft) {
                            GroupMembershipMessageFactory.memberLeft(
                                conversationId = groupPacket.groupId,
                                epoch = groupPacket.epoch,
                                contactId = removedParticipant.contactId,
                                contactName = membershipDisplayName(removedParticipant.contactId),
                                createdAtEpochMilliseconds = persistedAtEpochMilliseconds
                            )
                        } else {
                            GroupMembershipMessageFactory.memberRemoved(
                                conversationId = groupPacket.groupId,
                                epoch = groupPacket.epoch,
                                contactId = removedParticipant.contactId,
                                contactName = membershipDisplayName(removedParticipant.contactId),
                                createdAtEpochMilliseconds = persistedAtEpochMilliseconds
                            )
                        }
                    }
            val addedMembershipMessages =
                if (previousParticipants.isNotEmpty() && chatDao.hasMessages(groupPacket.groupId)) {
                    participants
                        .filterNot { participant -> participant.contactId in previousParticipantIds }
                        .map { addedParticipant ->
                            GroupMembershipMessageFactory.memberAdded(
                                conversationId = groupPacket.groupId,
                                epoch = groupPacket.epoch,
                                contactId = addedParticipant.contactId,
                                contactName = membershipDisplayName(addedParticipant.contactId),
                                createdAtEpochMilliseconds = persistedAtEpochMilliseconds
                            )
                        }
                } else {
                    emptyList()
                }
            chatDao.replaceConversationParticipantsWithMessages(
                conversationId = groupPacket.groupId,
                participants = participants,
                messages = removedMembershipMessages + addedMembershipMessages
            )

            val readyAcknowledgement =
                groupInvitationManager
                    .createReadyAcknowledgement(
                        groupId = groupPacket.groupId,
                        epoch = groupPacket.epoch,
                        welcomePacketId = groupPacket.packetId,
                        keyConfirmation =
                            groupSecurityManager.createKeyConfirmation(
                                groupId = groupPacket.groupId,
                                epoch = groupPacket.epoch,
                                groupKey = openedWelcome.groupKey
                            ),
                        memberSigningKeyPair =
                            localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                    ).getOrThrow()
            protocolOutbox.enqueue(context.contactId, readyAcknowledgement).getOrThrow()

            invitation?.let { acceptedInvitation ->
                when (acceptedInvitation.status) {
                    GroupInvitationStatus.JOIN_SENT.name -> {
                        val updated =
                            groupInvitationDao.updateStatus(
                                invitationId = acceptedInvitation.invitationId,
                                expectedStatus = GroupInvitationStatus.JOIN_SENT.name,
                                newStatus = GroupInvitationStatus.WAITING_FOR_ACTIVATION.name,
                                updatedAt =
                                    maxOf(
                                        acceptedInvitation.createdAtEpochMilliseconds,
                                        persistedAtEpochMilliseconds
                                    )
                            )
                        check(updated == 1) {
                            "Group invitation changed while the welcome was applied"
                        }
                    }

                    GroupInvitationStatus.WAITING_FOR_ACTIVATION.name,
                    GroupInvitationStatus.ACTIVE.name,
                    GroupInvitationStatus.LEAVE_SENT.name -> Unit
                    else -> {
                        if (existingSecurityState == null) {
                            error("Group welcome arrived before the invitation was accepted")
                        }
                    }
                }
            }
        }

    private suspend fun membershipDisplayName(contactId: String): String =
        contactDao
            .findById(contactId)
            ?.contact
            ?.displayName
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: "Member"

    private suspend fun resolveMemberContact(
        member: GroupMemberPayload,
        senderContactId: String,
        senderSigningPublicKey: ByteArray
    ): String {
        if (member.signingPublicKey.contentEquals(senderSigningPublicKey)) {
            member.phoneNumber
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { updateContactPhoneNumber(senderContactId, it) }

            return senderContactId
        }

        val phoneNumber = member.requirePhoneNumber()
        val normalizedPhoneNumber = phoneNumberNormalizer.normalize(phoneNumber).getOrThrow()
        val existingByPhoneNumber = contactDao.findByNormalizedPhoneNumber(normalizedPhoneNumber)

        if (existingByPhoneNumber != null) {
            return existingByPhoneNumber.contact.id
        }

        return createContact(phoneNumber, normalizedPhoneNumber)
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

    private fun GroupMemberPayload.requirePhoneNumber(): String =
        phoneNumber
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
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

        if (normalizedLocalPhoneNumber == null) {
            return false
        }

        val normalizedMemberPhoneNumber =
            phoneNumber
                ?.let { phoneNumberNormalizer.normalize(it).getOrNull() }
                ?: return false

        return normalizedMemberPhoneNumber == normalizedLocalPhoneNumber
    }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
