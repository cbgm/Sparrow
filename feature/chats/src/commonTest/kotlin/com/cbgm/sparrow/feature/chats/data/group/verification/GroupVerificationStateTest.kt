package com.cbgm.sparrow.feature.chats.data.group.verification

import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.feature.chats.runtime.group.verification.GroupVerificationState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.GroupVerificationInputsUseCase
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityReadRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupConversationMembershipSnapshot
import com.cbgm.sparrow.feature.membership.domain.model.GroupIncomingWelcomeAuthorization
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberPromotionResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.GroupMessageMembershipAccess
import com.cbgm.sparrow.feature.membership.domain.model.GroupMetadataMessageSender
import com.cbgm.sparrow.feature.membership.domain.model.GroupMetadataSendContext
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMemberKey
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMembership
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.MembershipVerificationSnapshot
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupVerificationStateTest {
    @Test
    fun stagedMembershipIsExposedImmediatelyAsPendingVerificationMember() =
        runTest {
            val membershipRepository = FakeGroupMembershipRepository()
            val verificationDao = FakeGroupVerificationDao()
            val state =
                GroupVerificationState(
                    verificationDataSource = GroupVerificationDataSource(verificationDao),
                    verificationInputs = GroupVerificationInputsUseCase(
                        membershipRepository = membershipRepository,
                        contacts = FakeContactRepository(),
                        getRemoteIdentity = GetRemoteIdentityUseCase(FakeRemoteIdentityRepository())
                    )
                )
            membershipRepository.memberships = listOf(ownerMembership())

            state.refreshOwnedState(GROUP_ID)

            assertEquals(1, verificationDao.rows.size)
            assertEquals(CONTACT_ID, verificationDao.rows.single().contactId)
            assertEquals(
                GroupVerificationPairEntity.PENDING_STATUS,
                verificationDao.rows.single().membershipStatus
            )
            // Fake Contacts returns no identity; pending keys must originate from Identity.
            assertEquals(listOf<Byte>(1), verificationDao.rows.single().participantEncryptionPublicKey?.toList())
            assertEquals(listOf<Byte>(2), verificationDao.rows.single().participantSigningPublicKey?.toList())
        }

    @Test
    fun memberPerspectiveMembershipIsNotTurnedIntoOwnedPendingMember() =
        runTest {
            val membershipRepository = FakeGroupMembershipRepository()
            val verificationDao = FakeGroupVerificationDao()
            val state =
                GroupVerificationState(
                    verificationDataSource = GroupVerificationDataSource(verificationDao),
                    verificationInputs = GroupVerificationInputsUseCase(
                        membershipRepository = membershipRepository,
                        contacts = FakeContactRepository(),
                        getRemoteIdentity = GetRemoteIdentityUseCase(FakeRemoteIdentityRepository())
                    )
                )
            membershipRepository.memberships = listOf(ownerMembership().copy(isOwner = false, isActive = true))

            state.refreshOwnedState(GROUP_ID)

            assertEquals(emptyList(), verificationDao.rows)
        }

    @Test
    fun activeGroupKeysAlwaysComeFromCurrentMembershipEpoch() = runTest {
        val memberships = FakeGroupMembershipRepository().apply {
            this.memberships = listOf(ownerMembership().copy(isActive = true, isVisiblePending = false))
            this.memberKeys = listOf(
                GroupVerificationMemberKey(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = byteArrayOf(3),
                    signingPublicKey = byteArrayOf(4),
                    isAdmin = false
                )
            )
        }
        val dao = FakeGroupVerificationDao()
        val identityRepository = FakeRemoteIdentityRepository()
        val state = GroupVerificationState(
            GroupVerificationDataSource(dao),
            GroupVerificationInputsUseCase(
                memberships,
                FakeContactRepository(),
                GetRemoteIdentityUseCase(identityRepository)
            )
        )
        state.refreshOwnedState(GROUP_ID)
        assertEquals(GroupVerificationPairEntity.ACTIVE_STATUS, dao.rows.single().membershipStatus)
        assertEquals(listOf<Byte>(3), dao.rows.single().participantEncryptionPublicKey?.toList())
        assertEquals(listOf<Byte>(4), dao.rows.single().participantSigningPublicKey?.toList())
        assertEquals(
            0,
            identityRepository.identityReads,
            "Active members must use keys authenticated by Membership, not Identity's pending key cache"
        )
    }

    private class FakeRemoteIdentityRepository : RemoteIdentityReadRepository {
        var identityReads: Int = 0

        override suspend fun get(peerId: String): Result<RemotePeerIdentity?> {
            identityReads++
            return Result.success(
                RemotePeerIdentity(
                    peerId = peerId,
                    encryptionPublicKey = byteArrayOf(1),
                    signingPublicKey = byteArrayOf(2),
                    verificationStatus = ContactVerificationStatus.UNVERIFIED,
                    keyExchangeStatus = KeyExchangeStatus.ONE_WAY,
                    verifiedByContact = false,
                    locallyImported = true,
                    updatedAtEpochMilliseconds = 1L
                )
            )
        }

        override suspend fun findPeerIdBySigningPublicKey(signingPublicKey: ByteArray): Result<String?> =
            Result.success(null)

        override fun observeAll(): Flow<List<RemotePeerIdentity>> = emptyFlow()
    }

    private class FakeGroupMembershipRepository : GroupMembershipRepository {
        var memberships: List<GroupVerificationMembership> = emptyList()
        var memberKeys: List<GroupVerificationMemberKey> = emptyList()

        override suspend fun getVerificationMembershipContext(groupId: String): Result<GroupVerificationMembershipContext> =
            Result.success(
                GroupVerificationMembershipContext(
                    security = null,
                    memberKeys = memberKeys,
                    memberships = memberships
                )
            )

        override suspend fun authorizeIncomingWelcome(
            groupId: String,
            senderContactId: String,
            packetId: String,
            epoch: Int
        ): Result<GroupIncomingWelcomeAuthorization?> = error("Unused in verification projection tests")

        override suspend fun sendGroupReadyAcknowledgement(
            groupId: String,
            epoch: Int,
            welcomePacketId: String,
            recipientContactId: String
        ): Result<Unit> = error("Unused in verification projection tests")

        override suspend fun completeIncomingWelcome(
            groupId: String,
            senderContactId: String,
            isFirstWelcome: Boolean,
            removedContactIds: Set<String>,
            persistedAt: Long
        ): Result<Unit> = error("Unused in verification projection tests")

        override suspend fun authorizeIncomingActivation(
            packet: GroupMemberActivatedPacket,
            ownerContactId: String,
            ownerSigningPublicKey: ByteArray,
            transportMode: String
        ): Result<Boolean> = error("Unused in verification projection tests")

        override suspend fun applyIncomingActivation(
            packet: GroupMemberActivatedPacket,
            ownerContactId: String,
            ownerSigningPublicKey: ByteArray,
            transportMode: String,
            memberContactId: String?,
            receivedAtEpochMilliseconds: Long
        ): Result<Boolean> = error("Unused in verification projection tests")

        override suspend fun authorizeIncomingGroupRemoval(
            packet: GroupMemberRemovedPacket,
            senderContactId: String,
            pendingOwnerSigningPublicKey: ByteArray?
        ): Result<Boolean> = error("Unused in verification projection tests")

        override suspend fun completeIncomingGroupRemoval(packet: GroupMemberRemovedPacket): Result<Unit> = error("Unused in verification projection tests")

        override suspend fun authorizeIncomingGroupDeletion(
            packet: GroupConversationDeletedPacket,
            ownerContactId: String,
            ownerSigningPublicKey: ByteArray
        ): Result<Unit> = error("Unused in verification projection tests")

        override suspend fun completeIncomingGroupDeletion(
            packet: GroupConversationDeletedPacket,
            ownerContactId: String
        ): Result<Unit> = error("Unused in verification projection tests")

        override suspend fun receiveReadyAcknowledgement(
            memberContactId: String,
            packet: GroupReadyAcknowledgementPacket,
            receivedAtEpochMilliseconds: Long
        ): Result<Unit> = error("Unused in verification projection tests")

        override suspend fun receiveMemberActivationAcknowledgement(
            packet: GroupMemberActivationAcknowledgementPacket,
            acknowledgingContactId: String,
            transportMode: String
        ): Result<Unit> = error("Unused in verification projection tests")

        override suspend fun receiveLeaveRequest(
            memberContactId: String,
            packet: GroupLeaveRequestPacket,
            context: GroupMembershipContext
        ): Result<GroupMemberRemovalResult> = error("Unused in verification projection tests")

        override suspend fun resolveMetadataMessageSender(
            groupId: String,
            epoch: Int,
            signingPublicKey: ByteArray
        ): Result<GroupMetadataMessageSender> = error("Unused in verification projection tests")

        override suspend fun wasGroupDeleted(groupId: String): Result<Boolean> = error("Unused in verification projection tests")

        override suspend fun inspectMessageAccess(groupId: String): Result<GroupMessageMembershipAccess> = error("Unused in verification projection tests")

        override suspend fun resolvePinnedSenderSigningKey(
            groupId: String,
            isMine: Boolean,
            senderContactId: String?
        ): Result<ByteArray?> = error("Unused in verification projection tests")

        override suspend fun getCurrentEpoch(groupId: String): Result<Int> = error("Unused in verification projection tests")

        override suspend fun verifyGroupKeyConfirmation(
            groupId: String,
            epoch: Int,
            confirmation: ByteArray
        ): Result<Unit> = error("Unused in verification projection tests")

        override suspend fun authorizeMetadataSend(
            groupId: String,
            localSigningPublicKey: ByteArray,
            action: String
        ): Result<GroupMetadataSendContext> = error("Unused in verification projection tests")

        override suspend fun authorizeMetadataReceive(
            groupId: String,
            epoch: Int,
            contactId: String,
            adminSigningPublicKey: ByteArray,
            action: String
        ): Result<Boolean> = error("Unused in verification projection tests")

        override fun observeAdministration(groupId: String): Flow<GroupAdministrationState> = error("Unused in verification projection tests")

        override fun observeConversationMembership(groupId: String): Flow<GroupConversationMembershipSnapshot> = error("Unused in verification projection tests")

        override fun observeVerificationSnapshot(groupId: String): Flow<MembershipVerificationSnapshot> = error("Unused in verification projection tests")

        override suspend fun removeMember(
            groupId: String,
            contactId: String,
            context: GroupMembershipContext
        ): Result<GroupMemberRemovalResult> = error("Unused in verification projection tests")

        override suspend fun promoteMember(
            groupId: String,
            contactId: String,
            context: GroupMembershipContext
        ): Result<GroupMemberPromotionResult> = error("Unused in verification projection tests")

        override suspend fun transferAdminAndLeave(
            groupId: String,
            contactId: String,
            context: GroupMembershipContext
        ): Result<GroupLocalMembershipEnd> = error("Unused in verification projection tests")

        override suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement> = error("Unused in verification projection tests")

        override suspend fun leave(
            groupId: String,
            context: GroupMembershipContext
        ): Result<GroupLocalMembershipEnd> = error("Unused in verification projection tests")

        override suspend fun delete(
            groupId: String,
            context: GroupMembershipContext
        ): Result<Long> = error("Unused in verification projection tests")
    }

    private fun ownerMembership() = GroupVerificationMembership(
        contactId = CONTACT_ID,
        sourceInvitationId = "invite-1",
        updatedAtEpochMilliseconds = 1L,
        isOwner = true,
        isActive = false,
        isVisiblePending = true
    )

    private class FakeGroupVerificationDao : GroupVerificationDao {
        var rows: List<GroupVerificationPairEntity> = emptyList()

        override suspend fun findLatestUpdatedAt(groupId: String): Long? = null

        override fun observeByGroupId(groupId: String): Flow<List<GroupVerificationPairEntity>> = emptyFlow()

        override suspend fun findByGroupId(groupId: String): List<GroupVerificationPairEntity> = rows

        override suspend fun findPair(groupId: String, invitationId: String): GroupVerificationPairEntity? = null

        override suspend fun upsertAll(rows: List<GroupVerificationPairEntity>) {
            this.rows = rows
        }

        override suspend fun deleteByGroupId(groupId: String) {
            rows = emptyList()
        }

        override suspend fun markAdminVerifiedParticipant(groupId: String, invitationId: String, updatedAt: Long): Int = 0

        override suspend fun markParticipantVerifiedAdmin(groupId: String, invitationId: String, updatedAt: Long): Int = 0
    }

    private class FakeContactRepository : ContactRepository {
        override suspend fun getContact(contactId: String): Result<Contact?> = Result.success(contact())

        override suspend fun importDeviceContact(request: ImportDeviceContactRequest): Result<Contact> = unused()

        override suspend fun upsertImportedContact(request: ImportContactRequest): Result<Contact> = unused()

        override suspend fun usePhoneNumberAsDisplayNameWhenMissing(
            contactId: String,
            phoneNumber: String,
            updatedAtEpochMilliseconds: Long
        ): Result<Unit> = unused()

        override suspend fun findOrCreateByPhoneNumber(phoneNumber: String): Result<Contact> = unused()

        override suspend fun resolveAuthenticatedPeerContact(
            senderContactId: String?,
            phoneNumber: String?
        ): Result<String> = unused()

        override fun observeContacts(): Flow<List<Contact>> = emptyFlow()

        override suspend fun updateContactDetails(contactId: String, displayName: String?, phoneNumber: String?): Result<Contact> = unused()

        override suspend fun updateDeviceContactLinkStatus(
            deviceContactId: String,
            status: DeviceContactLinkStatus
        ): Result<Contact?> = unused()

        private fun contact() =
            Contact(
                id = CONTACT_ID,
                displayName = "Member",
                phoneNumbers = emptyList(),
                preferredPhoneNumberId = null,
                deviceContactId = null,
                deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED,
                sparrowIdentity = null,
                createdAtEpochMilliseconds = 1L,
                updatedAtEpochMilliseconds = 1L
            )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val CONTACT_ID = "contact-1"

        fun unused(): Nothing = error("Unused test operation")
    }
}
