package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupDetailsContext
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationState
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarReadRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationReadRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupDescriptionReadRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationReadRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

class ObserveGroupDetailsContextUseCase(
    private val verificationRepository: GroupVerificationReadRepository,
    private val membershipRepository: GroupMembershipRepository,
    private val conversationRepository: GroupConversationReadRepository,
    private val avatarRepository: GroupAvatarReadRepository,
    private val descriptionRepository: GroupDescriptionReadRepository,
    private val contactRepository: ContactRepository,
    private val observeVerificationContext: ObserveGroupVerificationContextUseCase
) {
    operator fun invoke(groupId: String): Flow<GroupDetailsContext> {
        val verificationFlow =
            combine(
                verificationRepository.observePairs(groupId),
                contactRepository.observeContacts(),
                observeVerificationContext(groupId)
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
                .catch { error ->
                    SparrowLog.error("ObserveGroupDetailsContextUseCase", "Could not observe group details", error)
                    emit(null)
                },
            avatarRepository.observeMetadata(groupId),
            descriptionRepository.observe(groupId)
        ) { verification, administration, conversation, avatarMetadata, description ->
            GroupDetailsContext(
                verification = verification,
                administration = administration,
                conversation = conversation,
                avatarMetadata = avatarMetadata,
                description = description
            )
        }
    }
}
