package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupMembershipRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupAdministrationUseCase(
    private val repository: GroupMembershipRepository
) {
    operator fun invoke(
        groupId: String
    ): Flow<GroupAdministrationState> =
        repository.observeAdministration(groupId)
}
