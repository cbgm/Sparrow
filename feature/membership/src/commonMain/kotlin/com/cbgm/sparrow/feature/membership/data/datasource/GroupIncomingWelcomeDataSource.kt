package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.model.GroupIncomingWelcomeAuthorization

/** Owns the membership/security state used by a received group welcome. Chats installs only chat state. */
internal class GroupIncomingWelcomeDataSource(
    private val membershipStore: GroupMembershipStoreDataSource,
    private val securityStore: GroupSecurityStoreDataSource,
    private val lock: GroupMembershipLock
) {
    suspend fun authorize(
        groupId: String,
        senderContactId: String,
        packetId: String,
        epoch: Int
    ): GroupIncomingWelcomeAuthorization? {
        val currentSecurity = securityStore.findState(groupId)
        if (currentSecurity?.localRole == GROUP_LEFT_ROLE) return null
        val firstWelcome = currentSecurity == null
        val membership = membershipStore.findByGroupAndContact(groupId, senderContactId)
        if (firstWelcome) {
            val accepted = membership ?: error("Accepted group membership was not found")
            check(accepted.status.isAcceptedWelcomeStatus()) {
                "Group welcome arrived before the membership was accepted"
            }
            check(packetId == "group-welcome-$groupId-${accepted.sourceInvitationId}-$epoch") {
                "Group welcome does not belong to the current membership"
            }
        }
        val previousKeys = currentSecurity?.let { security ->
            securityStore.findMemberKeys(groupId, security.currentEpoch)
                .associate { member -> member.contactId to member.signingPublicKey.copyOf() }
        }.orEmpty()
        val priorAuthority = currentSecurity?.let { security ->
            val member = securityStore.findMemberKey(groupId, security.currentEpoch, senderContactId)
                ?: error("Group update sender is not part of the current epoch")
            check(member.role.isGroupAdminRole()) { "Group update sender is not an admin" }
            member
        }
        return GroupIncomingWelcomeAuthorization(
            isFirstWelcome = firstWelcome,
            sourceInvitationId = membership?.sourceInvitationId,
            previousSigningKeysByContactId = previousKeys,
            priorAdminEncryptionPublicKey = priorAuthority?.encryptionPublicKey?.copyOf(),
            priorAdminSigningPublicKey = priorAuthority?.signingPublicKey?.copyOf()
        )
    }

    suspend fun complete(
        groupId: String,
        senderContactId: String,
        isFirstWelcome: Boolean,
        removedContactIds: Set<String>,
        persistedAt: Long
    ): Unit = lock.withLock {
        // A stale/removed owner membership must not be resurrected by an updated welcome.
        removedContactIds.forEach { contactId ->
            val membership = membershipStore.findByGroupContactAndPerspective(
                groupId,
                contactId,
                GroupMembershipPerspective.OWNER.name
            ) ?: return@forEach
            if (membership.status.isTerminalMembershipStatus()) return@forEach
            membershipStore.updateStatus(
                membershipId = membership.membershipId,
                expectedStatus = membership.status,
                newStatus = GroupMembershipStatus.REMOVED.name,
                updatedAt = persistedAt
            )
        }

        val membership = membershipStore.findByGroupAndContact(groupId, senderContactId)
            ?: return@withLock
        when (membership.status) {
            GroupMembershipStatus.JOIN_REQUEST_SENT.name -> {
                val updated = membershipStore.updateStatus(
                    membershipId = membership.membershipId,
                    expectedStatus = GroupMembershipStatus.JOIN_REQUEST_SENT.name,
                    newStatus = GroupMembershipStateMachine.transition(
                        membership.status,
                        GroupMembershipEvent.WELCOME_RECEIVED
                    ).name,
                    updatedAt = maxOf(membership.createdAtEpochMilliseconds, persistedAt)
                )
                check(updated == 1) { "Group membership changed while the welcome was applied" }
            }
            GroupMembershipStatus.WAITING_FOR_ACTIVATION.name,
            GroupMembershipStatus.ACTIVE.name,
            GroupMembershipStatus.LEAVE_REQUESTED.name -> Unit
            else -> if (isFirstWelcome) error("Group welcome arrived before the membership was accepted")
        }
    }

    private fun String.isAcceptedWelcomeStatus() =
        this == GroupMembershipStatus.JOIN_REQUEST_SENT.name ||
            this == GroupMembershipStatus.WAITING_FOR_ACTIVATION.name ||
            this == GroupMembershipStatus.ACTIVE.name ||
            this == GroupMembershipStatus.LEAVE_REQUESTED.name

    private fun String.isTerminalMembershipStatus() =
        this == GroupMembershipStatus.FAILED.name ||
            this == GroupMembershipStatus.REMOVED.name ||
            this == GroupMembershipStatus.GROUP_DELETED.name
}
