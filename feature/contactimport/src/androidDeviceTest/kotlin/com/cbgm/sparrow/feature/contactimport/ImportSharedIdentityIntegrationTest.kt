package com.cbgm.sparrow.feature.contactimport

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.core.protocol.phone.DefaultPhoneNumberNormalizer
import com.cbgm.sparrow.data.database.SparrowDatabase
import com.cbgm.sparrow.feature.contactimport.domain.usecase.ImportSharedIdentityUseCase
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.contacts.data.repository.ContactRepositoryImpl
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDevicePhoneNumber
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactWriterRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import com.cbgm.sparrow.feature.identity.data.repository.IdentityShareRepositoryImpl
import com.cbgm.sparrow.feature.identity.domain.model.SharedContactDetails
import com.cbgm.sparrow.feature.identity.domain.model.SharedIdentityPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImportSharedIdentityIntegrationTest {
    private lateinit var database: SparrowDatabase
    private lateinit var contactRepository: ContactRepositoryImpl
    private lateinit var importSharedIdentity: ImportSharedIdentityUseCase
    private val identityShareRepository = IdentityShareRepositoryImpl()

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database =
            Room
                .inMemoryDatabaseBuilder<SparrowDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()

        val contactDao = database.contactDao()
        val phoneNumberNormalizer = DefaultPhoneNumberNormalizer()

        contactRepository =
            ContactRepositoryImpl(
                contactDao = contactDao,
                contactKeyExchangeDataSource = ContactKeyExchangeDataSource(contactDao),
                phoneNumberNormalizer = phoneNumberNormalizer
            )

        importSharedIdentity =
            ImportSharedIdentityUseCase(
                identityShareRepository = identityShareRepository,
                contactRepository = contactRepository,
                identityInvitationRepository = TestIdentityInvitationRepository,
                identityExchangeRepository = TestIdentityExchangeRepository,
                deviceContactWriterRepository = TestDeviceContactWriterRepository
            )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun sharedIdentityStoresKeysAndContactDetails() =
        runBlocking {
            val encryptionPublicKey = testKey(seed = 1)
            val signingPublicKey = testKey(seed = 101)
            val encodedIdentity =
                encodedIdentity(
                    encryptionPublicKey = encryptionPublicKey,
                    signingPublicKey = signingPublicKey,
                    displayName = "Alice",
                    phoneNumber = "+491701234567"
                )

            val importedContact = importSharedIdentity(encodedIdentity).getOrThrow()

            assertEquals("Alice", importedContact.displayName)
            assertEquals("+491701234567", importedContact.preferredPhoneNumber?.value)

            val identity = assertNotNull(importedContact.sparrowIdentity)
            assertContentEquals(encryptionPublicKey, identity.encryptionPublicKey)
            assertContentEquals(signingPublicKey, identity.signingPublicKey)
            assertEquals(ContactVerificationStatus.UNVERIFIED, identity.verificationStatus)
        }

    @Test
    fun importingSameIdentityTwiceDoesNotDuplicateContact() =
        runBlocking {
            val encodedIdentity =
                encodedIdentity(
                    encryptionPublicKey = testKey(seed = 2),
                    signingPublicKey = testKey(seed = 102),
                    displayName = "Alice",
                    phoneNumber = "+491701234568"
                )

            val first = importSharedIdentity(encodedIdentity).getOrThrow()
            val second = importSharedIdentity(encodedIdentity).getOrThrow()
            val storedContacts = contactRepository.observeContacts().first()

            assertEquals(first.id, second.id)
            assertEquals(1, storedContacts.size)
            assertEquals(1, storedContacts.single().phoneNumbers.size)
        }

    @Test
    fun sharedIdentityMergesWithExistingDeviceContactByPhoneNumber(): Unit =
        runBlocking {
            val deviceContact =
                contactRepository
                    .importDeviceContact(
                        ImportDeviceContactRequest(
                            deviceContactId = "device-contact-1",
                            displayName = "Alice Device",
                            phoneNumbers =
                                listOf(
                                    ImportDevicePhoneNumber(
                                        value = "+49 170 123 4569",
                                        type = ContactPhoneNumberType.MOBILE,
                                        label = null
                                    )
                                )
                        )
                    ).getOrThrow()

            val importedContact =
                importSharedIdentity(
                    encodedIdentity(
                        encryptionPublicKey = testKey(seed = 3),
                        signingPublicKey = testKey(seed = 103),
                        displayName = "Alice Sparrow",
                        phoneNumber = "+491701234569"
                    )
                ).getOrThrow()

            assertEquals(deviceContact.id, importedContact.id)
            assertEquals("device-contact-1", importedContact.deviceContactId)
            assertNotNull(importedContact.sparrowIdentity)
        }

    @Test
    fun invalidPayloadFailsWithoutCreatingContact() =
        runBlocking {
            val result = importSharedIdentity("not-a-sparrow-identity")

            assertTrue(result.isFailure)
            assertTrue(contactRepository.observeContacts().first().isEmpty())
        }

    private fun encodedIdentity(
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray,
        displayName: String?,
        phoneNumber: String
    ): String =
        identityShareRepository
            .encode(
                SharedIdentityPayload(
                    version = 1,
                    encryptionPublicKey = encryptionPublicKey,
                    signingPublicKey = signingPublicKey,
                    contactDetails =
                        SharedContactDetails(
                            displayName = displayName,
                            phoneNumber = phoneNumber
                        )
                )
            ).getOrThrow()

    private fun testKey(seed: Int): ByteArray =
        ByteArray(32) { index ->
            ((seed + index) and 0xFF).toByte()
        }
}

