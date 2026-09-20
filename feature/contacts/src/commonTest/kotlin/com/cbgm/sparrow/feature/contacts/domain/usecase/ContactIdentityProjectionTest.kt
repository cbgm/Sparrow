package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityReadRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveRemoteIdentitiesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ContactIdentityProjectionTest {
    @Test
    fun changingIdentityEmitsUpdatedContactWithoutChangingContactsTable() = runTest {
        val contacts = FakeContacts()
        val identities = FakeIdentities()
        val observe = ObserveContactsUseCase(
            repository = contacts,
            observeRemoteIdentities = ObserveRemoteIdentitiesUseCase(identities)
        )
        val observed = async(UnconfinedTestDispatcher(testScheduler)) {
            observe().take(3).toList()
        }
        runCurrent()
        identities.rows.value = listOf(identity(1))
        runCurrent()
        identities.rows.value = listOf(identity(2))
        val snapshots = observed.await()
        assertEquals(listOf("alice", "bob"), snapshots.first().map { it.id })
        assertNull(snapshots.first().first().sparrowIdentity)
        assertContentEquals(byteArrayOf(1), snapshots[1].first().sparrowIdentity?.encryptionPublicKey)
        assertNull(snapshots[1][1].sparrowIdentity)
        assertContentEquals(byteArrayOf(2), snapshots[2].first().sparrowIdentity?.encryptionPublicKey)
    }

    @Test
    fun singleContactReadObtainsIdentityFromIdentityOwner() = runTest {
        val getContact = GetContactUseCase(
            FakeContacts(),
            GetRemoteIdentityUseCase(
                FakeIdentities().apply {
                    rows.value = listOf(identity(3))
                }
            )
        )
        val alice = getContact("alice").getOrThrow()!!
        assertContentEquals(byteArrayOf(3), alice.sparrowIdentity?.encryptionPublicKey)
        assertNull(getContact("bob").getOrThrow()?.sparrowIdentity)
        assertNull(getContact("missing").getOrThrow())
    }

    private fun identity(seed: Int) = RemotePeerIdentity(
        peerId = "alice",
        encryptionPublicKey = byteArrayOf(seed.toByte()),
        signingPublicKey = byteArrayOf(8),
        verificationStatus = ContactVerificationStatus.VERIFIED,
        keyExchangeStatus = KeyExchangeStatus.MUTUAL,
        verifiedByContact = true,
        locallyImported = true,
        updatedAtEpochMilliseconds = seed.toLong()
    )

    private class FakeIdentities : RemoteIdentityReadRepository {
        val rows = MutableStateFlow<List<RemotePeerIdentity>>(emptyList())

        override fun observeAll(): Flow<List<RemotePeerIdentity>> = rows

        override suspend fun get(peerId: String): Result<RemotePeerIdentity?> =
            Result.success(rows.value.firstOrNull { it.peerId == peerId })

        override suspend fun findPeerIdBySigningPublicKey(signingPublicKey: ByteArray): Result<String?> =
            Result.success(rows.value.firstOrNull { it.signingPublicKey.contentEquals(signingPublicKey) }?.peerId)
    }

    private class FakeContacts : ContactRepository {
        private val contacts = MutableStateFlow(
            listOf("alice", "bob").map { id ->
                Contact(
                    id = id,
                    displayName = id,
                    phoneNumbers = emptyList(),
                    preferredPhoneNumberId = null,
                    deviceContactId = null,
                    deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED,
                    sparrowIdentity = null,
                    createdAtEpochMilliseconds = 1L,
                    updatedAtEpochMilliseconds = 1L
                )
            }
        )

        override fun observeContacts(): Flow<List<Contact>> = contacts

        override suspend fun getContact(contactId: String): Result<Contact?> =
            Result.success(contacts.value.firstOrNull { it.id == contactId })

        override suspend fun importDeviceContact(request: ImportDeviceContactRequest): Result<Contact> =
            error("Not used")

        override suspend fun upsertImportedContact(request: ImportContactRequest): Result<Contact> =
            error("Not used")

        override suspend fun usePhoneNumberAsDisplayNameWhenMissing(
            contactId: String,
            phoneNumber: String,
            updatedAtEpochMilliseconds: Long
        ): Result<Unit> = error("Not used")

        override suspend fun findOrCreateByPhoneNumber(phoneNumber: String): Result<Contact> = error("Not used")

        override suspend fun resolveAuthenticatedPeerContact(
            senderContactId: String?,
            phoneNumber: String?
        ): Result<String> = error("Not used")

        override suspend fun updateContactDetails(
            contactId: String,
            displayName: String?,
            phoneNumber: String?
        ): Result<Contact> = error("Not used")

        override suspend fun updateDeviceContactLinkStatus(
            deviceContactId: String,
            status: DeviceContactLinkStatus
        ): Result<Contact?> = error("Not used")
    }
}
