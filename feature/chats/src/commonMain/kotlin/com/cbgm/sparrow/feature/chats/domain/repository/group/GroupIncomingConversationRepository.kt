package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket

interface GroupIncomingConversationRepository {
    suspend fun recordWelcomeRestart(
        packet: GroupCreatedPacket,
        invitationId: String?,
        isFirstWelcome: Boolean,
        persistedAt: Long
    ): Result<Unit>

    suspend fun getParticipantIds(groupId: String): Result<List<String>>

    suspend fun installWelcome(
        packet: GroupCreatedPacket,
        previousSigningKeysByContactId: Map<String, ByteArray>,
        contactIdsByMember: List<String?>,
        contactDisplayNames: Map<String, String>,
        persistedAt: Long
    ): Result<Set<String>>

    suspend fun applyRemoval(packet: GroupMemberRemovedPacket): Result<Unit>

    suspend fun prepareDeletion(groupId: String): Result<Unit>

    suspend fun finishDeletion(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ): Result<Unit>
}
