package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipEvent
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager

internal class GroupCreatedIncomingProcessor(
    private val groupSecurityManager: GroupSecurityManager,
    private val groupSecurityDao: GroupSecurityDao,
    private val groupInvitationDao: GroupInvitationDao,
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

            val invitation = groupInvitationDao.findByGroupAndContact(packet.groupId, context.contactId)
            val isFirstWelcome = groupSecurityDao.findState(packet.groupId) == null
            validateInvitation(packet, invitation, isFirstWelcome)

            val welcome =
                welcomeSecurityProcessor.openAndTrustWelcome(
                    packet = packet,
                    senderContactId = context.contactId,
                    isFirstWelcome = isFirstWelcome
                )
            val previousMembership = welcomePersistence.loadPreviousMembership(packet.groupId)
            val persistedAt = maxOf(packet.createdAtEpochMilliseconds, context.receivedAtEpochMilliseconds)

            welcomePersistence.persistConversation(packet, persistedAt)
            welcomePersistence.recordMembershipRestartIfNeeded(
                packet = packet,
                invitationId = invitation?.invitationId,
                isFirstWelcome = isFirstWelcome,
                persistedAt = persistedAt
            )

            val membership = membershipResolver.resolve(packet, context.contactId, welcome)
            val referenceAdmin =
                welcomeSecurityProcessor.validateAuthorityAndResolveReferenceAdmin(
                    packet = packet,
                    senderContactId = context.contactId,
                    welcome = welcome,
                    membership = membership
                )

            welcomeSecurityProcessor.persistGroupSecurity(
                welcome = welcome,
                membership = membership,
                referenceAdmin = referenceAdmin,
                persistedAt = persistedAt
            )
            welcomePersistence.replaceMembership(packet, previousMembership, membership, persistedAt)
            welcomeSecurityProcessor.sendReadyAcknowledgement(packet, context.contactId, welcome)
            advanceInvitation(invitation, isFirstWelcome, persistedAt)
        }

    private fun validateInvitation(
        packet: GroupCreatedPacket,
        invitation: GroupInvitationEntity?,
        isFirstWelcome: Boolean
    ) {
        if (!isFirstWelcome) return

        val acceptedInvitation = invitation ?: error("Accepted group invitation was not found")
        check(acceptedInvitation.status.isAcceptedWelcomeStatus()) {
            "Group welcome arrived before the invitation was accepted"
        }
        check(
            packet.packetId ==
                groupSecurityManager.welcomePacketId(
                    groupId = packet.groupId,
                    invitationId = acceptedInvitation.invitationId,
                    epoch = packet.epoch
                )
        ) {
            "Group welcome does not belong to the current invitation"
        }
    }

    private suspend fun advanceInvitation(
        invitation: GroupInvitationEntity?,
        isFirstWelcome: Boolean,
        persistedAt: Long
    ) {
        val acceptedInvitation = invitation ?: return
        when (acceptedInvitation.status) {
            GroupInvitationStatus.JOIN_SENT.name -> markWaitingForActivation(acceptedInvitation, persistedAt)
            GroupInvitationStatus.WAITING_FOR_ACTIVATION.name,
            GroupInvitationStatus.ACTIVE.name,
            GroupInvitationStatus.LEAVE_SENT.name -> Unit
            else -> if (isFirstWelcome) error("Group welcome arrived before the invitation was accepted")
        }
    }

    private suspend fun markWaitingForActivation(
        invitation: GroupInvitationEntity,
        persistedAt: Long
    ) {
        val updated =
            groupInvitationDao.updateStatus(
                invitationId = invitation.invitationId,
                expectedStatus = GroupInvitationStatus.JOIN_SENT.name,
                newStatus =
                    GroupMembershipStateMachine.transition(
                        invitation.status,
                        GroupMembershipEvent.WELCOME_RECEIVED
                    ).name,
                updatedAt = maxOf(invitation.createdAtEpochMilliseconds, persistedAt)
            )
        check(updated == 1) { "Group invitation changed while the welcome was applied" }
    }

    private fun String.isAcceptedWelcomeStatus(): Boolean =
        this == GroupInvitationStatus.JOIN_SENT.name ||
            this == GroupInvitationStatus.WAITING_FOR_ACTIVATION.name ||
            this == GroupInvitationStatus.ACTIVE.name ||
            this == GroupInvitationStatus.LEAVE_SENT.name
}
