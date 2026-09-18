package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipSecurityDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipStoreDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipVerificationDataSource
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipAttemptDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus

class GroupMembershipAttemptDataSource(
    private val membershipStore: GroupMembershipStoreDataSource,
    private val groupSecurityManager: GroupMembershipSecurityDataSource,
    private val verificationDataSource: GroupMembershipVerificationDataSource,
    private val membershipLock: GroupMembershipLock,
    private val epochDataSource: GroupEpochDataSource
) {
    suspend fun initializeOwnedGroup(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            verificationDataSource.initializeOwnedGroup(groupId).getOrThrow()
        }

    suspend fun findBySourceInvitationId(invitationId: String): GroupMembershipAttemptDto? =
        membershipStore.findBySourceInvitationId(invitationId)?.toDto()

    suspend fun findOwnerAttempt(
        groupId: String,
        contactId: String
    ): GroupMembershipAttemptDto? =
        membershipStore
            .findByGroupContactAndPerspective(
                groupId = groupId,
                contactId = contactId,
                perspective = GroupMembershipPerspective.OWNER.name
            )?.toDto()

    suspend fun stageOwnerAttempt(
        groupId: String,
        contactId: String,
        sourceInvitationId: String,
        challenge: ByteArray,
        createdAtEpochMilliseconds: Long
    ): Result<GroupMembershipAttemptDto> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            require(sourceInvitationId.isNotBlank()) { "Source invitation ID must not be blank" }
            require(challenge.isNotEmpty()) { "Membership challenge must not be empty" }

            membershipLock.withLock {
                groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
                val currentContactIds =
                    epochDataSource
                        .findCurrentParticipants(groupId)
                        .mapTo(mutableSetOf()) { participant -> participant.contactId }
                check(contactId !in currentContactIds) { "Contact already belongs to this group" }

                val existing =
                    membershipStore.findByGroupContactAndPerspective(
                        groupId = groupId,
                        contactId = contactId,
                        perspective = GroupMembershipPerspective.OWNER.name
                    )
                check(existing == null || existing.status.canBeReplacedForFreshAttempt()) {
                    "Contact already has an active membership attempt"
                }

                val membership =
                    GroupMembershipEntity(
                        membershipId = IdGenerator.generate(prefix = "group-membership"),
                        sourceInvitationId = sourceInvitationId,
                        groupId = groupId,
                        contactId = contactId,
                        perspective = GroupMembershipPerspective.OWNER.name,
                        status = GroupMembershipStatus.STAGED.name,
                        challenge = challenge.copyOf(),
                        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                        updatedAtEpochMilliseconds = createdAtEpochMilliseconds
                    )
                membershipStore.replaceForGroupAndContact(membership)
                membership.toDto()
            }
        }

    suspend fun stageMemberAttempt(
        groupId: String,
        ownerContactId: String,
        sourceInvitationId: String,
        challenge: ByteArray,
        ownerEncryptionPublicKey: ByteArray,
        ownerSigningPublicKey: ByteArray,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Result<GroupMembershipAttemptDto> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(ownerContactId.isNotBlank()) { "Owner contact ID must not be blank" }
            require(sourceInvitationId.isNotBlank()) { "Source invitation ID must not be blank" }
            require(challenge.isNotEmpty()) { "Membership challenge must not be empty" }

            membershipLock.withLock {
                val membership =
                    GroupMembershipEntity(
                        membershipId = IdGenerator.generate(prefix = "group-membership"),
                        sourceInvitationId = sourceInvitationId,
                        groupId = groupId,
                        contactId = ownerContactId,
                        perspective = GroupMembershipPerspective.MEMBER.name,
                        status = GroupMembershipStatus.STAGED.name,
                        challenge = challenge.copyOf(),
                        ownerEncryptionPublicKey = ownerEncryptionPublicKey.copyOf(),
                        ownerSigningPublicKey = ownerSigningPublicKey.copyOf(),
                        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                        updatedAtEpochMilliseconds =
                            maxOf(
                                createdAtEpochMilliseconds,
                                updatedAtEpochMilliseconds
                            )
                    )
                membershipStore.replaceForGroupAndContact(membership)
                membership.toDto()
            }
        }

    suspend fun markJoinRequested(
        sourceInvitationId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<GroupMembershipAttemptDto> =
        transition(
            sourceInvitationId = sourceInvitationId,
            event = GroupMembershipEvent.JOIN_REQUESTED,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
        )

    suspend fun markJoinSendFailed(
        sourceInvitationId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<GroupMembershipAttemptDto> =
        transition(
            sourceInvitationId = sourceInvitationId,
            event = GroupMembershipEvent.JOIN_SEND_FAILED,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
        )

    suspend fun markIdentityConfirmed(
        sourceInvitationId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<GroupMembershipAttemptDto> =
        transition(
            sourceInvitationId = sourceInvitationId,
            event = GroupMembershipEvent.IDENTITY_CONFIRMED,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
        )

    suspend fun deleteAttempt(sourceInvitationId: String): Result<Unit> =
        runCatching {
            require(sourceInvitationId.isNotBlank()) { "Source invitation ID must not be blank" }
            membershipStore.deleteBySourceInvitationId(sourceInvitationId)
        }

    suspend fun deleteOwnerAttempt(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            membershipStore.deleteByGroupContactAndPerspective(
                groupId = groupId,
                contactId = contactId,
                perspective = GroupMembershipPerspective.OWNER.name
            )
        }

    suspend fun deleteSupersededMemberStagedAttempts(
        ownerContactId: String,
        currentInvitationId: String
    ): Result<Unit> =
        runCatching {
            membershipStore.deleteSupersededStagedMemberships(
                contactId = ownerContactId,
                currentInvitationId = currentInvitationId,
                perspective = GroupMembershipPerspective.MEMBER.name,
                stagedStatus = GroupMembershipStatus.STAGED.name
            )
        }

    suspend fun refreshOwnedMembership(groupId: String): Result<Unit> =
        verificationDataSource.onOwnedMembershipChanged(groupId)

    suspend fun clearRetiredMembershipBeforeRejoin(groupId: String): Result<Unit> =
        groupSecurityManager.clearRetiredMembershipBeforeRejoin(groupId)

    private suspend fun transition(
        sourceInvitationId: String,
        event: GroupMembershipEvent,
        updatedAtEpochMilliseconds: Long
    ): Result<GroupMembershipAttemptDto> =
        runCatching {
            membershipLock.withLock {
                val current =
                    requireNotNull(membershipStore.findBySourceInvitationId(sourceInvitationId)) {
                        "Group membership attempt was not found"
                    }
                val nextStatus = GroupMembershipStateMachine.transition(current.status, event)
                val updatedAt =
                    maxOf(
                        current.createdAtEpochMilliseconds,
                        updatedAtEpochMilliseconds
                    )
                val changed =
                    membershipStore.updateStatus(
                        membershipId = current.membershipId,
                        expectedStatus = current.status,
                        newStatus = nextStatus.name,
                        updatedAt = updatedAt
                    )
                check(changed == 1) { "Group membership changed while it was updated" }
                current.copy(status = nextStatus.name, updatedAtEpochMilliseconds = updatedAt).toDto()
            }
        }

    private fun String.canBeReplacedForFreshAttempt(): Boolean =
        this == GroupMembershipStatus.STAGED.name ||
            this == GroupMembershipStatus.FAILED.name ||
            this == GroupMembershipStatus.REMOVED.name ||
            this == GroupMembershipStatus.GROUP_DELETED.name
}

private fun GroupMembershipEntity.toDto(): GroupMembershipAttemptDto =
    GroupMembershipAttemptDto(
        membershipId = membershipId,
        sourceInvitationId = sourceInvitationId,
        groupId = groupId,
        contactId = contactId,
        perspective = GroupMembershipPerspective.valueOf(perspective),
        status = GroupMembershipStatus.valueOf(status),
        challenge = challenge.copyOf(),
        ownerEncryptionPublicKey = ownerEncryptionPublicKey?.copyOf(),
        ownerSigningPublicKey = ownerSigningPublicKey?.copyOf(),
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )
