package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationContext
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationMembershipStatus
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationPair
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationRepository
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GroupVerificationRepositoryImpl(
    private val groupVerificationDao: GroupVerificationDao,
    private val groupMembershipDao: GroupMembershipDao,
    private val groupSecurityDao: GroupSecurityDao
) : GroupVerificationRepository {
    override fun observePairs(groupId: String): Flow<List<GroupVerificationPair>> =
        groupVerificationDao
            .observeByGroupId(groupId)
            .map { rows -> rows.map { row -> row.toGroupVerificationPair() } }

    override fun observeContext(groupId: String): Flow<GroupVerificationContext> =
        combine(
            groupSecurityDao.observeState(groupId),
            groupMembershipDao.observeByGroupId(groupId),
            groupVerificationDao.observeByGroupId(groupId)
        ) { securityState, memberships, rows ->
            val isLocalAdmin =
                securityState?.localRole?.isGroupAdminRole() == true ||
                    (
                        securityState == null &&
                            (
                                rows.any { row -> row.contactId != null } ||
                                    memberships.any { membership ->
                                        membership.perspective == GroupMembershipPerspective.OWNER.name
                                    }
                            )
                    )
            val localMembership =
                if (isLocalAdmin) {
                    null
                } else {
                    memberships.singleOrNull { membership ->
                        membership.perspective == GroupMembershipPerspective.MEMBER.name
                    }
                }

            GroupVerificationContext(
                hasSecurityState = securityState != null,
                isLocalMemberActive =
                    isLocalAdmin ||
                        (securityState != null && securityState.localRole != GROUP_LEFT_ROLE),
                isLocalAdmin = isLocalAdmin,
                ownerContactId =
                    if (isLocalAdmin) {
                        null
                    } else {
                        securityState?.ownerContactId ?: localMembership?.contactId
                    },
                ownInvitationId = localMembership?.sourceInvitationId,
                isLeavePending =
                    localMembership?.status == GroupMembershipStatus.LEAVE_REQUESTED.name
            )
        }
}

private fun GroupVerificationPairEntity.toGroupVerificationPair(): GroupVerificationPair =
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
