package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupIncomingConversationDataSource
import com.cbgm.sparrow.feature.chats.data.group.incoming.PreviousGroupMembershipDto
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupIncomingConversationRepository

internal class GroupIncomingConversationRepositoryImpl(
    private val dataSource: GroupIncomingConversationDataSource
) : GroupIncomingConversationRepository {
    override suspend fun recordWelcomeRestart(
        packet: GroupCreatedPacket,
        invitationId: String?,
        isFirstWelcome: Boolean,
        persistedAt: Long
    ): Result<Unit> = runCatching {
        if (!isFirstWelcome) return@runCatching
        val latestEndAt = listOfNotNull(
            dataSource.findMessageTimestampByTransportMode(
                packet.groupId,
                GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_LEFT_TRANSPORT_MODE
            ),
            dataSource.findMessageTimestampByTransportMode(
                packet.groupId,
                GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_REMOVED_TRANSPORT_MODE
            )
        ).maxOrNull() ?: return@runCatching
        val latestStartAt = dataSource.findMessageTimestampByTransportMode(
            packet.groupId,
            GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_STARTED_TRANSPORT_MODE
        )
        if (latestStartAt != null && latestStartAt > latestEndAt) return@runCatching

        dataSource.upsertMessage(
            GroupMembershipMessageFactory.localMembershipStarted(
                conversationId = packet.groupId,
                referenceId = invitationId ?: packet.packetId,
                epoch = packet.epoch,
                createdAtEpochMilliseconds = persistedAt
            )
        )
    }

    override suspend fun getParticipantIds(groupId: String): Result<List<String>> =
        runCatching { dataSource.findConversationParticipants(groupId).map { it.contactId } }

    override suspend fun installWelcome(
        packet: GroupCreatedPacket,
        previousSigningKeysByContactId: Map<String, ByteArray>,
        contactIdsByMember: List<String?>,
        contactDisplayNames: Map<String, String>,
        persistedAt: Long
    ): Result<Set<String>> = runCatching {
        require(contactIdsByMember.size == packet.members.size) {
            "Group welcome contact mapping is incomplete"
        }
        val previous = loadPreviousMembership(packet.groupId, previousSigningKeysByContactId)
        val previouslyActiveIds = previous.participants.mapTo(hashSetOf()) { it.contactId }
        val current = packet.members.mapIndexedNotNull { index, member ->
            val contactId = contactIdsByMember[index] ?: return@mapIndexedNotNull null
            if (previouslyActiveIds.isNotEmpty() && contactId !in previouslyActiveIds) {
                return@mapIndexedNotNull null
            }
            ConversationParticipantEntity(
                conversationId = packet.groupId,
                contactId = contactId,
                role = member.role,
                joinedAtEpochMilliseconds = packet.createdAtEpochMilliseconds
            )
        }
        val removed = replaceMembership(
            packet = packet,
            previous = previous,
            current = current,
            contactDisplayNames = contactDisplayNames,
            persistedAt = persistedAt
        )
        dataSource.upsertConversation(
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
        removed
    }

    override suspend fun applyRemoval(packet: GroupMemberRemovedPacket): Result<Unit> = runCatching {
        val wasLocallyHidden = dataSource.hasMessageWithTransportMode(
            conversationId = packet.groupId,
            transportMode = GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE
        )
        if (!wasLocallyHidden) {
            val message = if (packet.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                GroupMembershipMessageFactory.localMembershipLeft(
                    conversationId = packet.groupId,
                    invitationId = packet.invitationId,
                    epoch = packet.epoch,
                    createdAtEpochMilliseconds = packet.removedAtEpochMilliseconds
                )
            } else {
                GroupMembershipMessageFactory.localMembershipRemoved(
                    conversationId = packet.groupId,
                    invitationId = packet.invitationId,
                    epoch = packet.epoch,
                    createdAtEpochMilliseconds = packet.removedAtEpochMilliseconds
                )
            }
            dataSource.applyLocalGroupRemoval(message)
        }
        dataSource.deleteVerificationRows(packet.groupId)
    }

    override suspend fun prepareDeletion(groupId: String): Result<Unit> = runCatching {
        dataSource.deleteConversationParticipants(groupId)
        dataSource.deleteVerificationRows(groupId)
    }

    override suspend fun finishDeletion(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        dataSource.updateConversationTimestamp(groupId, deletedAtEpochMilliseconds)
    }

    private suspend fun loadPreviousMembership(
        groupId: String,
        previousSigningKeysByContactId: Map<String, ByteArray>
    ): PreviousGroupMembershipDto {
        val participants = dataSource.findConversationParticipants(groupId)
        return PreviousGroupMembershipDto(
            participants = participants,
            signingKeysByContactId = participants.associate { participant ->
                participant.contactId to previousSigningKeysByContactId[participant.contactId]
            }
        )
    }

    private suspend fun replaceMembership(
        packet: GroupCreatedPacket,
        previous: PreviousGroupMembershipDto,
        current: List<ConversationParticipantEntity>,
        contactDisplayNames: Map<String, String>,
        persistedAt: Long
    ): Set<String> {
        val currentParticipantIds = current.mapTo(mutableSetOf()) { it.contactId }
        val previousParticipantIds = previous.participants.mapTo(mutableSetOf()) { it.contactId }
        val removedParticipantIds = previousParticipantIds - currentParticipantIds
        val removedMessages = previous.participants
            .filterNot { it.contactId in currentParticipantIds }
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
        val addedMessages = if (previousParticipantIds.isNotEmpty()) {
            current.filterNot { it.contactId in previousParticipantIds }.map { participant ->
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
        dataSource.replaceConversationParticipantsWithMessages(
            conversationId = packet.groupId,
            participants = current,
            messages = removedMessages + addedMessages
        )
        return removedParticipantIds
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
