package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.model.GroupVerificationState
import com.cbgm.securechat.feature.chats.domain.repository.GroupVerificationRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveGroupVerificationUseCase(
    private val repository: GroupVerificationRepository,
    private val observeContacts: ObserveContacts
) {
    operator fun invoke(groupId: String): Flow<GroupVerificationState> =
        combine(
            repository.observePairs(groupId),
            observeContacts(),
            repository.observeContext(groupId)
        ) { pairs, contacts, context ->
            val ownerDisplayName =
                context.ownerContactId
                    ?.let { ownerContactId ->
                        contacts.firstOrNull { contact -> contact.id == ownerContactId }
                    }?.displayName
                    ?.takeIf(String::isNotBlank)
                    .orEmpty()

            GroupVerificationState(
                context = context,
                ownerDisplayName = ownerDisplayName,
                pairs = pairs
            )
        }
}
