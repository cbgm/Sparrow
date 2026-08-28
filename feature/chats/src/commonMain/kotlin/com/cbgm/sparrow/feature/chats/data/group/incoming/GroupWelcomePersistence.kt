package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory

internal class GroupWelcomePersistence(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupSecurityDao: GroupSecurityDao
) {
    suspend fun loadPreviousMembership(groupId: String): PreviousGroupMembershipDto {
        val participants = chatDao.findConversationParticipants(groupId)
        val previousEpoch = groupSecurityDao.findState(groupId)?.currentEpoch
        val signingKeys =
            if (previousEpoch == null) {
                emptyMap()
            } else {
                participants.associate { participant ->
                    participant.contactId to
                        groupSecurityDao
                            .findMemberKey(
                                groupId = groupId,
                                epoch = previousEpoch,
                                contactId = participant.contactId
                            )?.signingPublicKey
                }
            }
        return PreviousGroupMembershipDto(participants, signingKeys)
    }

    suspend fun persistConversation(
        packet: GroupCreatedPacket,
        persistedAt: Long
    ) {
        chatDao.upsertConversation(
            ConversationEntity(
                id = packet.groupId,
                contactId = null,
                type = GROUP_CONVERSATION_TYPE,
                title = packet.title,
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = persistedAt
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
                chatDao.findMessageTimestampByTransportMode(
                    conversationId = packet.groupId,
                    transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_LEFT_TRANSPORT_MODE
                ),
                chatDao.findMessageTimestampByTransportMode(
                    conversationId = packet.groupId,
                    transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_REMOVED_TRANSPORT_MODE
                )
            ).maxOrNull() ?: return
        val latestStartAt =
            chatDao.findMessageTimestampByTransportMode(
                conversationId = packet.groupId,
                transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_STARTED_TRANSPORT_MODE
            )
        if (latestStartAt != null && latestStartAt > latestEndAt) return

        chatDao.upsertMessage(
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
        current: ResolvedGroupMembershipDto,
        persistedAt: Long
    ) {
        val currentParticipantIds = current.participants.mapTo(mutableSetOf()) { it.contactId }
        val previousParticipantIds = previous.participants.mapTo(mutableSetOf()) { it.contactId }
        val removedMessages = removedMembershipMessages(packet, previous, currentParticipantIds, persistedAt)
        val addedMessages = addedMembershipMessages(packet, current, previousParticipantIds, persistedAt)

        chatDao.replaceConversationParticipantsWithMessages(
            conversationId = packet.groupId,
            participants = current.participants,
            messages = removedMessages + addedMessages
        )
    }

    private suspend fun removedMembershipMessages(
        packet: GroupCreatedPacket,
        previous: PreviousGroupMembershipDto,
        currentParticipantIds: Set<String>,
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
                        contactName = membershipDisplayName(participant.contactId),
                        createdAtEpochMilliseconds = persistedAt
                    )
                } else {
                    GroupMembershipMessageFactory.memberRemoved(
                        conversationId = packet.groupId,
                        epoch = packet.epoch,
                        contactId = participant.contactId,
                        contactName = membershipDisplayName(participant.contactId),
                        createdAtEpochMilliseconds = persistedAt
                    )
                }
            }

    private suspend fun addedMembershipMessages(
        packet: GroupCreatedPacket,
        current: ResolvedGroupMembershipDto,
        previousParticipantIds: Set<String>,
        persistedAt: Long
    ) =
        if (previousParticipantIds.isNotEmpty() && chatDao.hasMessages(packet.groupId)) {
            current.participants
                .filterNot { participant -> participant.contactId in previousParticipantIds }
                .map { participant ->
                    GroupMembershipMessageFactory.memberAdded(
                        conversationId = packet.groupId,
                        epoch = packet.epoch,
                        contactId = participant.contactId,
                        contactName = membershipDisplayName(participant.contactId),
                        createdAtEpochMilliseconds = persistedAt
                    )
                }
        } else {
            emptyList()
        }

    private suspend fun membershipDisplayName(contactId: String): String =
        contactDao
            .findById(contactId)
            ?.contact
            ?.displayName
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: "Member"

    private fun GroupCreatedPacket.memberLeft(previousSigningPublicKey: ByteArray?): Boolean {
        val change = membershipChange ?: return false
        return change.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT &&
            previousSigningPublicKey?.contentEquals(change.memberSigningPublicKey) == true
    }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
