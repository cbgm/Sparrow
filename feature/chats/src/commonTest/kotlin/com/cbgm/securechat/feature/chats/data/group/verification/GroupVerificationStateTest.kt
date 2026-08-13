package com.cbgm.securechat.feature.chats.data.group.verification

import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.data.database.dao.GroupVerificationDao
import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.data.database.entity.GroupSecurityStateEntity
import com.cbgm.securechat.data.database.entity.GroupVerificationPairEntity
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationDirection
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupVerificationStateTest {
    @Test
    fun ownerInitializationKeepsPendingInvitationsBeforeSecurityEpochExists() =
        runTest {
            val invitationDao = FakeGroupInvitationDao()
            val verificationDao = FakeGroupVerificationDao()
            val state =
                GroupVerificationState(
                    groupVerificationDao = verificationDao,
                    groupInvitationDao = invitationDao,
                    groupSecurityDao = EmptyGroupSecurityDao(),
                    getContact = GetContactUseCase(FakeContactRepository())
                )
            invitationDao.invitations = listOf(outgoingInvitation())

            state.refreshOwnedState(GROUP_ID)

            val row = verificationDao.rows.single()
            assertEquals(CONTACT_ID, row.contactId)
            assertEquals(GroupVerificationPairEntity.PENDING_STATUS, row.membershipStatus)
        }

    @Test
    fun historicalIncomingInvitationIsNotTurnedIntoPendingMember() =
        runTest {
            val invitationDao = FakeGroupInvitationDao()
            val verificationDao = FakeGroupVerificationDao()
            val state =
                GroupVerificationState(
                    groupVerificationDao = verificationDao,
                    groupInvitationDao = invitationDao,
                    groupSecurityDao = EmptyGroupSecurityDao(),
                    getContact = GetContactUseCase(FakeContactRepository())
                )
            invitationDao.invitations =
                listOf(
                    outgoingInvitation().copy(
                        direction = GroupInvitationDirection.INCOMING.name,
                        status = GroupInvitationStatus.ACTIVE.name
                    )
                )

            state.refreshOwnedState(GROUP_ID)

            assertEquals(emptyList(), verificationDao.rows)
        }

    private fun outgoingInvitation() =
        GroupInvitationEntity(
            invitationId = "invite-1",
            groupId = GROUP_ID,
            contactId = CONTACT_ID,
            direction = GroupInvitationDirection.OUTGOING.name,
            status = GroupInvitationStatus.INVITE_SENT.name,
            challenge = byteArrayOf(1),
            createdAtEpochMilliseconds = 1L,
            expiresAtEpochMilliseconds = 2L,
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

    private class FakeGroupInvitationDao : GroupInvitationDao {
        var invitations: List<GroupInvitationEntity> = emptyList()

        override suspend fun upsert(invitation: GroupInvitationEntity) = unused()

        override suspend fun upsertAll(invitations: List<GroupInvitationEntity>) = unused()

        override suspend fun findByInvitationId(invitationId: String): GroupInvitationEntity? = null

        override suspend fun findByGroupAndContact(groupId: String, contactId: String): GroupInvitationEntity? = null

        override suspend fun findByGroupContactAndDirection(
            groupId: String,
            contactId: String,
            direction: String
        ): GroupInvitationEntity? = null

        override suspend fun findByGroupId(groupId: String): List<GroupInvitationEntity> = invitations

        override suspend fun deleteByGroupAndContact(groupId: String, contactId: String) = unused()

        override suspend fun deleteByGroupContactAndDirection(
            groupId: String,
            contactId: String,
            direction: String
        ) = unused()

        override suspend fun deleteByGroupId(groupId: String) = unused()

        override fun observeByGroupId(groupId: String): Flow<List<GroupInvitationEntity>> = emptyFlow()

        override suspend fun updateStatus(invitationId: String, expectedStatus: String, newStatus: String, updatedAt: Long): Int = 0

        override suspend fun failSupersededIncomingInvitations(
            contactId: String,
            currentInvitationId: String,
            awaitingAcceptanceStatus: String,
            failedStatus: String,
            updatedAt: Long
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
                secureChatIdentity = null,
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
