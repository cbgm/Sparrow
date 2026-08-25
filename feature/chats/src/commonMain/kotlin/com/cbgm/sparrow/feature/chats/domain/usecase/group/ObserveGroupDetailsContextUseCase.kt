package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupDetailsContext
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationState
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMembershipRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

class ObserveGroupDetailsContextUseCase(
    private val verificationRepository: GroupVerificationRepository,
    private val membershipRepository: GroupMembershipRepository,
    private val conversationRepository: GroupConversationRepository,
    private val avatarRepository: GroupAvatarRepository,
    private val contactRepository: ContactRepository
) {
    operator fun invoke(groupId: String): Flow<GroupDetailsContext> {
        val verificationFlow =
            combine(
                verificationRepository.observePairs(groupId),
                contactRepository.observeContacts(),
                verificationRepository.observeContext(groupId)
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

        return combine(
            verificationFlow,
            membershipRepository.observeAdministration(groupId),
            conversationRepository
                .observe(groupId)
                .onStart { emit(null) }
                .catch { emit(null) },
            avatarRepository.observe(groupId)
        ) { verification, administration, conversation, avatar ->
            GroupDetailsContext(
                verification = verification,
                administration = administration,
                conversation = conversation,
                avatar = avatar
            )
        }
    }
}
