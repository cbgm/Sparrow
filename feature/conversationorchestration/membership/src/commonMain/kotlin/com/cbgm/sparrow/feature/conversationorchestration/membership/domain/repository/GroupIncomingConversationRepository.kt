package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository

import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket

/** Conversation projection of group lifecycle packets that Membership already authenticated. */
interface GroupIncomingConversationRepository {
    suspend fun recordWelcomeRestart(
        packet: GroupCreatedPacket,
        invitationId: String?,
        isFirstWelcome: Boolean,
        persistedAt: Long
    ): Result<Unit>

    suspend fun getParticipantIds(groupId: String): Result<List<String>>

    /** Returns the contact IDs that are no longer participants after this welcome. */
    suspend fun installWelcome(
        packet: GroupCreatedPacket,
        previousSigningKeysByContactId: Map<String, ByteArray>,
        contactIdsByMember: List<String?>,
        contactDisplayNames: Map<String, String>,
        persistedAt: Long
    ): Result<Set<String>>

    suspend fun applyRemoval(packet: GroupMemberRemovedPacket): Result<Unit>

    suspend fun prepareDeletion(groupId: String): Result<Unit>

    suspend fun finishDeletion(groupId: String, deletedAtEpochMilliseconds: Long): Result<Unit>
}
