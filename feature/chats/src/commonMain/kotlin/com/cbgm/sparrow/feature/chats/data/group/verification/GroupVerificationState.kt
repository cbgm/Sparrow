package com.cbgm.sparrow.feature.chats.data.group.verification

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole

internal class GroupVerificationState(
    private val groupVerificationDao: GroupVerificationDao,
    private val groupMembershipDao: GroupMembershipDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val getContact: GetContactUseCase
) {
    suspend fun ownsGroup(groupId: String): Boolean =
        groupVerificationDao
            .findByGroupId(groupId)
            .any { row -> row.contactId != null } ||
            groupMembershipDao
                .findByGroupId(groupId)
                .any { membership ->
                    membership.perspective == GroupMembershipPerspective.OWNER.name
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
        val memberships = groupMembershipDao.findByGroupId(groupId)
        val membershipByContactId = memberships.associateBy { membership -> membership.contactId }
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
                val membership = membershipByContactId[memberKey.contactId]
                val previous = existingByContactId[memberKey.contactId]
                val sameIdentity = previous.matches(memberKey)
                GroupVerificationPairEntity(
                    groupId = groupId,
                    invitationId =
                        membership?.sourceInvitationId
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
                        maxOf(membership?.updatedAtEpochMilliseconds ?: 0L, now)
                )
            }

        val activeContactIds =
            currentMemberKeys.mapTo(mutableSetOf()) { memberKey -> memberKey.contactId }
        val pendingRows =
            memberships
                .filter { membership ->
                    membership.perspective == GroupMembershipPerspective.OWNER.name &&
                        membership.status.isVisiblePendingStatus() &&
                        membership.contactId !in activeContactIds
                }.map { membership ->
                    val contact = requireContact(membership.contactId)
                    val identity = contact.sparrowIdentity
                    val previous = existingByContactId[membership.contactId]
                    GroupVerificationPairEntity(
                        groupId = groupId,
                        invitationId = membership.sourceInvitationId,
                        contactId = membership.contactId,
                        displayName = contact.verificationDisplayName(),
                        membershipStatus = GroupVerificationPairEntity.PENDING_STATUS,
                        participantEncryptionPublicKey = identity?.encryptionPublicKey?.copyOf(),
                        participantSigningPublicKey = identity?.signingPublicKey?.copyOf(),
                        adminVerifiedParticipant = false,
                        participantVerifiedAdmin = false,
                        updatedAtEpochMilliseconds =
                            maxOf(
                                membership.updatedAtEpochMilliseconds,
                                previous?.updatedAtEpochMilliseconds ?: 0L
                            )
                    )
                }

        val authoritativeInvitationIds =
            (activeRows + pendingRows).mapTo(mutableSetOf()) { row -> row.invitationId }
        val remotePendingRows =
            existingRows.filter { row ->
                row.contactId == null &&
                    row.membershipStatus == GroupVerificationPairEntity.PENDING_STATUS &&
                    row.invitationId !in authoritativeInvitationIds
            }

        groupVerificationDao.replaceGroup(
            groupId = groupId,
            rows = activeRows + pendingRows + remotePendingRows
        )
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

    private fun Contact.verificationDisplayName(): String =
        displayName?.trim()?.takeIf(String::isNotBlank) ?: "Unknown member"

    private fun String.isVisiblePendingStatus(): Boolean =
        this == GroupMembershipStatus.STAGED.name ||
            this == GroupMembershipStatus.IDENTITY_READY.name ||
            this == GroupMembershipStatus.WELCOME_SENT.name
}
