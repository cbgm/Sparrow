package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupDetailsContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

class ObserveGroupDetailsContextUseCase(
    private val observeGroupVerification: ObserveGroupVerificationUseCase,
    private val observeGroupAdministration: ObserveGroupAdministrationUseCase,
    private val observeGroupConversation: ObserveGroupConversationUseCase,
    private val observeGroupAvatar: ObserveGroupAvatarUseCase
) {
    operator fun invoke(groupId: String): Flow<GroupDetailsContext> =
        combine(
            observeGroupVerification(groupId),
            observeGroupAdministration(groupId),
            observeGroupConversation(groupId)
                .onStart { emit(null) }
                .catch { emit(null) },
            observeGroupAvatar(groupId)
        ) { verification, administration, conversation, avatar ->
            GroupDetailsContext(
                verification = verification,
                administration = administration,
                conversation = conversation,
                avatar = avatar
            )
        }
}
