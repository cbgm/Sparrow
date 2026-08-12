package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.model.GroupAdministrationState
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupAdministration(
    private val repository: ChatsRepository
) {
    operator fun invoke(groupId: String): Flow<GroupAdministrationState> =
        repository.observeGroupAdministration(groupId)
}
