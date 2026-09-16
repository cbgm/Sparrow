package com.cbgm.sparrow.feature.membership.data.coordinator

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.membership.data.GroupMembershipIdentity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipBroadcastDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipSecurityDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipVerificationDataSource
import com.cbgm.sparrow.feature.membership.data.model.GROUP_ADMIN_ROLE
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole

class GroupMemberPromotionCoordinator(
    private val chatDao: ChatDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val groupSecurityManager: GroupMembershipSecurityDataSource,
    private val groupVerificationCoordinator: GroupMembershipVerificationDataSource,
    private val membershipLock: GroupMembershipLock,
    private val identity: GroupMembershipIdentity,
    private val epochCoordinator: GroupEpochCoordinator,
    private val packetBroadcaster: GroupMembershipBroadcastDataSource
) {
    suspend fun promoteMember(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            membershipLock.withLock {
                promoteMemberLocked(groupId, contactId)
            }
        }

    private suspend fun promoteMemberLocked(
        groupId: String,
        contactId: String
    ) {
        val conversation = chatDao.findConversationById(groupId) ?: error("Group conversation was not found")
        val currentEpoch =
            groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
                ?: error("Active group security state was not found")
        val participants = epochCoordinator.findCurrentParticipants(groupId)
        val target =
            participants.firstOrNull { participant -> participant.contactId == contactId }
                ?: error("Only an active group member can be promoted")
        if (target.role.isGroupAdminRole()) return

        val contacts =
            participants
                .map { participant -> identity.requireContact(participant.contactId) }
                .sortedBy(Contact::id)
        requireCurrentMemberKey(groupId, contactId)

        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val nextEpoch = currentEpoch + 1
        val roleOverrides = mapOf(contactId to GROUP_ADMIN_ROLE)
        val securedGroup =
            groupSecurityManager
                .rotateOwnedGroup(
                    groupId = groupId,
                    title = requireNotNull(conversation.title),
                    createdAtEpochMilliseconds = conversation.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                    memberPayloads =
                        epochCoordinator.createMemberPayloads(
                            groupId = groupId,
                            localIdentity = localIdentity,
                            localPhoneNumber = localPhoneNumber,
                            contacts = contacts,
                            roleOverrides = roleOverrides
                        ),
                    memberKeys = epochCoordinator.createMemberKeys(groupId, nextEpoch, contacts, roleOverrides),
                    recipients = epochCoordinator.createRecipients(groupId, contacts),
                    localSigningKeyPair = localSigningKeyPair
                ).getOrThrow()

        packetBroadcaster.enqueueAll(securedGroup.welcomePacketsByContactId).getOrThrow()
        check(chatDao.updateConversationParticipantRole(groupId, contactId, GROUP_ADMIN_ROLE) == 1) {
            "Promoted group member disappeared while the new epoch was created"
        }
        groupVerificationCoordinator.onOwnedMembershipChanged(groupId).getOrThrow()
        chatDao.updateConversationTimestamp(groupId, SystemClock.nowEpochMilliseconds())
    }

    private suspend fun requireCurrentMemberKey(
        groupId: String,
        contactId: String
    ) =
        groupSecurityManager
            .findRemoteMemberKey(groupId = groupId, contactId = contactId)
            .getOrThrow()
            ?: error("Group member is not part of the current group epoch")
}
