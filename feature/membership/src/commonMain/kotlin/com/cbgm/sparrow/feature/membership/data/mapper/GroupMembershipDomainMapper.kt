package com.cbgm.sparrow.feature.membership.data.mapper

import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.model.GroupLeaveRequirementDto
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.model.MembershipPerspective
import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipStatus
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective as DataMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus as DataMembershipStatus

internal fun GroupLeaveRequirementDto.toDomain(): GroupLeaveRequirement =
    when (this) {
        GroupLeaveRequirementDto.CanLeave -> GroupLeaveRequirement.CanLeave
        is GroupLeaveRequirementDto.PromoteAdminFirst ->
            GroupLeaveRequirement.PromoteAdminFirst(contactIds)
    }

internal fun GroupMembershipEntity.toMembershipResult(): MembershipResult =
    MembershipResult(
        membershipId = membershipId,
        sourceInvitationId = sourceInvitationId,
        groupId = groupId,
        peerId = contactId,
        perspective =
            DataMembershipPerspective.entries
                .firstOrNull { value -> value.name == perspective }
                ?.let { value -> MembershipPerspective.valueOf(value.name) }
                ?: error("Unknown membership perspective: $perspective"),
        status =
            DataMembershipStatus.entries
                .firstOrNull { value -> value.name == status }
                ?.let { value -> MembershipStatus.valueOf(value.name) }
                ?: error("Unknown membership status: $status"),
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )
