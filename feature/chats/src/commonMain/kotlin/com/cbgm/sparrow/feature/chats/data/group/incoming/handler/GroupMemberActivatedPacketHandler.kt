package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipEvent
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationCoordinator
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus

class GroupMemberActivatedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupVerificationCoordinator: GroupVerificationCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupMemberActivatedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val activation = packet.requireActivationPacket()
            validateOwnerAndPacket(context, activation)
            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()

            if (
                activation.member.isLocalMember(
                    localEncryptionPublicKey = localIdentity.encryptionPublicKey,
                    localSigningPublicKey = localIdentity.signingPublicKey
                )
            ) {
                activateLocalMembership(context, activation)
                return@runCatching
            }

            activateRemoteMember(context, activation)
        }

    private fun SparrowPacket.requireActivationPacket(): GroupMemberActivatedPacket =
        this as? GroupMemberActivatedPacket
            ?: error("GroupMemberActivatedPacketHandler received an incompatible packet")

    private suspend fun validateOwnerAndPacket(
        context: IncomingPacketContext,
        packet: GroupMemberActivatedPacket
    ) {
        check(context.transportMode == SEALED_BOX_TRANSPORT_MODE) {
            "Group member activation requires encrypted transport"
        }
        val ownerIdentity =
            contactDao.findPublicIdentityByContactId(context.contactId)
                ?: error("Group owner has no Sparrow identity")
        check(ownerIdentity.keyExchangeStatus == MUTUAL_KEY_EXCHANGE_STATUS) {
            "Group owner key exchange is not mutual"
        }
        val securityState =
            groupSecurityDao.findState(packet.groupId)
                ?: error("Group security state was not found")
        check(securityState.currentEpoch == packet.epoch) {
            "Group member activation uses the wrong epoch"
        }

        groupSecurityManager
            .requireRemoteAdmin(
                groupId = packet.groupId,
                contactId = context.contactId,
                signingPublicKey = ownerIdentity.signingPublicKey
            ).getOrThrow()
        membershipPacketProtocol
            .verifyMemberActivated(
                packet = packet,
                expectedOwnerSigningPublicKey = ownerIdentity.signingPublicKey
            ).getOrThrow()
    }

    private suspend fun activateLocalMembership(
        context: IncomingPacketContext,
        packet: GroupMemberActivatedPacket
    ) {
        check(packet.activationRound == GroupMemberActivatedPacket.FINAL_ROUND) {
            "Local group membership requires a final activation"
        }
        activateLocalInvitation(
            groupId = packet.groupId,
            ownerContactId = context.contactId,
            updatedAtEpochMilliseconds =
                maxOf(packet.activatedAtEpochMilliseconds, context.receivedAtEpochMilliseconds)
        )
        groupVerificationCoordinator.synchronize(packet.groupId).getOrThrow()
    }

    private suspend fun activateRemoteMember(
        context: IncomingPacketContext,
        packet: GroupMemberActivatedPacket
    ) {
        val memberContactId = resolveMemberContact(packet.member)
        validatePinnedIdentity(memberContactId, packet.member)
        validateExistingMemberKey(packet, memberContactId)
        storeMemberKey(packet, memberContactId)

        if (packet.activationRound > GroupMemberActivatedPacket.FINAL_ROUND) {
            sendActivationAcknowledgement(context, packet)
        } else {
            storeParticipant(packet, memberContactId)
        }
    }

    private suspend fun validatePinnedIdentity(
        contactId: String,
        member: GroupMemberPayload
    ) {
        val identity = contactDao.findPublicIdentityByContactId(contactId) ?: return
        check(identity.encryptionPublicKey.contentEquals(member.encryptionPublicKey)) {
            "Activated group member conflicts with the pinned contact encryption key"
        }
        check(identity.signingPublicKey.contentEquals(member.signingPublicKey)) {
            "Activated group member conflicts with the pinned contact signing key"
        }
    }

    private suspend fun validateExistingMemberKey(
        packet: GroupMemberActivatedPacket,
        contactId: String
    ) {
        val existing =
            groupSecurityDao.findMemberKey(
                groupId = packet.groupId,
                epoch = packet.epoch,
                contactId = contactId
            ) ?: return

        check(existing.encryptionPublicKey.contentEquals(packet.member.encryptionPublicKey)) {
            "Activated group member encryption key changed"
        }
        check(existing.signingPublicKey.contentEquals(packet.member.signingPublicKey)) {
            "Activated group member signing key changed"
        }
    }

    private suspend fun storeMemberKey(
        packet: GroupMemberActivatedPacket,
        contactId: String
    ) {
        groupSecurityDao.upsertMemberKeys(
            listOf(
                GroupMemberKeyEntity(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    contactId = contactId,
                    encryptionPublicKey = packet.member.encryptionPublicKey.copyOf(),
                    signingPublicKey = packet.member.signingPublicKey.copyOf(),
                    role = packet.member.role
                )
            )
        )
    }

    private suspend fun sendActivationAcknowledgement(
        context: IncomingPacketContext,
        packet: GroupMemberActivatedPacket
    ) {
        val acknowledgement =
            membershipPacketProtocol
                .createMemberActivationAcknowledgement(
                    activationPacket = packet,
                    acknowledgedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                    memberSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                ).getOrThrow()
        protocolOutbox.enqueue(context.contactId, acknowledgement).getOrThrow()
    }

    private suspend fun storeParticipant(
        packet: GroupMemberActivatedPacket,
        contactId: String
    ) {
        chatDao.upsertConversationParticipant(
            ConversationParticipantEntity(
                conversationId = packet.groupId,
                contactId = contactId,
                role = packet.member.role,
                joinedAtEpochMilliseconds = packet.activatedAtEpochMilliseconds
            )
        )
    }

    private suspend fun activateLocalInvitation(
        groupId: String,
        ownerContactId: String,
        updatedAtEpochMilliseconds: Long
    ) {
        val invitation = groupInvitationDao.findByGroupAndContact(groupId, ownerContactId) ?: return
        if (invitation.status == GroupInvitationStatus.ACTIVE.name) return

        check(invitation.status == GroupInvitationStatus.WAITING_FOR_ACTIVATION.name) {
            "Group membership was activated from status ${invitation.status}"
        }
        val updated =
            groupInvitationDao.updateStatus(
                invitationId = invitation.invitationId,
                expectedStatus = GroupInvitationStatus.WAITING_FOR_ACTIVATION.name,
                newStatus =
                    GroupMembershipStateMachine.transition(
                        invitation.status,
                        GroupMembershipEvent.MEMBER_ACTIVATED
                    ).name,
                updatedAt = maxOf(invitation.createdAtEpochMilliseconds, updatedAtEpochMilliseconds)
            )
        check(updated == 1) { "Group invitation changed while membership activation was applied" }
    }

    private suspend fun resolveMemberContact(member: GroupMemberPayload): String {
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

    private fun GroupMemberPayload.requirePhoneNumber(): String =
        phoneNumber?.trim()?.takeIf(String::isNotEmpty)
            ?: error("Group member has no phone number")

    private fun GroupMemberPayload.isLocalMember(
        localEncryptionPublicKey: ByteArray,
        localSigningPublicKey: ByteArray
    ): Boolean =
        encryptionPublicKey.contentEquals(localEncryptionPublicKey) &&
            signingPublicKey.contentEquals(localSigningPublicKey)

    private companion object {
        const val MUTUAL_KEY_EXCHANGE_STATUS = "MUTUAL"
        const val SEALED_BOX_TRANSPORT_MODE = "SEALED_BOX"
    }
}
