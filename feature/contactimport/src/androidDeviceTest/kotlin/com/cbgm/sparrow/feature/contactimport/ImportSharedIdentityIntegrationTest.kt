package com.cbgm.sparrow.feature.contactimport

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.core.protocol.phone.DefaultPhoneNumberNormalizer
import com.cbgm.sparrow.data.database.SparrowDatabase
import com.cbgm.sparrow.feature.contactimport.domain.usecase.ImportSharedIdentityUseCase
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactLocalDataSource
import com.cbgm.sparrow.feature.contacts.data.repository.ContactRepositoryImpl
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDevicePhoneNumber
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactWriterRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.identity.data.repository.IdentityShareRepositoryImpl
import com.cbgm.sparrow.feature.identity.data.repository.RemoteIdentityImportRepositoryImpl
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.model.SharedContactDetails
import com.cbgm.sparrow.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityReadRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.CancelIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.FindRemoteIdentityPeerIdUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ImportRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StartManualIdentityExchangeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
                contactDataSource = ContactLocalDataSource(contactDao),
                phoneNumberNormalizer = phoneNumberNormalizer
            )

        val identityReads = object : RemoteIdentityReadRepository {
            override suspend fun get(peerId: String): Result<RemotePeerIdentity?> = Result.success(
                database.remoteIdentityDao().findByPeerId(peerId)?.let { row ->
                    RemotePeerIdentity(
                        peerId = row.contactId,
                        encryptionPublicKey = row.encryptionPublicKey.copyOf(),
                        signingPublicKey = row.signingPublicKey.copyOf(),
                        verificationStatus = ContactVerificationStatus.valueOf(row.verificationStatus),
                        keyExchangeStatus = com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus.valueOf(row.keyExchangeStatus),
                        verifiedByContact = row.verifiedByContact,
                        locallyImported = row.locallyImported,
                        updatedAtEpochMilliseconds = row.updatedAtEpochMilliseconds
                    )
                }
            )

            override suspend fun findPeerIdBySigningPublicKey(signingPublicKey: ByteArray): Result<String?> =
                Result.success(database.remoteIdentityDao().findBySigningPublicKey(signingPublicKey)?.contactId)

            override fun observeAll(): Flow<List<RemotePeerIdentity>> = kotlinx.coroutines.flow.flowOf(emptyList())
        }

        importSharedIdentity =
            ImportSharedIdentityUseCase(
                identityShareRepository = identityShareRepository,
                contactRepository = contactRepository,
                cancelIdentityExchange = CancelIdentityExchangeUseCase(TestIdentityExchangeRepository),
                importRemoteIdentity = ImportRemoteIdentityUseCase(
                    RemoteIdentityImportRepositoryImpl(database.remoteIdentityDao())
                ),
                findRemoteIdentityPeerId = FindRemoteIdentityPeerIdUseCase(identityReads),
                startManualIdentityExchange = StartManualIdentityExchangeUseCase(TestIdentityExchangeRepository),
                deviceContactWriterRepository = TestDeviceContactWriterRepository,
                getContact = GetContactUseCase(contactRepository, GetRemoteIdentityUseCase(identityReads))
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
    fun signingKeyMatchReusesContactWhenSharedPhoneNumberChanges() =
        runBlocking {
            val encryptionKey = testKey(seed = 12)
            val signingKey = testKey(seed = 112)
            val first = importSharedIdentity(
                encodedIdentity(encryptionKey, signingKey, "Alice", "+491701234571")
            ).getOrThrow()
            val second = importSharedIdentity(
                encodedIdentity(encryptionKey, signingKey, "Alice Updated", "+491701234572")
            ).getOrThrow()

            assertEquals(first.id, second.id)
            assertEquals(1, contactRepository.observeContacts().first().size)
            assertEquals("Alice Updated", second.displayName)
            assertEquals("+491701234572", second.preferredPhoneNumber?.value)
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
    override suspend fun stageRemoteIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Boolean> = error("Not used")

    override suspend fun acceptRemoteIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> = error("Not used")

    override suspend fun establishMutualIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> = error("Not used")

    override suspend fun ensureRemoteSigningIdentity(
        peerId: String,
        signingPublicKey: ByteArray
    ): Result<Unit> = error("Not used")

    override suspend fun start(peerId: String, invitationSenderLabel: String) = error("Not used")

    override suspend fun startManual(peerId: String): Result<Unit> = Result.success(Unit)

    override suspend fun accept(exchangeId: String): Result<Unit> = error("Not used")

    override suspend fun decline(exchangeId: String): Result<Unit> = error("Not used")

    override fun observeState(peerId: String): Flow<com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState?> =
        kotlinx.coroutines.flow.emptyFlow()

    override fun observeResults(): Flow<List<com.cbgm.sparrow.feature.identity.domain.model.IdentityResult>> =
        kotlinx.coroutines.flow.emptyFlow()

    override suspend fun cancel(peerId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getPeerState(
        peerId: String
    ): Result<com.cbgm.sparrow.feature.identity.domain.model.IdentityPeerState> =
        Result.success(com.cbgm.sparrow.feature.identity.domain.model.IdentityPeerState(false, false))

    override suspend fun getExchangeClosure(peerId: String): Result<com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosure?> =
        error("Not used")

    override suspend fun closeExchange(exchangeId: String, peerId: String): Result<Unit> = error("Not used")

    override suspend fun getExchangeBinding(exchangeId: String): Result<com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeBinding?> =
        error("Not used")

    override suspend fun invalidateExchange(
        exchangeId: String,
        peerId: String,
        expectedChallenge: ByteArray,
        expectedSigningPublicKey: ByteArray,
        atEpochMilliseconds: Long
    ): Result<Unit> = error("Not used")

    override suspend fun receiveExchange(
        context: com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext,
        offer: com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeOffer,
        wasKnownPeerAtReceive: Boolean
    ): Result<Unit> = error("Not used")

    override suspend fun reassignPeer(fromPeerId: String, toPeerId: String): Result<Unit> = error("Not used")

    override suspend fun receiveManualIdentity(
        context: com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext,
        packet: com.cbgm.sparrow.core.protocol.packet.IdentityPacket
    ): Result<Boolean> = error("Not used")

    override suspend fun receiveIdentityAcknowledgement(
        context: com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext,
        packet: com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
    ): Result<Boolean> = error("Not used")

    override suspend fun receiveAccepted(
        context: com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext,
        acceptance: com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeAcceptance
    ): Result<Unit> = error("Not used")

    override suspend fun recordRemoteDecline(
        exchangeId: String,
        peerId: String,
        inviteChallenge: ByteArray,
        remoteSigningPublicKey: ByteArray,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = error("Not used")

    override suspend fun receiveReady(
        context: com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext,
        ready: com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeReady
    ): Result<Unit> = error("Not used")
}

private object TestDeviceContactWriterRepository : DeviceContactWriterRepository {
    override suspend fun addIfNotExists(
        request: AddDeviceContactRequest
    ): AddDeviceContactResult = AddDeviceContactResult.AlreadyExists
}
