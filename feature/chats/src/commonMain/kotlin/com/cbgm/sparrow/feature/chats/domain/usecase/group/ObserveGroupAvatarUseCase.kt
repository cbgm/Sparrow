package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart

class ObserveGroupAvatarUseCase(
    private val repository: GroupAvatarRepository
) {
    operator fun invoke(groupId: String): Flow<GroupAvatar> =
        repository
            .observe(groupId)
            .onStart { emit(GroupAvatar(groupId = groupId)) }
            .catch { emit(GroupAvatar(groupId = groupId)) }
}
