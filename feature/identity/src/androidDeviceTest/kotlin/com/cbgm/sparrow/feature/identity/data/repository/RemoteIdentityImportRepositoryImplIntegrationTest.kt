package com.cbgm.sparrow.feature.identity.data.repository

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.data.database.SparrowDatabase
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoteIdentityImportRepositoryImplIntegrationTest {
    private lateinit var database: SparrowDatabase
    private lateinit var store: RemoteIdentityImportRepositoryImpl

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder<SparrowDatabase>(context = context)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        store = RemoteIdentityImportRepositoryImpl(database.remoteIdentityDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun preparingRemoteIdentityForHandshakePromotesRemoteIdentityToMutual() =
        runBlocking {
            createContact()
            store
                .storeRemoteIdentity(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = ENCRYPTION_KEY,
                    signingPublicKey = SIGNING_KEY,
                    origin = RemoteIdentityOrigin.REMOTE_PACKET
                ).getOrThrow()

            store
                .acceptRemoteIdentity(
                    contactId = CONTACT_ID,
                    expectedRemoteEncryptionPublicKey = ENCRYPTION_KEY,
                    expectedRemoteSigningPublicKey = SIGNING_KEY
                ).getOrThrow()

            val identity =
                requireNotNull(
                    database.remoteIdentityDao().findByPeerId(CONTACT_ID)
                )
            assertEquals(KeyExchangeStatus.MUTUAL.name, identity.keyExchangeStatus)
            assertTrue(identity.locallyImported)
            assertTrue(identity.remoteIdentityPacketReceived)
        }

    @Test
    fun remoteHandshakeRemainsOneWayUntilReadyConfirmation() =
        runBlocking {
            createContact()
            store
                .storeRemoteIdentity(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = ENCRYPTION_KEY,
                    signingPublicKey = SIGNING_KEY,
                    origin = RemoteIdentityOrigin.REMOTE_HANDSHAKE
                ).getOrThrow()

            var identity =
                requireNotNull(
                    database.remoteIdentityDao().findByPeerId(CONTACT_ID)
                )
            assertEquals(KeyExchangeStatus.ONE_WAY.name, identity.keyExchangeStatus)
            assertFalse(identity.locallyImported)
            assertTrue(identity.remoteIdentityPacketReceived)

            store
                .acceptRemoteIdentityForHandshake(
                    contactId = CONTACT_ID,
                    expectedRemoteEncryptionPublicKey = ENCRYPTION_KEY,
                    expectedRemoteSigningPublicKey = SIGNING_KEY
                ).getOrThrow()

            identity =
                requireNotNull(
                    database.remoteIdentityDao().findByPeerId(CONTACT_ID)
                )
            assertEquals(KeyExchangeStatus.ONE_WAY.name, identity.keyExchangeStatus)
            assertTrue(identity.locallyImported)
            assertTrue(identity.remoteIdentityPacketReceived)

            store
                .markMutual(
                    contactId = CONTACT_ID,
                    expectedRemoteEncryptionPublicKey = ENCRYPTION_KEY,
                    expectedRemoteSigningPublicKey = SIGNING_KEY
                ).getOrThrow()

            identity =
                requireNotNull(
                    database.remoteIdentityDao().findByPeerId(CONTACT_ID)
                )
            assertEquals(KeyExchangeStatus.MUTUAL.name, identity.keyExchangeStatus)
        }

    @Test
    fun trustedQrIdentityIsVerifiedBeforeHandshakeCompletes() =
        runBlocking {
            createContact()

            store
                .storeRemoteIdentity(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = ENCRYPTION_KEY,
                    signingPublicKey = SIGNING_KEY,
                    origin = RemoteIdentityOrigin.TRUSTED_QR_IMPORT
                ).getOrThrow()

            val identity =
                requireNotNull(
                    database.remoteIdentityDao().findByPeerId(CONTACT_ID)
                )

            assertEquals(KeyExchangeStatus.ONE_WAY.name, identity.keyExchangeStatus)
            assertEquals(ContactVerificationStatus.VERIFIED.name, identity.verificationStatus)
            assertTrue(identity.locallyImported)
            assertFalse(identity.remoteIdentityPacketReceived)
        }

    @Test
    fun directHandshakePreservesTrustedQrVerificationForSameKeys() =
        runBlocking {
            createContact()

            store
                .storeRemoteIdentity(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = ENCRYPTION_KEY,
                    signingPublicKey = SIGNING_KEY,
                    origin = RemoteIdentityOrigin.TRUSTED_QR_IMPORT
                ).getOrThrow()

            store
                .storeRemoteIdentity(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = ENCRYPTION_KEY,
                    signingPublicKey = SIGNING_KEY,
                    origin = RemoteIdentityOrigin.REMOTE_HANDSHAKE
                ).getOrThrow()

            store
                .markMutual(
                    contactId = CONTACT_ID,
                    expectedRemoteEncryptionPublicKey = ENCRYPTION_KEY,
                    expectedRemoteSigningPublicKey = SIGNING_KEY
                ).getOrThrow()

            val identity =
                requireNotNull(
                    database.remoteIdentityDao().findByPeerId(CONTACT_ID)
                )

            assertEquals(KeyExchangeStatus.MUTUAL.name, identity.keyExchangeStatus)
            assertEquals(ContactVerificationStatus.VERIFIED.name, identity.verificationStatus)
        }

    @Test
    fun handshakePreparationCannotReplaceTrustedQrIdentity() =
        runBlocking {
            createContact()
            store.storeRemoteIdentity(
                contactId = CONTACT_ID,
                encryptionPublicKey = ENCRYPTION_KEY,
                signingPublicKey = SIGNING_KEY,
                origin = RemoteIdentityOrigin.TRUSTED_QR_IMPORT
            ).getOrThrow()

            val outcome = store.prepareRemoteIdentityForHandshake(
                contactId = CONTACT_ID,
                remoteEncryptionPublicKey = byteArrayOf(99),
                remoteSigningPublicKey = byteArrayOf(98)
            )

            assertTrue(outcome.isFailure)
            val stored = requireNotNull(database.remoteIdentityDao().findByPeerId(CONTACT_ID))
            assertTrue(stored.encryptionPublicKey.contentEquals(ENCRYPTION_KEY))
            assertTrue(stored.signingPublicKey.contentEquals(SIGNING_KEY))
            assertEquals(ContactVerificationStatus.VERIFIED.name, stored.verificationStatus)
        }

    @Test
    fun handshakePreparationCannotReplaceMutualIdentity() =
        runBlocking {
            createContact()
            store.storeRemoteIdentity(
                contactId = CONTACT_ID,
                encryptionPublicKey = ENCRYPTION_KEY,
                signingPublicKey = SIGNING_KEY,
                origin = RemoteIdentityOrigin.LOCAL_IMPORT
            ).getOrThrow()
            store.storeRemoteIdentity(
                contactId = CONTACT_ID,
                encryptionPublicKey = ENCRYPTION_KEY,
                signingPublicKey = SIGNING_KEY,
                origin = RemoteIdentityOrigin.REMOTE_PACKET
            ).getOrThrow()

            val outcome = store.prepareRemoteIdentityForHandshake(
                contactId = CONTACT_ID,
                remoteEncryptionPublicKey = byteArrayOf(99),
                remoteSigningPublicKey = byteArrayOf(98)
            )

            assertTrue(outcome.isFailure)
            val stored = requireNotNull(database.remoteIdentityDao().findByPeerId(CONTACT_ID))
            assertTrue(stored.encryptionPublicKey.contentEquals(ENCRYPTION_KEY))
            assertTrue(stored.signingPublicKey.contentEquals(SIGNING_KEY))
            assertEquals(KeyExchangeStatus.MUTUAL.name, stored.keyExchangeStatus)
        }

    @Test
    fun handshakePreparationAllowsUnpinnedIdentityChange() =
        runBlocking {
            createContact()
            store.storeRemoteIdentity(
                contactId = CONTACT_ID,
                encryptionPublicKey = ENCRYPTION_KEY,
                signingPublicKey = SIGNING_KEY,
                origin = RemoteIdentityOrigin.REMOTE_PACKET
            ).getOrThrow()

            store.prepareRemoteIdentityForHandshake(
                contactId = CONTACT_ID,
                remoteEncryptionPublicKey = byteArrayOf(99),
                remoteSigningPublicKey = byteArrayOf(98)
            ).getOrThrow()

            val stored = requireNotNull(database.remoteIdentityDao().findByPeerId(CONTACT_ID))
            assertTrue(stored.encryptionPublicKey.contentEquals(byteArrayOf(99)))
            assertTrue(stored.signingPublicKey.contentEquals(byteArrayOf(98)))
            assertEquals(KeyExchangeStatus.ONE_WAY.name, stored.keyExchangeStatus)
            assertEquals(ContactVerificationStatus.UNVERIFIED.name, stored.verificationStatus)
        }

    @Test
    fun mutualIdentityCannotBeSilentlyReplacedByRemoteHandshake() =
        runBlocking {
            createContact()
            store
                .storeRemoteIdentity(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = ENCRYPTION_KEY,
                    signingPublicKey = SIGNING_KEY,
                    origin = RemoteIdentityOrigin.LOCAL_IMPORT
                ).getOrThrow()
            store
                .storeRemoteIdentity(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = ENCRYPTION_KEY,
                    signingPublicKey = SIGNING_KEY,
                    origin = RemoteIdentityOrigin.REMOTE_PACKET
                ).getOrThrow()

            val result =
                store.storeRemoteIdentity(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = byteArrayOf(99),
                    signingPublicKey = byteArrayOf(98),
                    origin = RemoteIdentityOrigin.REMOTE_HANDSHAKE
                )

            assertTrue(result.isFailure)
            val identity =
                requireNotNull(
                    database.remoteIdentityDao().findByPeerId(CONTACT_ID)
                )
            assertEquals(KeyExchangeStatus.MUTUAL.name, identity.keyExchangeStatus)
            assertTrue(identity.encryptionPublicKey.contentEquals(ENCRYPTION_KEY))
            assertTrue(identity.signingPublicKey.contentEquals(SIGNING_KEY))
        }

    @Test
    fun preparingRemoteIdentityForHandshakeRejectsChangedKeys() =
        runBlocking {
            createContact()
            store
                .storeRemoteIdentity(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = ENCRYPTION_KEY,
                    signingPublicKey = SIGNING_KEY,
                    origin = RemoteIdentityOrigin.REMOTE_PACKET
                ).getOrThrow()

            val result =
                store.acceptRemoteIdentity(
                    contactId = CONTACT_ID,
                    expectedRemoteEncryptionPublicKey = byteArrayOf(99),
                    expectedRemoteSigningPublicKey = SIGNING_KEY
                )

            assertTrue(result.isFailure)
            val identity =
                requireNotNull(
                    database.remoteIdentityDao().findByPeerId(CONTACT_ID)
                )
            assertEquals(KeyExchangeStatus.ONE_WAY.name, identity.keyExchangeStatus)
            assertFalse(identity.locallyImported)
            assertTrue(identity.remoteIdentityPacketReceived)
        }

    @Test
    fun acceptingHandshakeWithChangedKeysNeverReplacesStoredIdentity() =
        runBlocking {
            createContact()
            store.storeRemoteIdentity(
                contactId = CONTACT_ID,
                encryptionPublicKey = ENCRYPTION_KEY,
                signingPublicKey = SIGNING_KEY,
                origin = RemoteIdentityOrigin.REMOTE_PACKET
            ).getOrThrow()

            val result = store.acceptRemoteIdentityForHandshake(
                contactId = CONTACT_ID,
                expectedRemoteEncryptionPublicKey = REINSTALLED_ENCRYPTION_KEY,
                expectedRemoteSigningPublicKey = REINSTALLED_SIGNING_KEY
            )
            assertTrue(result.isFailure)
            val stored = requireNotNull(database.remoteIdentityDao().findByPeerId(CONTACT_ID))
            assertTrue(stored.encryptionPublicKey.contentEquals(ENCRYPTION_KEY))
            assertTrue(stored.signingPublicKey.contentEquals(SIGNING_KEY))
        }

    @Test
    fun remoteHandshakeCanReplaceOldMutualIdentityAfterRemoteReinstall() =
        runBlocking {
            createContact()
            store
                .storeRemoteIdentity(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = ENCRYPTION_KEY,
                    signingPublicKey = SIGNING_KEY,
                    origin = RemoteIdentityOrigin.LOCAL_IMPORT
                ).getOrThrow()
            store
                .storeRemoteIdentity(
                    contactId = CONTACT_ID,
                    encryptionPublicKey = ENCRYPTION_KEY,
                    signingPublicKey = SIGNING_KEY,
                    origin = RemoteIdentityOrigin.REMOTE_PACKET
                ).getOrThrow()

            store
                .prepareRemoteIdentityForHandshake(
                    contactId = CONTACT_ID,
                    remoteEncryptionPublicKey = REINSTALLED_ENCRYPTION_KEY,
                    remoteSigningPublicKey = REINSTALLED_SIGNING_KEY
                ).getOrThrow()

            val identity =
                requireNotNull(
                    database.remoteIdentityDao().findByPeerId(CONTACT_ID)
                )

            assertTrue(identity.encryptionPublicKey.contentEquals(REINSTALLED_ENCRYPTION_KEY))
            assertTrue(identity.signingPublicKey.contentEquals(REINSTALLED_SIGNING_KEY))
            assertEquals(KeyExchangeStatus.ONE_WAY.name, identity.keyExchangeStatus)
            assertEquals(ContactVerificationStatus.UNVERIFIED.name, identity.verificationStatus)
            assertTrue(identity.locallyImported)
            assertTrue(identity.remoteIdentityPacketReceived)
            assertFalse(identity.verifiedByContact)
        }

    private suspend fun createContact() {
        database.contactDao().upsertContact(
            ContactEntity(
                id = CONTACT_ID,
                displayName = "Alice",
                deviceContactId = null,
                deviceContactLinkStatus = "NOT_LINKED",
                preferredPhoneNumberId = null,
                createdAtEpochMilliseconds = 1L,
                updatedAtEpochMilliseconds = 1L
            )
        )
    }

    private companion object {
        const val CONTACT_ID = "contact-1"
        val ENCRYPTION_KEY = byteArrayOf(1, 2, 3)
        val SIGNING_KEY = byteArrayOf(4, 5, 6)
        val REINSTALLED_ENCRYPTION_KEY = byteArrayOf(7, 8, 9)
        val REINSTALLED_SIGNING_KEY = byteArrayOf(10, 11, 12)
    }
}
