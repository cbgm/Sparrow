package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.model.GROUP_ADMIN_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPeerDto
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberPromotionResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

internal class GroupMemberPromotionDataSource(
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val groupEpochSecurity: GroupEpochSecurityDataSource,
    private val securityStore: GroupSecurityStoreDataSource,
    private val membershipLock: GroupMembershipLock,
    private val epochDataSource: GroupEpochDataSource,
    private val packetBroadcaster: GroupPacketBroadcaster
) {
    suspend fun promoteMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberPromotionResult> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            membershipLock.withLock {
                promoteMemberLocked(groupId, contactId, context)
            }
        }

    private suspend fun promoteMemberLocked(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): GroupMemberPromotionResult {
        val currentEpoch =
            securityStore.findOwnedGroupEpoch(groupId)
                ?: error("Active group security state was not found")
        val participants = epochDataSource.findCurrentParticipants(groupId)
        val target =
            participants.firstOrNull { participant -> participant.contactId == contactId }
                ?: error("Only an active group member can be promoted")
        if (target.role.isGroupAdminRole()) {
            return GroupMemberPromotionResult(
                groupId = groupId,
                contactId = contactId,
                epoch = currentEpoch,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        }

        requireCurrentMemberKey(groupId, contactId)
        val contacts =
            epochDataSource
                .loadCurrentParticipantContacts(groupId)
                .sortedBy(GroupMembershipPeerDto::id)

        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val nextEpoch = currentEpoch + 1
        val roleOverrides = mapOf(contactId to GROUP_ADMIN_ROLE)
        val updatedAt = maxOf(context.createdAtEpochMilliseconds, SystemClock.nowEpochMilliseconds())
        val securedGroup =
            groupEpochSecurity
                .rotateOwnedGroup(
                    groupId = groupId,
                    title = context.title,
                    createdAtEpochMilliseconds = context.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = updatedAt,
                    memberPayloads =
                        epochDataSource.createMemberPayloads(
                            groupId = groupId,
                            localIdentity = localIdentity,
                            localPhoneNumber = localPhoneNumber,
                            contacts = contacts,
                            roleOverrides = roleOverrides
                        ),
                    memberKeys = epochDataSource.createMemberKeys(groupId, nextEpoch, contacts, roleOverrides),
                    recipients = epochDataSource.createRecipients(groupId, contacts),
                    localSigningKeyPair = localSigningKeyPair
                ).getOrThrow()

        packetBroadcaster.enqueueAll(securedGroup.welcomePacketsByContactId).getOrThrow()

        return GroupMemberPromotionResult(
            groupId = groupId,
            contactId = contactId,
            epoch = nextEpoch,
            updatedAtEpochMilliseconds = updatedAt
        )
    }

    private suspend fun requireCurrentMemberKey(
        groupId: String,
        contactId: String
    ) =
        securityStore.findCurrentRemoteMemberKey(groupId, contactId)
            ?: error("Group member is not part of the current group epoch")
}
