package com.cbgm.sparrow.feature.conversationorchestration.domain.port

import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

interface ConversationPort {
    /** Conversation-only side effects after Membership authenticates the welcome. */
    suspend fun recordIncomingGroupWelcomeRestart(
        packet: GroupCreatedPacket,
        invitationId: String?,
        isFirstWelcome: Boolean,
        persistedAt: Long
    ): Result<Unit>

    suspend fun getCurrentGroupParticipantIds(groupId: String): Result<List<String>>

    /** Conversation projection of an already-authenticated, Membership-persisted welcome. */
    suspend fun installIncomingGroupWelcome(
        packet: GroupCreatedPacket,
        previousSigningKeysByContactId: Map<String, ByteArray>,
        contactIdsByMember: List<String?>,
        contactDisplayNames: Map<String, String>,
        persistedAt: Long
    ): Result<Set<String>>

    /** Replays the Chats-owned group metadata after Membership accepted readiness. */
    suspend fun sendCurrentGroupMetadataTo(groupId: String, peerId: String)

    /** Chats alone owns verification records and snapshot broadcasting. */
    suspend fun refreshOwnedGroupVerification(groupId: String): Result<Unit>

    suspend fun onLocalGroupMemberActivated(groupId: String): Result<Unit>

    /** Persist a signed owner announcement without granting message-routing access. */
    suspend fun recordRemoteGroupMemberAdded(
        groupId: String,
        contactId: String,
        epoch: Int,
        activationId: String,
        memberDisplayName: String,
        joinedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun onRemoteGroupMemberActivated(
        groupId: String,
        contactId: String,
        role: String,
        epoch: Int,
        activationId: String,
        memberDisplayName: String,
        joinedAtEpochMilliseconds: Long
    ): Result<Unit>

    /** Chats owns attachments, installed group key, participants and verification records. */
    suspend fun applyIncomingGroupRemoval(
        packet: GroupMemberRemovedPacket,
        senderContactId: String
    ): Result<Unit>

    suspend fun prepareIncomingGroupDeletion(groupId: String): Result<Unit>

    suspend fun finishIncomingGroupDeletion(groupId: String, deletedAtEpochMilliseconds: Long): Result<Unit>

    suspend fun getOrCreateConversation(peerId: String): Result<String>

    suspend fun activateAuthorizedConversation(peerId: String): Result<Unit>

    suspend fun discardPendingAuthorizationMessages(peerId: String): Result<Unit>

    suspend fun runPendingAuthorizationCleanup()

    suspend fun findPeerId(conversationId: String): Result<String?>

    /** Chats-owned lookup for group receipt routing; null if the message or group is absent. */
    suspend fun findGroupIdForMessage(messageId: String): Result<String?>

    suspend fun deleteConversation(conversationId: String): Result<Unit>

    suspend fun getGroupTitle(groupId: String): Result<String>

    suspend fun getGroupMembershipContext(groupId: String): Result<GroupMembershipContext>

    suspend fun stageIncomingGroupConversation(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean>

    /** Publish the receiver's already-staged group after local invitation acceptance. */
    suspend fun showAcceptedIncomingGroupConversation(groupId: String): Result<Unit>

    suspend fun discardPendingGroupConversation(
        groupId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun addGroupParticipant(
        groupId: String,
        peerId: String,
        memberDisplayName: String,
        epoch: Int,
        joinedAtEpochMilliseconds: Long,
        eventId: String
    ): Result<Unit>

    suspend fun promoteGroupParticipant(
        groupId: String,
        peerId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun removeGroupParticipant(
        groupId: String,
        peerId: String,
        memberDisplayName: String,
        epoch: Int,
        eventId: String,
        updatedAtEpochMilliseconds: Long,
        memberLeft: Boolean
    ): Result<Unit>

    suspend fun endLocalGroupMembership(
        groupId: String,
        referenceId: String,
        epoch: Int,
        endedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun deleteLocalGroupConversation(groupId: String, deletedAtEpochMilliseconds: Long): Result<Unit>

    suspend fun deleteGroupAttachments(groupId: String): Result<Unit>
}
