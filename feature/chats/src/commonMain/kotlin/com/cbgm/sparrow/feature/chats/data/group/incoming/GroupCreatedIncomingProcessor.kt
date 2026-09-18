package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus

internal class GroupCreatedIncomingProcessor(
    private val groupSecurityManager: GroupSecurityManager,
    private val groupSecurityDao: GroupSecurityDao,
    private val groupMembershipDao: GroupMembershipDao,
    private val welcomeSecurityProcessor: GroupWelcomeSecurityProcessor,
    private val membershipResolver: GroupWelcomeMembershipResolver,
    private val welcomePersistence: GroupWelcomePersistence
) {
    suspend fun process(
        context: IncomingPacketContext,
        packet: GroupCreatedPacket
    ): Result<Unit> =
        runCatching {
            if (groupSecurityManager.isLocalMembershipRetired(packet.groupId).getOrThrow()) {
                return@runCatching
            }

            val localMembership = groupMembershipDao.findByGroupAndContact(packet.groupId, context.contactId)
            val isFirstWelcome = groupSecurityDao.findState(packet.groupId) == null
            validateMembership(packet, localMembership, isFirstWelcome)

            val welcome =
                welcomeSecurityProcessor.openAndTrustWelcome(
                    packet = packet,
                    senderContactId = context.contactId,
                    isFirstWelcome = isFirstWelcome
                )
            val previousMembership = welcomePersistence.loadPreviousMembership(packet.groupId)
            val persistedAt = maxOf(packet.createdAtEpochMilliseconds, context.receivedAtEpochMilliseconds)

            welcomePersistence.recordMembershipRestartIfNeeded(
                packet = packet,
                invitationId = localMembership?.sourceInvitationId,
                isFirstWelcome = isFirstWelcome,
                persistedAt = persistedAt
            )

            val resolvedMembership = membershipResolver.resolve(packet, context.contactId, welcome)
            val referenceAdmin =
                welcomeSecurityProcessor.validateAuthorityAndResolveReferenceAdmin(
                    packet = packet,
                    senderContactId = context.contactId,
                    welcome = welcome,
                    membership = resolvedMembership
                )

            welcomeSecurityProcessor.persistGroupSecurity(
                welcome = welcome,
                membership = resolvedMembership,
                referenceAdmin = referenceAdmin,
                persistedAt = persistedAt
            )
            welcomePersistence.replaceMembership(packet, previousMembership, resolvedMembership, persistedAt)
            welcomeSecurityProcessor.sendReadyAcknowledgement(packet, context.contactId, welcome)
            advanceMembership(localMembership, isFirstWelcome, persistedAt)
            welcomePersistence.persistConversation(packet, persistedAt)
        }

    private fun validateMembership(
        packet: GroupCreatedPacket,
        membership: GroupMembershipEntity?,
        isFirstWelcome: Boolean
    ) {
        if (!isFirstWelcome) return

        val acceptedMembership = membership ?: error("Accepted group membership was not found")
        check(acceptedMembership.status.isAcceptedWelcomeStatus()) {
            "Group welcome arrived before the membership was accepted"
        }
        check(
            packet.packetId ==
                groupSecurityManager.welcomePacketId(
                    groupId = packet.groupId,
                    invitationId = acceptedMembership.sourceInvitationId,
                    epoch = packet.epoch
                )
        ) {
            "Group welcome does not belong to the current membership"
        }
    }

    private suspend fun advanceMembership(
        membership: GroupMembershipEntity?,
        isFirstWelcome: Boolean,
        persistedAt: Long
    ) {
        val acceptedMembership = membership ?: return
        when (acceptedMembership.status) {
            GroupMembershipStatus.JOIN_REQUEST_SENT.name -> markWaitingForActivation(acceptedMembership, persistedAt)
            GroupMembershipStatus.WAITING_FOR_ACTIVATION.name,
            GroupMembershipStatus.ACTIVE.name,
            GroupMembershipStatus.LEAVE_REQUESTED.name -> Unit
            else -> if (isFirstWelcome) error("Group welcome arrived before the membership was accepted")
        }
    }

    private suspend fun markWaitingForActivation(
        membership: GroupMembershipEntity,
        persistedAt: Long
    ) {
        val updated =
            groupMembershipDao.updateStatus(
                membershipId = membership.membershipId,
                expectedStatus = GroupMembershipStatus.JOIN_REQUEST_SENT.name,
                newStatus =
                    GroupMembershipStateMachine.transition(
                        membership.status,
                        GroupMembershipEvent.WELCOME_RECEIVED
                    ).name,
                updatedAt = maxOf(membership.createdAtEpochMilliseconds, persistedAt)
            )
        check(updated == 1) { "Group membership changed while the welcome was applied" }
    }

    private fun String.isAcceptedWelcomeStatus(): Boolean =
        this == GroupMembershipStatus.JOIN_REQUEST_SENT.name ||
            this == GroupMembershipStatus.WAITING_FOR_ACTIVATION.name ||
            this == GroupMembershipStatus.ACTIVE.name ||
            this == GroupMembershipStatus.LEAVE_REQUESTED.name
}
