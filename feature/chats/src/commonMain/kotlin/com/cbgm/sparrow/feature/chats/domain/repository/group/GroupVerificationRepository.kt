package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationPair
import kotlinx.coroutines.flow.Flow

interface GroupVerificationRepository {
    fun observePairs(groupId: String): Flow<List<GroupVerificationPair>>
}
