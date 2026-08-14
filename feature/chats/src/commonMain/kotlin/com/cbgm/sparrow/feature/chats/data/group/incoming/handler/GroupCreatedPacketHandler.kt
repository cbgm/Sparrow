package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipEvent
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.data.group.security.OpenedGroupWelcome
import com.cbgm.sparrow.feature.chats.data.group.security.isGroupAdminRole
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository

/**
 * Applies a GroupCreatedPacket / group welcome.
 *
 * The handler deliberately reads like a protocol recipe. Each step has one
 * named helper so a new reader can follow validation -> security -> members ->
 * persistence -> acknowledgement without a single giant function.
 */
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
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val protocolOutbox: ProtocolOutbox,
    private val contactKeyExchangeRepository: ContactKeyExchangeRepository
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupCreatedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val groupPacket = packet.requireGroupCreatedPacket()
            if (groupSecurityManager.isLocalMembershipRetired(groupPacket.groupId).getOrThrow()) {
                return@runCatching
            }
            val invitation = findInvitation(groupPacket, context.contactId)
            val isFirstWelcome = groupSecurityDao.findState(groupPacket.groupId) == null

            validateInvitation(groupPacket, invitation, isFirstWelcome)
            val welcome =
                openAndTrustWelcome(
                    packet = groupPacket,
                    senderContactId = context.contactId,
                    isFirstWelcome = isFirstWelcome
                )
            val previousMembership = loadPreviousMembership(groupPacket.groupId)
            val persistedAt = persistedAt(groupPacket, context)

            persistConversation(groupPacket, persistedAt)
            recordMembershipRestartIfNeeded(groupPacket, invitation, isFirstWelcome, persistedAt)
            val membership = resolveMembership(groupPacket, context.contactId, welcome)
            val referenceAdmin = validateAuthorityAndResolveReferenceAdmin(groupPacket, context.contactId, welcome, membership)

            persistGroupSecurity(groupPacket, welcome, membership, referenceAdmin, persistedAt)
            replaceMembership(groupPacket, previousMembership, membership, persistedAt)
            sendReadyAcknowledgement(groupPacket, context.contactId, welcome.openedWelcome)
            advanceInvitation(invitation, isFirstWelcome, persistedAt)
        }

    private fun SparrowPacket.requireGroupCreatedPacket(): GroupCreatedPacket =
        this as? GroupCreatedPacket
            ?: error("GroupCreatedPacketHandler received an incompatible packet")

    private suspend fun findInvitation(
        packet: GroupCreatedPacket,
        senderContactId: String
    ): GroupInvitationEntity? =
        groupInvitationDao.findByGroupAndContact(packet.groupId, senderContactId)

    private fun validateInvitation(
        packet: GroupCreatedPacket,
        invitation: GroupInvitationEntity?,
        isFirstWelcome: Boolean
    ) {
        if (!isFirstWelcome) return

        val acceptedInvitation = invitation ?: error("Accepted group invitation was not found")
        check(acceptedInvitation.status.isAcceptedWelcomeStatus()) {
            "Group welcome arrived before the invitation was accepted"
        }
        check(
            packet.packetId ==
                groupSecurityManager.welcomePacketId(
                    groupId = packet.groupId,
                    invitationId = acceptedInvitation.invitationId,
                    epoch = packet.epoch
                )
        ) {
            "Group welcome does not belong to the current invitation"
        }
    }

    private suspend fun openAndTrustWelcome(
        packet: GroupCreatedPacket,
        senderContactId: String,
        isFirstWelcome: Boolean
    ): WelcomeContext {
        val authorityIdentity =
            resolveAuthorityIdentity(
                groupId = packet.groupId,
                senderContactId = senderContactId,
                isFirstWelcome = isFirstWelcome
            )
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localEncryptionKeyPair = localEncryptionKeyPairProvider.getEncryptionKeyPair().getOrThrow()
        val openedWelcome =
            groupSecurityManager
                .openWelcome(
                    packet = packet,
                    senderContactId = senderContactId,
                    expectedOwnerEncryptionPublicKey = authorityIdentity.encryptionPublicKey,
                    expectedOwnerSigningPublicKey = authorityIdentity.signingPublicKey,
                    localEncryptionKeyPair = localEncryptionKeyPair,
                    localSigningPublicKey = localIdentity.signingPublicKey
                ).getOrThrow()

        if (isFirstWelcome) {
            contactKeyExchangeRepository
                .markMutual(
                    contactId = senderContactId,
                    expectedRemoteEncryptionPublicKey = authorityIdentity.encryptionPublicKey,
                    expectedRemoteSigningPublicKey = authorityIdentity.signingPublicKey
                ).getOrThrow()
        }

        return WelcomeContext(
            authorityIdentity = authorityIdentity,
            localIdentity = localIdentity,
            openedWelcome = openedWelcome
        )
    }

    private suspend fun resolveAuthorityIdentity(
        groupId: String,
        senderContactId: String,
        isFirstWelcome: Boolean
    ): AuthorityIdentity {
        if (isFirstWelcome) {
            val contactIdentity =
                contactDao.findPublicIdentityByContactId(senderContactId)
                    ?: error("Inviting group admin has no accepted Sparrow identity")
            return AuthorityIdentity(
                encryptionPublicKey = contactIdentity.encryptionPublicKey.copyOf(),
                signingPublicKey = contactIdentity.signingPublicKey.copyOf()
            )
        }

        val state =
            groupSecurityDao.findState(groupId)
                ?: error("Group security state was not found")
        val memberKey =
            groupSecurityDao.findMemberKey(
                groupId = groupId,
                epoch = state.currentEpoch,
                contactId = senderContactId
            ) ?: error("Group update sender is not part of the current epoch")
        check(memberKey.role.isGroupAdminRole()) {
            "Group update sender is not an admin"
        }
        return AuthorityIdentity(
            encryptionPublicKey = memberKey.encryptionPublicKey.copyOf(),
            signingPublicKey = memberKey.signingPublicKey.copyOf()
        )
    }

    private suspend fun loadPreviousMembership(groupId: String): PreviousMembership {
        val participants = chatDao.findConversationParticipants(groupId)
        val previousEpoch = groupSecurityDao.findState(groupId)?.currentEpoch
        val signingKeys =
            if (previousEpoch == null) {
                emptyMap()
            } else {
                participants.associate { participant ->
                    participant.contactId to
                        groupSecurityDao
                            .findMemberKey(
                                groupId = groupId,
                                epoch = previousEpoch,
                                contactId = participant.contactId
                            )?.signingPublicKey
                }
            }
        return PreviousMembership(participants, signingKeys)
    }

    private fun persistedAt(
        packet: GroupCreatedPacket,
        context: IncomingPacketContext
    ): Long =
        maxOf(packet.createdAtEpochMilliseconds, context.receivedAtEpochMilliseconds)

    private suspend fun persistConversation(
        packet: GroupCreatedPacket,
        persistedAt: Long
    ) {
        chatDao.upsertConversation(
            ConversationEntity(
                id = packet.groupId,
                contactId = null,
                type = GROUP_CONVERSATION_TYPE,
                title = packet.title,
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = persistedAt
            )
        )
    }

    private suspend fun recordMembershipRestartIfNeeded(
        packet: GroupCreatedPacket,
        invitation: GroupInvitationEntity?,
        isFirstWelcome: Boolean,
        persistedAt: Long
    ) {
        if (!isFirstWelcome) return
        val latestEndAt =
            listOfNotNull(
                chatDao.findMessageTimestampByTransportMode(
                    conversationId = packet.groupId,
                    transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_LEFT_TRANSPORT_MODE
                ),
                chatDao.findMessageTimestampByTransportMode(
                    conversationId = packet.groupId,
                    transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_REMOVED_TRANSPORT_MODE
                )
            ).maxOrNull() ?: return
        val latestStartAt =
            chatDao.findMessageTimestampByTransportMode(
                conversationId = packet.groupId,
                transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_STARTED_TRANSPORT_MODE
            )
        if (latestStartAt != null && latestStartAt > latestEndAt) return

        chatDao.upsertMessage(
            GroupMembershipMessageFactory.localMembershipStarted(
                conversationId = packet.groupId,
                referenceId = invitation?.invitationId ?: packet.packetId,
                epoch = packet.epoch,
                createdAtEpochMilliseconds = persistedAt
            )
        )
    }

    private suspend fun resolveMembership(
        packet: GroupCreatedPacket,
        senderContactId: String,
        welcome: WelcomeContext
    ): ResolvedMembership {
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
            participants += member.toParticipant(packet, contactId)
            memberKeys += member.toMemberKey(packet, contactId)
        }

        return ResolvedMembership(participants, memberKeys)
    }

    private suspend fun normalizedLocalPhoneNumber(): String? =
        localPhoneNumberProvider
            .getLocalPhoneNumber()
            .getOrNull()
            ?.let { phoneNumber -> phoneNumberNormalizer.normalize(phoneNumber).getOrNull() }

    private fun GroupMemberPayload.toParticipant(
        packet: GroupCreatedPacket,
        contactId: String
    ): ConversationParticipantEntity =
        ConversationParticipantEntity(
            conversationId = packet.groupId,
            contactId = contactId,
            role = role,
            joinedAtEpochMilliseconds = packet.createdAtEpochMilliseconds
        )

    private fun GroupMemberPayload.toMemberKey(
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

    private fun validateAuthorityAndResolveReferenceAdmin(
        packet: GroupCreatedPacket,
        senderContactId: String,
        welcome: WelcomeContext,
        membership: ResolvedMembership
    ): ReferenceAdmin {
        val membershipChange = packet.membershipChange
        val authorityLeft =
            membershipChange != null &&
                membershipChange.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT &&
                membershipChange.memberSigningPublicKey.contentEquals(welcome.authorityIdentity.signingPublicKey)

        val senderIsAdmin =
            membership.memberKeys.any { member ->
                member.contactId == senderContactId && member.role.isGroupAdminRole()
            }
        check(authorityLeft || senderIsAdmin) { "Authenticated sender is not a group admin" }

        if (!authorityLeft) {
            return ReferenceAdmin(
                contactId = senderContactId,
                signingPublicKey = welcome.authorityIdentity.signingPublicKey
            )
        }

        val continuingAdmin = membership.memberKeys.firstOrNull { it.role.isGroupAdminRole() }
        return ReferenceAdmin(
            contactId = continuingAdmin?.contactId ?: senderContactId,
            signingPublicKey = continuingAdmin?.signingPublicKey ?: welcome.authorityIdentity.signingPublicKey
        )
    }

    private suspend fun persistGroupSecurity(
        packet: GroupCreatedPacket,
        welcome: WelcomeContext,
        membership: ResolvedMembership,
        referenceAdmin: ReferenceAdmin,
        persistedAt: Long
    ) {
        groupSecurityManager
            .persistJoinedGroup(
                openedWelcome = welcome.openedWelcome,
                ownerContactId = referenceAdmin.contactId,
                authoritySigningPublicKey = referenceAdmin.signingPublicKey,
                localSigningPublicKey = welcome.localIdentity.signingPublicKey,
                memberKeys = membership.memberKeys,
                receivedAtEpochMilliseconds = persistedAt
            ).getOrThrow()
    }

    private suspend fun replaceMembership(
        packet: GroupCreatedPacket,
        previous: PreviousMembership,
        current: ResolvedMembership,
        persistedAt: Long
    ) {
        val currentParticipantIds = current.participants.mapTo(mutableSetOf()) { it.contactId }
        val previousParticipantIds = previous.participants.mapTo(mutableSetOf()) { it.contactId }
        val removedMessages = removedMembershipMessages(packet, previous, currentParticipantIds, persistedAt)
        val addedMessages = addedMembershipMessages(packet, current, previousParticipantIds, persistedAt)

        chatDao.replaceConversationParticipantsWithMessages(
            conversationId = packet.groupId,
            participants = current.participants,
            messages = removedMessages + addedMessages
        )
    }

    private suspend fun removedMembershipMessages(
        packet: GroupCreatedPacket,
        previous: PreviousMembership,
        currentParticipantIds: Set<String>,
        persistedAt: Long
    ) =
        previous.participants
            .filterNot { participant -> participant.contactId in currentParticipantIds }
            .map { participant ->
                if (packet.memberLeft(previous.signingKeysByContactId[participant.contactId])) {
                    GroupMembershipMessageFactory.memberLeft(
                        conversationId = packet.groupId,
                        epoch = packet.epoch,
                        contactId = participant.contactId,
                        contactName = membershipDisplayName(participant.contactId),
                        createdAtEpochMilliseconds = persistedAt
                    )
                } else {
                    GroupMembershipMessageFactory.memberRemoved(
                        conversationId = packet.groupId,
                        epoch = packet.epoch,
                        contactId = participant.contactId,
                        contactName = membershipDisplayName(participant.contactId),
                        createdAtEpochMilliseconds = persistedAt
                    )
                }
            }

    private fun GroupCreatedPacket.memberLeft(previousSigningPublicKey: ByteArray?): Boolean {
        val change = membershipChange ?: return false
        return change.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT &&
            previousSigningPublicKey?.contentEquals(change.memberSigningPublicKey) == true
    }

    private suspend fun addedMembershipMessages(
        packet: GroupCreatedPacket,
        current: ResolvedMembership,
        previousParticipantIds: Set<String>,
        persistedAt: Long
    ) =
        if (previousParticipantIds.isNotEmpty() && chatDao.hasMessages(packet.groupId)) {
            current.participants
                .filterNot { participant -> participant.contactId in previousParticipantIds }
                .map { participant ->
                    GroupMembershipMessageFactory.memberAdded(
                        conversationId = packet.groupId,
                        epoch = packet.epoch,
                        contactId = participant.contactId,
                        contactName = membershipDisplayName(participant.contactId),
                        createdAtEpochMilliseconds = persistedAt
                    )
                }
        } else {
            emptyList()
        }

    private suspend fun sendReadyAcknowledgement(
        packet: GroupCreatedPacket,
        senderContactId: String,
        openedWelcome: OpenedGroupWelcome
    ) {
        val readyAcknowledgement =
            membershipPacketProtocol
                .createReadyAcknowledgement(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    welcomePacketId = packet.packetId,
                    keyConfirmation =
                        groupSecurityManager.createKeyConfirmation(
                            groupId = packet.groupId,
                            epoch = packet.epoch,
                            groupKey = openedWelcome.groupKey
                        ),
                    memberSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                ).getOrThrow()
        protocolOutbox.enqueue(senderContactId, readyAcknowledgement).getOrThrow()
    }

    private suspend fun advanceInvitation(
        invitation: GroupInvitationEntity?,
        isFirstWelcome: Boolean,
        persistedAt: Long
    ) {
        val acceptedInvitation = invitation ?: return
        when (acceptedInvitation.status) {
            GroupInvitationStatus.JOIN_SENT.name -> markWaitingForActivation(acceptedInvitation, persistedAt)
            GroupInvitationStatus.WAITING_FOR_ACTIVATION.name,
            GroupInvitationStatus.ACTIVE.name,
            GroupInvitationStatus.LEAVE_SENT.name -> Unit
            else -> if (isFirstWelcome) error("Group welcome arrived before the invitation was accepted")
        }
    }

    private suspend fun markWaitingForActivation(
        invitation: GroupInvitationEntity,
        persistedAt: Long
    ) {
        val updated =
            groupInvitationDao.updateStatus(
                invitationId = invitation.invitationId,
                expectedStatus = GroupInvitationStatus.JOIN_SENT.name,
                newStatus =
                    GroupMembershipStateMachine.transition(
                        invitation.status,
                        GroupMembershipEvent.WELCOME_RECEIVED
                    ).name,
                updatedAt = maxOf(invitation.createdAtEpochMilliseconds, persistedAt)
            )
        check(updated == 1) { "Group invitation changed while the welcome was applied" }
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

    private fun String.isAcceptedWelcomeStatus(): Boolean =
        this == GroupInvitationStatus.JOIN_SENT.name ||
            this == GroupInvitationStatus.WAITING_FOR_ACTIVATION.name ||
            this == GroupInvitationStatus.ACTIVE.name ||
            this == GroupInvitationStatus.LEAVE_SENT.name

    private data class WelcomeContext(
        val authorityIdentity: AuthorityIdentity,
        val localIdentity: LocalPublicIdentity,
        val openedWelcome: OpenedGroupWelcome
    )

    private data class AuthorityIdentity(
        val encryptionPublicKey: ByteArray,
        val signingPublicKey: ByteArray
    )

    private data class PreviousMembership(
        val participants: List<ConversationParticipantEntity>,
        val signingKeysByContactId: Map<String, ByteArray?>
    )

    private data class ResolvedMembership(
        val participants: List<ConversationParticipantEntity>,
        val memberKeys: List<GroupMemberKeyEntity>
    )

    private data class ReferenceAdmin(
        val contactId: String,
        val signingPublicKey: ByteArray
    )

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
