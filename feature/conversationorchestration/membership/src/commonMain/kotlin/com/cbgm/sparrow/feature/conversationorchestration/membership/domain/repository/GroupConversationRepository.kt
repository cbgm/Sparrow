package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupConversationKind
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantActivation
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantAddition
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantRemoval
import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

/** The group conversation row, its participants and the system events that record their changes. */
interface GroupConversationRepository {
    suspend fun create(title: String): Result<String>

    suspend fun getMembershipContext(groupId: String): Result<GroupMembershipContext>

    /** What the conversation row with ID [groupId] is; an invitation must not overwrite a foreign one. */
    suspend fun findKind(groupId: String): Result<GroupConversationKind>

    suspend fun isParticipant(groupId: String, contactId: String): Result<Boolean>

    /** False when the user deleted this group after the offer was created; it must not reappear. */
    suspend fun stageIncoming(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean>

    suspend fun showAcceptedIncoming(groupId: String): Result<Unit>

    suspend fun discardPending(groupId: String, updatedAtEpochMilliseconds: Long): Result<Unit>

    suspend fun addParticipant(addition: GroupParticipantAddition): Result<Unit>

    suspend fun promoteParticipant(groupId: String, contactId: String, updatedAtEpochMilliseconds: Long): Result<Unit>

    suspend fun removeParticipant(removal: GroupParticipantRemoval): Result<Unit>

    /** Records the DISCOVERY history event without granting message routing. */
    suspend fun recordRemoteMemberAdded(activation: GroupParticipantActivation): Result<Unit>

    /** FINAL activation makes the member an active participant. */
    suspend fun activateRemoteParticipant(activation: GroupParticipantActivation): Result<Unit>

    suspend fun endLocalMembership(end: GroupLocalMembershipEnd): Result<Unit>

    suspend fun deleteLocalConversation(groupId: String, deletedAtEpochMilliseconds: Long): Result<Unit>
}
