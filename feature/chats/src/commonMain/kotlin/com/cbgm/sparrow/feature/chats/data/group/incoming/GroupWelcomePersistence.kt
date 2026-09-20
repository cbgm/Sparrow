package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupIncomingConversationDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory

internal class GroupWelcomePersistence(
    private val incomingConversationDataSource: GroupIncomingConversationDataSource
) {
    suspend fun loadPreviousMembership(
        groupId: String,
        previousSigningKeysByContactId: Map<String, ByteArray>
    ): PreviousGroupMembershipDto {
        val participants = incomingConversationDataSource.findConversationParticipants(groupId)
        return PreviousGroupMembershipDto(
            participants = participants,
            signingKeysByContactId = participants.associate { participant ->
                participant.contactId to previousSigningKeysByContactId[participant.contactId]
            }
        )
    }

    suspend fun persistConversation(
        packet: GroupCreatedPacket,
        persistedAt: Long
    ) {
        incomingConversationDataSource.upsertConversation(
            ConversationEntity(
                id = packet.groupId,
                contactId = null,
                type = GROUP_CONVERSATION_TYPE,
                title = packet.title,
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = persistedAt,
                isVisible = true
            )
        )
    }

    suspend fun recordMembershipRestartIfNeeded(
        packet: GroupCreatedPacket,
        invitationId: String?,
        isFirstWelcome: Boolean,
        persistedAt: Long
    ) {
        if (!isFirstWelcome) return
        val latestEndAt =
            listOfNotNull(
                incomingConversationDataSource.findMessageTimestampByTransportMode(
                    conversationId = packet.groupId,
                    transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_LEFT_TRANSPORT_MODE
                ),
                incomingConversationDataSource.findMessageTimestampByTransportMode(
                    conversationId = packet.groupId,
                    transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_REMOVED_TRANSPORT_MODE
                )
            ).maxOrNull() ?: return
        val latestStartAt =
            incomingConversationDataSource.findMessageTimestampByTransportMode(
                conversationId = packet.groupId,
                transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_STARTED_TRANSPORT_MODE
            )
        if (latestStartAt != null && latestStartAt > latestEndAt) return

        incomingConversationDataSource.upsertMessage(
            GroupMembershipMessageFactory.localMembershipStarted(
                conversationId = packet.groupId,
                referenceId = invitationId ?: packet.packetId,
                epoch = packet.epoch,
                createdAtEpochMilliseconds = persistedAt
            )
        )
    }

    suspend fun replaceMembership(
        packet: GroupCreatedPacket,
        previous: PreviousGroupMembershipDto,
        current: List<ConversationParticipantEntity>,
        contactDisplayNames: Map<String, String>,
        persistedAt: Long
    ): Set<String> {
        val currentParticipantIds = current.mapTo(mutableSetOf()) { it.contactId }
        val previousParticipantIds = previous.participants.mapTo(mutableSetOf()) { it.contactId }
        val removedParticipantIds = previousParticipantIds - currentParticipantIds
        val removedMessages = removedMembershipMessages(packet, previous, currentParticipantIds, contactDisplayNames, persistedAt)
        val addedMessages = addedMembershipMessages(packet, current, previousParticipantIds, contactDisplayNames, persistedAt)

        incomingConversationDataSource.replaceConversationParticipantsWithMessages(
            conversationId = packet.groupId,
            participants = current,
            messages = removedMessages + addedMessages
        )
        return removedParticipantIds
    }

    private fun removedMembershipMessages(
        packet: GroupCreatedPacket,
        previous: PreviousGroupMembershipDto,
        currentParticipantIds: Set<String>,
        contactDisplayNames: Map<String, String>,
        persistedAt: Long
    ) =
        previous.participants
            .filterNot { participant -> participant.contactId in currentParticipantIds }
            .map { participant ->
                if (packet.memberLeft(previous.signingKeysByContactId[participant.contactId])) {
                    GroupMembershipMessageFactory.memberLeft(
                        conversationId = packet.groupId,
                        epoch = packet.epoch,
                        contactId = participant.contactId,
                        contactName = contactDisplayNames[participant.contactId] ?: "Member",
                        createdAtEpochMilliseconds = persistedAt
                    )
                } else {
                    GroupMembershipMessageFactory.memberRemoved(
                        conversationId = packet.groupId,
                        epoch = packet.epoch,
                        contactId = participant.contactId,
                        contactName = contactDisplayNames[participant.contactId] ?: "Member",
                        createdAtEpochMilliseconds = persistedAt
                    )
                }
            }

    private fun addedMembershipMessages(
        packet: GroupCreatedPacket,
        current: List<ConversationParticipantEntity>,
        previousParticipantIds: Set<String>,
        contactDisplayNames: Map<String, String>,
        persistedAt: Long
    ) =
        if (previousParticipantIds.isNotEmpty()) {
            current
                .filterNot { participant -> participant.contactId in previousParticipantIds }
                .map { participant ->
                    GroupMembershipMessageFactory.memberAdded(
                        conversationId = packet.groupId,
                        epoch = packet.epoch,
                        contactId = participant.contactId,
                        contactName = contactDisplayNames[participant.contactId] ?: "Member",
                        createdAtEpochMilliseconds = persistedAt
                    )
                }
        } else {
            emptyList()
        }

    private fun GroupCreatedPacket.memberLeft(previousSigningPublicKey: ByteArray?): Boolean {
        val change = membershipChange ?: return false
        return change.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT &&
            previousSigningPublicKey?.contentEquals(change.memberSigningPublicKey) == true
    }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
