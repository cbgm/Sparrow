package com.cbgm.sparrow.feature.membership.data.repository

import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.datasource.GroupEpochSecurityDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupIncomingActivationDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupIncomingDeletionDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupIncomingRemovalDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupIncomingWelcomeDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipLifecycleDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipStoreDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupReadyAcknowledgementDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupSecurityStoreDataSource
import com.cbgm.sparrow.feature.membership.data.mapper.toDomain
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupLeaveRequirementDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupConversationMembershipSnapshot
import com.cbgm.sparrow.feature.membership.domain.model.GroupIncomingWelcomeAuthorization
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberLifecycleSnapshot
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberPromotionResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.GroupMessageMembershipAccess
import com.cbgm.sparrow.feature.membership.domain.model.GroupMetadataMessageSender
import com.cbgm.sparrow.feature.membership.domain.model.GroupMetadataSendContext
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMemberKey
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMembership
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationSecurityState
import com.cbgm.sparrow.feature.membership.domain.model.MembershipVerificationSnapshot
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformLatest

internal class GroupMembershipRepositoryImpl(
    private val securityStore: GroupSecurityStoreDataSource,
    private val epochSecurity: GroupEpochSecurityDataSource,
    private val incomingWelcome: GroupIncomingWelcomeDataSource,
    private val readyAcknowledgement: GroupReadyAcknowledgementDataSource,
    private val incomingActivation: GroupIncomingActivationDataSource,
    private val incomingDeletion: GroupIncomingDeletionDataSource,
    private val incomingRemoval: GroupIncomingRemovalDataSource,
    private val operations: GroupMembershipLifecycleDataSource,
    private val membershipStore: GroupMembershipStoreDataSource
) : GroupMembershipRepository {
    override suspend fun getVerificationMembershipContext(groupId: String): Result<GroupVerificationMembershipContext> =
        runCatching {
            val security = securityStore.findState(groupId)
            val keys = security?.let { state ->
                securityStore.findMemberKeys(groupId, state.currentEpoch)
            }.orEmpty()
            val memberships = membershipStore.findByGroupId(groupId)
            GroupVerificationMembershipContext(
                security = security?.let { state ->
                    GroupVerificationSecurityState(
                        ownerContactId = state.ownerContactId,
                        isLocalAdmin = state.localRole.isGroupAdminRole()
                    )
                },
                memberKeys = keys.map { key ->
                    GroupVerificationMemberKey(
                        contactId = key.contactId,
                        encryptionPublicKey = key.encryptionPublicKey.copyOf(),
                        signingPublicKey = key.signingPublicKey.copyOf(),
                        isAdmin = key.role.isGroupAdminRole()
                    )
                },
                memberships = memberships.map { member ->
                    GroupVerificationMembership(
                        contactId = member.contactId,
                        sourceInvitationId = member.sourceInvitationId,
                        updatedAtEpochMilliseconds = member.updatedAtEpochMilliseconds,
                        isOwner = member.perspective == GroupMembershipPerspective.OWNER.name,
                        isActive = member.status == GroupMembershipStatus.ACTIVE.name,
                        isVisiblePending = member.status in setOf(
                            GroupMembershipStatus.STAGED.name,
                            GroupMembershipStatus.IDENTITY_READY.name,
                            GroupMembershipStatus.WELCOME_SENT.name
                        )
                    )
                }
            )
        }

    override suspend fun authorizeIncomingWelcome(
        groupId: String,
        senderContactId: String,
        packetId: String,
        epoch: Int
    ): Result<GroupIncomingWelcomeAuthorization?> = runCatching {
        incomingWelcome.authorize(groupId, senderContactId, packetId, epoch)
    }

    override suspend fun sendGroupReadyAcknowledgement(
        groupId: String,
        epoch: Int,
        welcomePacketId: String,
        recipientContactId: String
    ): Result<Unit> = runCatching {
        readyAcknowledgement.send(groupId, epoch, welcomePacketId, recipientContactId)
    }

    override suspend fun completeIncomingWelcome(
        groupId: String,
        senderContactId: String,
        isFirstWelcome: Boolean,
        removedContactIds: Set<String>,
        persistedAt: Long
    ): Result<Unit> = runCatching {
        incomingWelcome.complete(groupId, senderContactId, isFirstWelcome, removedContactIds, persistedAt)
    }

    override suspend fun authorizeIncomingActivation(
        packet: GroupMemberActivatedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray,
        transportMode: String
    ): Result<Boolean> = runCatching {
        incomingActivation.authorize(packet, ownerContactId, ownerSigningPublicKey, transportMode)
    }

    override suspend fun applyIncomingActivation(
        packet: GroupMemberActivatedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray,
        transportMode: String,
        memberContactId: String?,
        receivedAtEpochMilliseconds: Long
    ): Result<Boolean> = runCatching {
        incomingActivation.apply(
            packet,
            ownerContactId,
            ownerSigningPublicKey,
            transportMode,
            memberContactId,
            receivedAtEpochMilliseconds
        )
    }

    override suspend fun authorizeIncomingGroupRemoval(
        packet: GroupMemberRemovedPacket,
        senderContactId: String,
        pendingOwnerSigningPublicKey: ByteArray?
    ): Result<Boolean> = runCatching {
        incomingRemoval.authorize(packet, senderContactId, pendingOwnerSigningPublicKey)
    }

    override suspend fun completeIncomingGroupRemoval(packet: GroupMemberRemovedPacket): Result<Unit> =
        runCatching { incomingRemoval.complete(packet) }

    override suspend fun authorizeIncomingGroupDeletion(
        packet: GroupConversationDeletedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray
    ): Result<Unit> = runCatching {
        incomingDeletion.authorize(packet, ownerContactId, ownerSigningPublicKey)
    }

    override suspend fun completeIncomingGroupDeletion(
        packet: GroupConversationDeletedPacket,
        ownerContactId: String
    ): Result<Unit> = runCatching {
        incomingDeletion.complete(packet, ownerContactId)
    }

    override suspend fun receiveReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = operations.receiveReadyAcknowledgement(memberContactId, packet, receivedAtEpochMilliseconds)

    override suspend fun receiveMemberActivationAcknowledgement(
        packet: GroupMemberActivationAcknowledgementPacket,
        acknowledgingContactId: String,
        transportMode: String
    ): Result<Unit> = operations.receiveMemberActivationAcknowledgement(packet, acknowledgingContactId, transportMode)

    override suspend fun receiveLeaveRequest(
        memberContactId: String,
        packet: GroupLeaveRequestPacket,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> = operations.receiveLeaveRequest(memberContactId, packet, context)

    override suspend fun resolveMetadataMessageSender(
        groupId: String,
        epoch: Int,
        signingPublicKey: ByteArray
    ): Result<GroupMetadataMessageSender> = runCatching {
        val state = securityStore.findState(groupId) ?: error("Group security state was not found")
        val isLocal = state.localSigningPublicKey.contentEquals(signingPublicKey)
        GroupMetadataMessageSender(
            isLocal = isLocal,
            memberContactId =
                if (isLocal) {
                    null
                } else {
                    securityStore.findMemberKeys(groupId, epoch)
                        .firstOrNull { it.signingPublicKey.contentEquals(signingPublicKey) }
                        ?.contactId
                }
        )
    }

    override suspend fun resolvePinnedSenderSigningKey(
        groupId: String,
        isMine: Boolean,
        senderContactId: String?
    ): Result<ByteArray?> = runCatching {
        val state = securityStore.findState(groupId) ?: error("Group security state was not found")
        if (isMine) {
            state.localSigningPublicKey.copyOf()
        } else {
            val contactId = requireNotNull(senderContactId) {
                "Incoming group message has no sender contact"
            }
            securityStore.findMemberKeys(groupId, state.currentEpoch)
                .firstOrNull { member -> member.contactId == contactId }
                ?.signingPublicKey
                ?.copyOf()
        }
    }

    override suspend fun verifyGroupKeyConfirmation(
        groupId: String,
        epoch: Int,
        confirmation: ByteArray
    ): Result<Unit> = runCatching {
        epochSecurity.verifyKeyConfirmation(groupId, epoch, confirmation)
    }

    override suspend fun getCurrentEpoch(groupId: String): Result<Int> = runCatching {
        securityStore.findState(groupId)?.currentEpoch
            ?: error("Group security state was not found")
    }

    override suspend fun inspectMessageAccess(groupId: String): Result<GroupMessageMembershipAccess> = runCatching {
        val memberships = membershipStore.findByGroupId(groupId)
        GroupMessageMembershipAccess(
            isJoinPending = memberships.any { member ->
                member.perspective == GroupMembershipPerspective.MEMBER.name &&
                    member.status in setOf(
                        GroupMembershipStatus.STAGED.name,
                        GroupMembershipStatus.JOIN_REQUEST_SENT.name,
                        GroupMembershipStatus.WAITING_FOR_ACTIVATION.name
                    )
            },
            isLeavePending = memberships.any { it.status == GroupMembershipStatus.LEAVE_REQUESTED.name },
            isDeleted = memberships.any { it.status == GroupMembershipStatus.GROUP_DELETED.name },
            hasCurrentMembership = memberships.any {
                it.status != GroupMembershipStatus.REMOVED.name &&
                    it.status != GroupMembershipStatus.GROUP_DELETED.name
            }
        )
    }

    override suspend fun wasGroupDeleted(groupId: String): Result<Boolean> = runCatching {
        membershipStore.findByGroupId(groupId).any {
            it.status == GroupMembershipStatus.GROUP_DELETED.name
        }
    }

    override suspend fun authorizeMetadataSend(
        groupId: String,
        localSigningPublicKey: ByteArray,
        action: String
    ): Result<GroupMetadataSendContext> = runCatching {
        val state = securityStore.findState(groupId) ?: error("Group security state was not found")
        check(state.localRole.isGroupAdminRole()) { "Only a group admin may change the $action" }
        check(state.localSigningPublicKey.contentEquals(localSigningPublicKey)) {
            "Local admin signing key does not match the group security state"
        }
        GroupMetadataSendContext(
            epoch = state.currentEpoch,
            recipientContactIds = securityStore.findMemberKeys(groupId, state.currentEpoch)
                .asSequence()
                .filterNot { it.signingPublicKey.contentEquals(localSigningPublicKey) }
                .map { it.contactId }
                .filter(String::isNotBlank)
                .toSet()
        )
    }

    override suspend fun authorizeMetadataReceive(
        groupId: String,
        epoch: Int,
        contactId: String,
        adminSigningPublicKey: ByteArray,
        action: String
    ): Result<Boolean> = runCatching {
        val state = securityStore.findState(groupId) ?: error("Group security state was not found")
        if (epoch < state.currentEpoch) return@runCatching false
        check(epoch == state.currentEpoch) { "Group $action update belongs to a future group epoch" }
        check(
            securityStore.findMemberKeys(groupId, epoch).any { member ->
                member.contactId == contactId &&
                    member.role.isGroupAdminRole() &&
                    member.signingPublicKey.contentEquals(adminSigningPublicKey)
            }
        ) { "Group $action update was not signed by an active group admin" }
        true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAdministration(groupId: String): Flow<GroupAdministrationState> =
        combine(
            securityStore.observeState(groupId),
            securityStore.observeCurrentMemberKeys(groupId)
        ) { securityState, memberKeys ->
            securityState to memberKeys
        }.transformLatest { (securityState, memberKeys) ->
            if (securityState == null || securityState.localRole == GROUP_LEFT_ROLE) {
                emit(GroupAdministrationState())
                return@transformLatest
            }

            val currentMembers = memberKeys.mapTo(mutableSetOf()) { memberKey -> memberKey.contactId }
            val currentAdmins =
                memberKeys
                    .filter { memberKey -> memberKey.role.isGroupAdminRole() }
                    .mapTo(mutableSetOf()) { memberKey -> memberKey.contactId }

            val localIsAdmin = securityState.localRole.isGroupAdminRole()
            val leaveRequirement =
                GroupMembershipStateMachine.leaveRequirement(
                    isLocalAdmin = localIsAdmin,
                    currentMemberContactIds = currentMembers,
                    currentAdminContactIds = currentAdmins
                )
            emit(
                GroupAdministrationState(
                    isLocalAdmin = localIsAdmin,
                    adminContactIds = currentAdmins,
                    currentMemberContactIds = currentMembers,
                    promotableContactIds =
                        currentMembers.filterTo(mutableSetOf()) { contactId ->
                            contactId !in currentAdmins
                        },
                    requiresPromotionBeforeLeave =
                        leaveRequirement is GroupLeaveRequirementDto.PromoteAdminFirst,
                    activeMemberCount = currentMembers.size + 1
                )
            )
        }

    override fun observeConversationMembership(groupId: String): Flow<GroupConversationMembershipSnapshot> =
        combine(
            securityStore.observeCurrentMemberKeys(groupId),
            membershipStore.observeByGroupId(groupId)
        ) { currentMemberKeys, memberships ->
            GroupConversationMembershipSnapshot(
                participantContactIds = currentMemberKeys.map { it.contactId },
                memberships = memberships.map { membership ->
                    GroupMemberLifecycleSnapshot(
                        contactId = membership.contactId,
                        sourceInvitationId = membership.sourceInvitationId,
                        status = membership.status,
                        createdAtEpochMilliseconds = membership.createdAtEpochMilliseconds
                    )
                }
            )
        }

    override fun observeVerificationSnapshot(groupId: String): Flow<MembershipVerificationSnapshot> =
        combine(
            securityStore.observeState(groupId),
            membershipStore.observeByGroupId(groupId)
        ) { securityState, memberships ->
            val isSecurityAdmin = securityState?.localRole?.isGroupAdminRole() == true
            val member = memberships.singleOrNull { membership ->
                membership.perspective == GroupMembershipPerspective.MEMBER.name
            }
            MembershipVerificationSnapshot(
                hasSecurityState = securityState != null,
                isSecurityAdmin = isSecurityAdmin,
                isSecurityMemberActive = securityState != null && securityState.localRole != GROUP_LEFT_ROLE,
                hasOwnerMembership = memberships.any { membership ->
                    membership.perspective == GroupMembershipPerspective.OWNER.name
                },
                securityOwnerContactId = securityState?.ownerContactId,
                memberContactId = member?.contactId,
                memberInvitationId = member?.sourceInvitationId,
                isMemberLeavePending = member?.status == GroupMembershipStatus.LEAVE_REQUESTED.name
            )
        }

    override suspend fun removeMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> = operations.removeMember(groupId, contactId, context)

    override suspend fun promoteMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberPromotionResult> = operations.promoteMember(groupId, contactId, context)

    override suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEnd> = operations.transferAdminAndLeave(groupId, contactId, context).map { it.toDomain() }

    override suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            val securityState = securityStore.findState(groupId)
            if (securityState == null || !securityState.localRole.isGroupAdminRole()) {
                return@runCatching GroupLeaveRequirement.CanLeave
            }

            val memberKeys =
                securityStore.findMemberKeys(
                    groupId = groupId,
                    epoch = securityState.currentEpoch
                )
            val currentMembers = memberKeys.mapTo(mutableSetOf()) { memberKey -> memberKey.contactId }
            val currentAdmins =
                memberKeys
                    .filter { memberKey -> memberKey.role.isGroupAdminRole() }
                    .mapTo(mutableSetOf()) { memberKey -> memberKey.contactId }

            GroupMembershipStateMachine
                .leaveRequirement(
                    isLocalAdmin = true,
                    currentMemberContactIds = currentMembers,
                    currentAdminContactIds = currentAdmins
                ).toDomain()
        }

    override suspend fun leave(
        groupId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEnd> = operations.leaveGroup(groupId, context).map { it.toDomain() }

    override suspend fun delete(
        groupId: String,
        context: GroupMembershipContext
    ): Result<Long> = operations.deleteGroupConversation(groupId, context)
}
