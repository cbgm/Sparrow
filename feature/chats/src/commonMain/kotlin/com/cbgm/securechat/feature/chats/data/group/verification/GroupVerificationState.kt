package com.cbgm.securechat.feature.chats.data.group.verification

import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.data.database.dao.GroupVerificationDao
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.data.database.entity.GroupVerificationPairEntity
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationDirection
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.group.security.isGroupAdminRole
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact

internal class GroupVerificationState(
    private val groupVerificationDao: GroupVerificationDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val getContact: GetContact
) {
    suspend fun ownsGroup(groupId: String): Boolean =
        groupVerificationDao
            .findByGroupId(groupId)
            .any { row -> row.contactId != null } ||
            groupInvitationDao
                .findByGroupId(groupId)
                .any { invitation ->
                    invitation.direction == GroupInvitationDirection.OUTGOING.name
                }

    suspend fun requireCurrentParticipant(
        groupId: String,
        contactId: String
    ): GroupMemberKeyEntity {
        val state =
            groupSecurityDao.findState(groupId)
                ?: error("Group security state was not found")
        return groupSecurityDao.findMemberKey(
            groupId = groupId,
            epoch = state.currentEpoch,
            contactId = contactId
        ) ?: error("Group participant is not part of the current epoch")
    }

    suspend fun requireCurrentRemoteAdmin(
        groupId: String,
        contactId: String
    ): GroupMemberKeyEntity {
        val memberKey = requireCurrentParticipant(groupId, contactId)
        check(memberKey.role.isGroupAdminRole()) { "Group participant is not an admin" }
        return memberKey
    }

    suspend fun refreshOwnedState(groupId: String) {
        val existingRows = groupVerificationDao.findByGroupId(groupId)
        val existingByContactId =
            existingRows.mapNotNull { row -> row.contactId?.let { it to row } }.toMap()
        val invitations = groupInvitationDao.findByGroupId(groupId)
        val invitationByContactId = invitations.associateBy { invitation -> invitation.contactId }
        val securityState = groupSecurityDao.findState(groupId)
        val currentMemberKeys =
            securityState
                ?.let { state ->
                    groupSecurityDao.findMemberKeys(
                        groupId = groupId,
                        epoch = state.currentEpoch
                    )
                }.orEmpty()
        val now = SystemClock.nowEpochMilliseconds()

        val activeRows =
            currentMemberKeys.map { memberKey ->
                val contact = requireContact(memberKey.contactId)
                val invitation = invitationByContactId[memberKey.contactId]
                val previous = existingByContactId[memberKey.contactId]
                val sameIdentity = previous.matches(memberKey)
                GroupVerificationPairEntity(
                    groupId = groupId,
                    invitationId =
                        invitation?.invitationId
                            ?: previous?.invitationId
                            ?: "member-${memberKey.contactId}",
                    contactId = memberKey.contactId,
                    displayName = contact.verificationDisplayName(),
                    membershipStatus = GroupVerificationPairEntity.ACTIVE_STATUS,
                    participantEncryptionPublicKey = memberKey.encryptionPublicKey.copyOf(),
                    participantSigningPublicKey = memberKey.signingPublicKey.copyOf(),
                    adminVerifiedParticipant = sameIdentity && previous?.adminVerifiedParticipant == true,
                    participantVerifiedAdmin = sameIdentity && previous?.participantVerifiedAdmin == true,
                    updatedAtEpochMilliseconds =
                        maxOf(invitation?.updatedAtEpochMilliseconds ?: 0L, now)
                )
            }

        val activeContactIds =
            currentMemberKeys.mapTo(mutableSetOf()) { memberKey -> memberKey.contactId }
        val pendingRows =
            invitations
                .filter { invitation ->
                    invitation.direction == GroupInvitationDirection.OUTGOING.name &&
                        !invitation.status.isTerminalStatus() &&
                        invitation.contactId !in activeContactIds
                }.map { invitation ->
                    val contact = requireContact(invitation.contactId)
                    val identity = contact.secureChatIdentity
                    val previous = existingByContactId[invitation.contactId]
                    GroupVerificationPairEntity(
                        groupId = groupId,
                        invitationId = invitation.invitationId,
                        contactId = invitation.contactId,
                        displayName = contact.verificationDisplayName(),
                        membershipStatus = GroupVerificationPairEntity.PENDING_STATUS,
                        participantEncryptionPublicKey = identity?.encryptionPublicKey?.copyOf(),
                        participantSigningPublicKey = identity?.signingPublicKey?.copyOf(),
                        adminVerifiedParticipant = false,
                        participantVerifiedAdmin = false,
                        updatedAtEpochMilliseconds =
                            maxOf(
                                invitation.updatedAtEpochMilliseconds,
                                previous?.updatedAtEpochMilliseconds ?: 0L
                            )
                    )
                }

        groupVerificationDao.replaceGroup(groupId = groupId, rows = activeRows + pendingRows)
    }

    suspend fun requireContact(contactId: String): Contact =
        getContact(contactId).getOrThrow()
            ?: error("Contact not found: $contactId")

    private fun GroupVerificationPairEntity?.matches(
        memberKey: GroupMemberKeyEntity
    ): Boolean {
        val previous = this ?: return false
        val encryptionPublicKey = previous.participantEncryptionPublicKey ?: return false
        val signingPublicKey = previous.participantSigningPublicKey ?: return false
        return encryptionPublicKey.contentEquals(memberKey.encryptionPublicKey) &&
            signingPublicKey.contentEquals(memberKey.signingPublicKey)
    }

    private fun GroupVerificationPairEntity?.matches(
        identity: SecureChatIdentity?
    ): Boolean {
        val previous = this ?: return false
        val encryptionPublicKey = previous.participantEncryptionPublicKey ?: return false
        val signingPublicKey = previous.participantSigningPublicKey ?: return false
        val currentIdentity = identity ?: return false
        return encryptionPublicKey.contentEquals(currentIdentity.encryptionPublicKey) &&
            signingPublicKey.contentEquals(currentIdentity.signingPublicKey)
    }

    private fun Contact.verificationDisplayName(): String =
        displayName?.trim()?.takeIf(String::isNotBlank) ?: "Unknown member"

    private fun String.isTerminalStatus(): Boolean =
        this == GroupInvitationStatus.DECLINED.name ||
            this == GroupInvitationStatus.EXPIRED.name ||
            this == GroupInvitationStatus.FAILED.name ||
            this == GroupInvitationStatus.REMOVED.name ||
            this == GroupInvitationStatus.GROUP_DELETED.name
}
