package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationState
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveGroupVerificationUseCase(
    private val repository: GroupVerificationRepository,
    private val contactRepository: ContactRepository
) {
    operator fun invoke(groupId: String): Flow<GroupVerificationState> =
        combine(
            repository.observePairs(groupId),
            contactRepository.observeContacts(),
            repository.observeContext(groupId)
        ) { pairs, contacts, context ->
            val ownerDisplayName =
                context.ownerContactId
                    ?.let { ownerId -> contacts.firstOrNull { contact -> contact.id == ownerId } }
                    ?.displayName
                    ?.takeIf(String::isNotBlank)
                    .orEmpty()
            GroupVerificationState(
                context = context,
                ownerDisplayName = ownerDisplayName,
                pairs = pairs
            )
        }
}
