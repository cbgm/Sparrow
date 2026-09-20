package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class TransferGroupAdminAndLeaveUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEnd> =
        repository.transferAdminAndLeave(groupId, contactId, context)
}
