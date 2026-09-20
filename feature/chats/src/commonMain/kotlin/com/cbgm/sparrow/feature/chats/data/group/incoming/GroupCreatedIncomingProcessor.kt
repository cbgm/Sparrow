package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity

/** Chats persists only the already-authenticated group welcome's conversation projection. */
internal class GroupCreatedIncomingProcessor(
    private val welcomePersistence: GroupWelcomePersistence
) {
    suspend fun process(
        packet: GroupCreatedPacket,
        previousSigningKeysByContactId: Map<String, ByteArray>,
        contactIdsByMember: List<String?>,
        contactDisplayNames: Map<String, String>,
        persistedAt: Long
    ): Result<Set<String>> = runCatching {
        require(contactIdsByMember.size == packet.members.size) { "Group welcome contact mapping is incomplete" }
        val previous = welcomePersistence.loadPreviousMembership(
            packet.groupId,
            previousSigningKeysByContactId
        )
        val participants = packet.members.mapIndexedNotNull { index, member ->
            val contactId = contactIdsByMember[index] ?: return@mapIndexedNotNull null
            ConversationParticipantEntity(
                conversationId = packet.groupId,
                contactId = contactId,
                role = member.role,
                joinedAtEpochMilliseconds = packet.createdAtEpochMilliseconds
            )
        }
        val removed = welcomePersistence.replaceMembership(
            packet = packet,
            previous = previous,
            current = participants,
            contactDisplayNames = contactDisplayNames,
            persistedAt = persistedAt
        )
        welcomePersistence.persistConversation(packet, persistedAt)
        removed
    }
}
