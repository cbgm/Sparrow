package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.model.group.GroupVerificationState
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupVerificationRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveGroupVerificationUseCase(
    private val repository: GroupVerificationRepository,
    private val observeContacts: ObserveContacts
) {
    operator fun invoke(groupId: String): Flow<GroupVerificationState> =
        combine(repository.observePairs(groupId), observeContacts(), repository.observeContext(groupId)) { pairs, contacts, context ->
            val ownerDisplayName = context.ownerContactId?.let { ownerId -> contacts.firstOrNull { it.id == ownerId } }?.displayName?.takeIf(String::isNotBlank).orEmpty()
            GroupVerificationState(context = context, ownerDisplayName = ownerDisplayName, pairs = pairs)
        }
}
