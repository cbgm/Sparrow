package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.model.GROUP_ADMIN_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPeerDto
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberPromotionResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider

internal class GroupMemberPromotionDataSource(
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val groupEpochSecurity: GroupEpochSecurityDataSource,
    private val securityStore: GroupSecurityStoreDataSource,
    private val membershipLock: GroupMembershipLock,
    private val epochDataSource: GroupEpochDataSource
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
        val updatedAt = maxOf(context.createdAtEpochMilliseconds, SystemClock.nowEpochMilliseconds())
        rotateLocked(groupId, currentEpoch, context, mapOf(contactId to GROUP_ADMIN_ROLE), updatedAt)
        return GroupMemberPromotionResult(
            groupId = groupId,
            contactId = contactId,
            epoch = currentEpoch + 1,
            updatedAtEpochMilliseconds = updatedAt
        )
    }

    /**
     * The member an admin named when leaving rotates once more: the departed admin generated the epoch it left
     * with and still knows that key. Only an admin that is still on [expectedEpoch] rotates, so a repeated
     * welcome never rotates twice.
     */
    suspend fun rotateAfterAdminLeft(
        groupId: String,
        expectedEpoch: Int,
        context: GroupMembershipContext
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            membershipLock.withLock {
                val state = securityStore.findState(groupId) ?: return@withLock
                if (!state.localRole.isGroupAdminRole() || state.currentEpoch != expectedEpoch) return@withLock
                val updatedAt = maxOf(context.createdAtEpochMilliseconds, SystemClock.nowEpochMilliseconds())
                rotateLocked(groupId, state.currentEpoch, context, emptyMap(), updatedAt)
            }
        }

    private suspend fun rotateLocked(
        groupId: String,
        currentEpoch: Int,
        context: GroupMembershipContext,
        roleOverrides: Map<String, String>,
        updatedAt: Long
    ) {
        val contacts =
            epochDataSource
                .loadCurrentParticipantContacts(groupId)
                .sortedBy(GroupMembershipPeerDto::id)
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
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
                memberKeys = epochDataSource.createMemberKeys(groupId, currentEpoch + 1, contacts, roleOverrides),
                recipients = epochDataSource.createRecipients(groupId, contacts),
                localSigningKeyPair = localSigningKeyPair
            ).getOrThrow()
    }

    private suspend fun requireCurrentMemberKey(
        groupId: String,
        contactId: String
    ) =
        securityStore.findCurrentRemoteMemberKey(groupId, contactId)
            ?: error("Group member is not part of the current group epoch")
}
