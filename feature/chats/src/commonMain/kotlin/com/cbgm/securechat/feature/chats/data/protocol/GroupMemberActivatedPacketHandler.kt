package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.security.GroupInvitationManager
import com.cbgm.securechat.feature.chats.data.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.data.verification.GroupVerificationCoordinator
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus

class GroupMemberActivatedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val groupInvitationManager: GroupInvitationManager,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupVerificationCoordinator: GroupVerificationCoordinator
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupMemberActivatedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val activationPacket =
                packet as? GroupMemberActivatedPacket
                    ?: error("GroupMemberActivatedPacketHandler received an incompatible packet")
            check(context.transportMode == SEALED_BOX_TRANSPORT_MODE) {
                "Group member activation requires encrypted transport"
            }
            val ownerIdentity =
                contactDao.findPublicIdentityByContactId(context.contactId)
                    ?: error("Group owner has no SecureChat identity")
            check(ownerIdentity.keyExchangeStatus == MUTUAL_KEY_EXCHANGE_STATUS) {
                "Group owner key exchange is not mutual"
            }
            val securityState =
                groupSecurityDao.findState(activationPacket.groupId)
                    ?: error("Group security state was not found")
            check(securityState.currentEpoch == activationPacket.epoch) {
                "Group member activation uses the wrong epoch"
            }
            groupSecurityManager
                .requireRemoteAdmin(
                    groupId = activationPacket.groupId,
                    contactId = context.contactId,
                    signingPublicKey = ownerIdentity.signingPublicKey
                ).getOrThrow()

            groupInvitationManager
                .verifyMemberActivated(
                    packet = activationPacket,
                    expectedOwnerSigningPublicKey = ownerIdentity.signingPublicKey
                ).getOrThrow()

            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            if (
                activationPacket.member.isLocalMember(
                    localEncryptionPublicKey = localIdentity.encryptionPublicKey,
                    localSigningPublicKey = localIdentity.signingPublicKey
                )
            ) {
                check(activationPacket.activationRound == GroupMemberActivatedPacket.FINAL_ROUND) {
                    "Local group membership requires a final activation"
                }
                activateLocalInvitation(
                    groupId = activationPacket.groupId,
                    ownerContactId = context.contactId,
                    updatedAtEpochMilliseconds =
                        maxOf(
                            activationPacket.activatedAtEpochMilliseconds,
                            context.receivedAtEpochMilliseconds
                        )
                )
                groupVerificationCoordinator
                    .synchronize(activationPacket.groupId)
                    .getOrThrow()
                return@runCatching
            }

            val memberContactId = resolveMemberContact(activationPacket.member)
            contactDao.findPublicIdentityByContactId(memberContactId)?.let { pinnedIdentity ->
                check(pinnedIdentity.encryptionPublicKey.contentEquals(activationPacket.member.encryptionPublicKey)) {
                    "Activated group member conflicts with the pinned contact encryption key"
                }
                check(pinnedIdentity.signingPublicKey.contentEquals(activationPacket.member.signingPublicKey)) {
                    "Activated group member conflicts with the pinned contact signing key"
                }
            }

            val existingMemberKey =
                groupSecurityDao.findMemberKey(
                    groupId = activationPacket.groupId,
                    epoch = activationPacket.epoch,
                    contactId = memberContactId
                )
            if (existingMemberKey != null) {
                check(
                    existingMemberKey.encryptionPublicKey.contentEquals(activationPacket.member.encryptionPublicKey)
                ) {
                    "Activated group member encryption key changed"
                }
                check(existingMemberKey.signingPublicKey.contentEquals(activationPacket.member.signingPublicKey)) {
                    "Activated group member signing key changed"
                }
            }

            groupSecurityDao.upsertMemberKeys(
                listOf(
                    GroupMemberKeyEntity(
                        groupId = activationPacket.groupId,
                        epoch = activationPacket.epoch,
                        contactId = memberContactId,
                        encryptionPublicKey = activationPacket.member.encryptionPublicKey.copyOf(),
                        signingPublicKey = activationPacket.member.signingPublicKey.copyOf(),
                        role = activationPacket.member.role
                    )
                )
            )

            if (activationPacket.activationRound > GroupMemberActivatedPacket.FINAL_ROUND) {
                val acknowledgement =
                    groupInvitationManager
                        .createMemberActivationAcknowledgement(
                            activationPacket = activationPacket,
                            acknowledgedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                            memberSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                        ).getOrThrow()
                protocolOutbox.enqueue(context.contactId, acknowledgement).getOrThrow()
                return@runCatching
            }

            chatDao.upsertConversationParticipant(
                ConversationParticipantEntity(
                    conversationId = activationPacket.groupId,
                    contactId = memberContactId,
                    role = activationPacket.member.role,
                    joinedAtEpochMilliseconds = activationPacket.activatedAtEpochMilliseconds
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
                newStatus = GroupInvitationStatus.ACTIVE.name,
                updatedAt = maxOf(invitation.createdAtEpochMilliseconds, updatedAtEpochMilliseconds)
            )
        check(updated == 1) { "Group invitation changed while membership activation was applied" }
    }

    private suspend fun resolveMemberContact(member: GroupMemberPayload): String {
        val phoneNumber = member.requirePhoneNumber()
        val normalizedPhoneNumber = phoneNumberNormalizer.normalize(phoneNumber).getOrThrow()
        val existingByPhoneNumber = contactDao.findByNormalizedPhoneNumber(normalizedPhoneNumber)
        if (existingByPhoneNumber != null) {
            return existingByPhoneNumber.contact.id
        }

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