private object TestIdentityExchangeRepository : IdentityExchangeRepository {
    override suspend fun startManualExchange(contactId: String): Result<Unit> = Result.success(Unit)
}

private object TestDeviceContactWriterRepository : DeviceContactWriterRepository {
    override suspend fun addIfNotExists(
        request: AddDeviceContactRequest
    ): AddDeviceContactResult = AddDeviceContactResult.AlreadyExists
}

private object TestIdentityInvitationRepository : IdentityInvitationRepository {
    override suspend fun start(contactId: String): Result<Unit> = error("Not used")

    override fun observePendingIncoming(): Flow<List<com.cbgm.sparrow.feature.contacts.domain.model.PendingContactInvitation>> =
        emptyFlow()

    override fun observeInvitations(
        direction: com.cbgm.sparrow.feature.contacts.domain.model.IdentityInvitationDirection
    ): Flow<List<com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitation>> = emptyFlow()

    override fun observeAcceptedContactIds(): Flow<Set<String>> = emptyFlow()

    override fun observeDeclinedOutgoingContactIds(): Flow<Set<String>> = emptyFlow()

    override fun observeState(
        contactId: String
    ): Flow<com.cbgm.sparrow.feature.contacts.domain.model.IdentityHandshakeState?> = emptyFlow()

    override suspend fun getContactId(invitationId: String): Result<String> = error("Not used")

    override suspend fun accept(invitationId: String): Result<Unit> = error("Not used")

    override suspend fun decline(invitationId: String): Result<Unit> = error("Not used")

    override suspend fun markViewed(
        direction: com.cbgm.sparrow.feature.contacts.domain.model.IdentityInvitationDirection
    ): Result<Unit> = error("Not used")

    override suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> = error("Not used")

    override suspend fun cancelForManualSetup(contactId: String): Result<Unit> = Result.success(Unit)

    override suspend fun requireDirectChatAuthorization(
        contactId: String,
        mode: com.cbgm.sparrow.core.security.DirectIdentitySetupMode
    ): Result<Unit> = error("Not used")

    override suspend fun revokeDirectChatAuthorization(contactId: String): Result<Unit> = error("Not used")

    override suspend fun receiveInvite(
        context: com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext,
        packet: com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket,
        setupMode: com.cbgm.sparrow.core.security.DirectIdentitySetupMode,
        blockedContactIds: Set<String>,
        blockUnknownContactInvites: Boolean
    ): Result<Unit> = error("Not used")

    override suspend fun receiveAccepted(
        context: com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext,
        packet: com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
    ): Result<Unit> = error("Not used")

    override suspend fun receiveReady(
        context: com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext,
        packet: com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
    ): Result<Unit> = error("Not used")

    override suspend fun receiveDeclined(
        context: com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext,
        packet: com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
    ): Result<Unit> = error("Not used")

    override suspend fun receiveDirectChatAuthorizationRevoked(
        context: com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext,
        packet: com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
    ): Result<Unit> = error("Not used")
}
