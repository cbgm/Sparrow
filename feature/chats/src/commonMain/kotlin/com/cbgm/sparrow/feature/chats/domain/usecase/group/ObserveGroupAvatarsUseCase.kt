package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class ObserveGroupAvatarsUseCase(
    private val repository: GroupAvatarRepository
) {
    operator fun invoke(groupIds: Set<String>): Flow<Map<String, ByteArray?>> {
        if (groupIds.isEmpty()) return flowOf(emptyMap())

        val flows =
            groupIds.map { groupId ->
                repository
                    .observe(groupId)
                    .map { avatar -> groupId to avatar.bytes }
                    .onStart { emit(groupId to null) }
                    .catch { emit(groupId to null) }
            }

        return combine(flows) { entries -> entries.toMap() }
    }
}
