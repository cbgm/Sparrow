package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupConversationHistoryDataSource
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationMembershipStatus
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationPair
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class GroupVerificationRepositoryImpl(
    private val historyDataSource: GroupConversationHistoryDataSource
) : GroupVerificationRepository {
    override fun observePairs(groupId: String): Flow<List<GroupVerificationPair>> =
        historyDataSource.observeVerificationRows(groupId).map { rows ->
            rows.map { row -> row.toGroupVerificationPair() }
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
                GroupVerificationPairEntity.ACTIVE_STATUS -> GroupVerificationMembershipStatus.ACTIVE
                else -> GroupVerificationMembershipStatus.PENDING
            },
        adminVerifiedParticipant = adminVerifiedParticipant,
        participantVerifiedAdmin = participantVerifiedAdmin,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )
