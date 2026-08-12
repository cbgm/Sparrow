package com.cbgm.securechat.feature.chats.data.repository

import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.data.database.dao.GroupVerificationDao
import com.cbgm.securechat.data.database.entity.GroupVerificationPairEntity
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationDirection
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.security.isGroupAdminRole
import com.cbgm.securechat.feature.chats.domain.model.GroupVerificationContext
import com.cbgm.securechat.feature.chats.domain.model.GroupVerificationMembershipStatus
import com.cbgm.securechat.feature.chats.domain.model.GroupVerificationPair
import com.cbgm.securechat.feature.chats.domain.repository.GroupVerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DefaultGroupVerificationRepository(
    private val groupVerificationDao: GroupVerificationDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao
) : GroupVerificationRepository {
    override fun observePairs(groupId: String): Flow<List<GroupVerificationPair>> =
        groupVerificationDao
            .observeByGroupId(groupId)
            .map { rows -> rows.map(GroupVerificationPairEntity::toDomain) }

    override fun observeContext(groupId: String): Flow<GroupVerificationContext> =
        combine(
            groupSecurityDao.observeState(groupId),
            groupInvitationDao.observeByGroupId(groupId),
            groupVerificationDao.observeByGroupId(groupId)
        ) { securityState, invitations, rows ->
            val isLocalAdmin =
                securityState?.localRole?.isGroupAdminRole() == true ||
                    (
                        securityState == null &&
                            (
                                rows.any { row -> row.contactId != null } ||
                                    invitations.any { invitation ->
                                        invitation.direction == GroupInvitationDirection.OUTGOING.name
                                    }
                            )
                    )
            val localInvitation =
                if (isLocalAdmin) {
                    null
                } else {
                    invitations.singleOrNull { invitation ->
                        invitation.direction == GroupInvitationDirection.INCOMING.name
                    }
                }

            GroupVerificationContext(
                hasSecurityState = securityState != null,
                isLocalAdmin = isLocalAdmin,
                ownerContactId =
                    if (isLocalAdmin) {
                        null
                    } else {
                        securityState?.ownerContactId ?: localInvitation?.contactId
                    },
                ownInvitationId = localInvitation?.invitationId,
                isLeavePending =
                    localInvitation?.status == GroupInvitationStatus.LEAVE_SENT.name
            )
        }
}

private fun GroupVerificationPairEntity.toDomain(): GroupVerificationPair =
    GroupVerificationPair(
        groupId = groupId,
        invitationId = invitationId,
        contactId = contactId,
        displayName = displayName,
        membershipStatus =
            when (membershipStatus) {
                GroupVerificationPairEntity.ACTIVE_STATUS ->
                    GroupVerificationMembershipStatus.ACTIVE
                else -> GroupVerificationMembershipStatus.PENDING
            },
        adminVerifiedParticipant = adminVerifiedParticipant,
        participantVerifiedAdmin = participantVerifiedAdmin,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )
