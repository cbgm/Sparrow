package com.cbgm.sparrow.feature.chats.data.group.verification

import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.data.database.entity.GroupSecurityStateEntity
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupVerificationStateTest {
    @Test
    fun stagedMembershipIsExposedImmediatelyAsPendingVerificationMember() =
        runTest {
            val membershipDao = FakeGroupMembershipDao()
            val verificationDao = FakeGroupVerificationDao()
            val state =
                GroupVerificationState(
                    groupVerificationDao = verificationDao,
                    groupMembershipDao = membershipDao,
                    groupSecurityDao = EmptyGroupSecurityDao(),
                    getContact = GetContactUseCase(FakeContactRepository())
                )
            membershipDao.memberships = listOf(ownerMembership())

            state.refreshOwnedState(GROUP_ID)

            assertEquals(1, verificationDao.rows.size)
            assertEquals(CONTACT_ID, verificationDao.rows.single().contactId)
            assertEquals(
                GroupVerificationPairEntity.PENDING_STATUS,
                verificationDao.rows.single().membershipStatus
            )
        }

    @Test
    fun memberPerspectiveMembershipIsNotTurnedIntoOwnedPendingMember() =
        runTest {
            val membershipDao = FakeGroupMembershipDao()
            val verificationDao = FakeGroupVerificationDao()
            val state =
                GroupVerificationState(
                    groupVerificationDao = verificationDao,
                    groupMembershipDao = membershipDao,
                    groupSecurityDao = EmptyGroupSecurityDao(),
                    getContact = GetContactUseCase(FakeContactRepository())
                )
            membershipDao.memberships =
                listOf(
                    ownerMembership().copy(
                        perspective = GroupMembershipPerspective.MEMBER.name,
                        status = GroupMembershipStatus.ACTIVE.name
                    )
                )

            state.refreshOwnedState(GROUP_ID)

            assertEquals(emptyList(), verificationDao.rows)
        }

    private fun ownerMembership() =
        GroupMembershipEntity(
            membershipId = "membership-1",
            sourceInvitationId = "invite-1",
            groupId = GROUP_ID,
            contactId = CONTACT_ID,
            perspective = GroupMembershipPerspective.OWNER.name,
            status = GroupMembershipStatus.STAGED.name,
            challenge = byteArrayOf(1),
            createdAtEpochMilliseconds = 1L,
            updatedAtEpochMilliseconds = 1L
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

    private class FakeGroupMembershipDao : GroupMembershipDao {
        var memberships: List<GroupMembershipEntity> = emptyList()

        override suspend fun upsert(membership: GroupMembershipEntity) = unused()

        override suspend fun upsertAll(memberships: List<GroupMembershipEntity>) = unused()

        override suspend fun findBySourceInvitationId(invitationId: String): GroupMembershipEntity? = null

        override suspend fun findByGroupAndContact(groupId: String, contactId: String): GroupMembershipEntity? = null

        override suspend fun findByGroupContactAndPerspective(
            groupId: String,
            contactId: String,
            perspective: String
        ): GroupMembershipEntity? = null

        override suspend fun findByGroupId(groupId: String): List<GroupMembershipEntity> = memberships

        override suspend fun deleteByGroupAndContact(groupId: String, contactId: String) = unused()

        override suspend fun deleteByGroupContactAndPerspective(
            groupId: String,
            contactId: String,
            perspective: String
        ) = unused()

        override suspend fun deleteByGroupId(groupId: String) = unused()

        override fun observeByGroupId(groupId: String): Flow<List<GroupMembershipEntity>> = emptyFlow()

        override fun observeByPerspective(perspective: String): Flow<List<GroupMembershipEntity>> = emptyFlow()

        override fun observeAll(): Flow<List<GroupMembershipEntity>> = emptyFlow()

        override suspend fun findByMembershipId(membershipId: String): GroupMembershipEntity? = null

        override suspend fun deleteByMembershipId(membershipId: String): Int = 0

        override suspend fun deleteBySourceInvitationId(invitationId: String): Int = 0

        override suspend fun updateStatus(
            membershipId: String,
            expectedStatus: String,
            newStatus: String,
            updatedAt: Long
        ): Int = 0

        override suspend fun deleteSupersededStagedMemberships(
            contactId: String,
            currentInvitationId: String,
            perspective: String,
            stagedStatus: String
        ): Int = 0

        override suspend fun markGroupActive(groupId: String, readyStatus: String, activeStatus: String, updatedAt: Long): Int = 0
    }

    private class EmptyGroupSecurityDao : GroupSecurityDao {
        override suspend fun upsertState(state: GroupSecurityStateEntity) = unused()

        override suspend fun upsertMemberKeys(memberKeys: List<GroupMemberKeyEntity>) = unused()

        override suspend fun findState(groupId: String): GroupSecurityStateEntity? = null

        override fun observeState(groupId: String): Flow<GroupSecurityStateEntity?> = emptyFlow()

        override suspend fun deleteState(groupId: String) = unused()

        override suspend fun deleteMemberKeys(groupId: String) = unused()

        override suspend fun findMemberKey(groupId: String, epoch: Int, contactId: String): GroupMemberKeyEntity? = null

        override suspend fun findLatestMemberKey(groupId: String, contactId: String): GroupMemberKeyEntity? = null

        override suspend fun findMemberKeys(groupId: String, epoch: Int): List<GroupMemberKeyEntity> = emptyList()

        override fun observeCurrentMemberKeys(groupId: String): Flow<List<GroupMemberKeyEntity>> = emptyFlow()

        override suspend fun findAllCurrentMemberKeys(): List<GroupMemberKeyEntity> = emptyList()

        override suspend fun updateLocalRole(groupId: String, role: String, updatedAtEpochMilliseconds: Long): Int = 0
    }

    private class FakeContactRepository : ContactRepository {
        override suspend fun getContact(contactId: String): Result<Contact?> = Result.success(contact())

        override suspend fun importDeviceContact(request: ImportDeviceContactRequest): Result<Contact> = unused()

        override suspend fun importContact(request: ImportContactRequest): Result<Contact> = unused()

        override suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): Result<Contact?> = unused()

        override suspend fun findOrCreateByPhoneNumber(phoneNumber: String): Result<Contact> = unused()

        override fun observeContacts(): Flow<List<Contact>> = emptyFlow()

        override suspend fun updateContactDetails(contactId: String, displayName: String?, phoneNumber: String?): Result<Contact> = unused()

        override suspend fun markVerified(contactId: String): Result<Contact> = unused()

        override suspend fun markKeyExchangeMutual(contactId: String): Result<Contact> = unused()

        override suspend fun resetKeyExchange(contactId: String): Result<Contact> = unused()

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
