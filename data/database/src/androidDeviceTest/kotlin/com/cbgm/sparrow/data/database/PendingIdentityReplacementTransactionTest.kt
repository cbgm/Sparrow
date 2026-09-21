package com.cbgm.sparrow.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.sparrow.data.database.entity.PendingRemoteIdentityChangeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Requires Android device; tests the same Room transaction used by the cutover primitive. */
class PendingIdentityReplacementTransactionTest {
    private lateinit var database: SparrowDatabase
    private val oldEnc = ByteArray(32) { 1 }
    private val oldSig = ByteArray(32) { 2 }
    private val newEnc = ByteArray(32) { 3 }
    private val newSig = ByteArray(32) { 4 }

    @BeforeTest fun setup() {
        database = Room.inMemoryDatabaseBuilder<SparrowDatabase>(
            context = ApplicationProvider.getApplicationContext<Context>()
        ).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    @AfterTest fun tearDown() {
        database.close()
    }

    @Test fun confirmedReplacementKeepsContactAndClearsOldTrust() = runBlocking {
        seed()
        val dao = database.pendingRemoteIdentityChangeDao()
        assertTrue(dao.replaceConfirmedIdentity("contact", "invite", 200L))
        val current = requireNotNull(database.remoteIdentityDao().findByPeerId("contact"))
        assertTrue(current.encryptionPublicKey.contentEquals(newEnc))
        assertTrue(current.signingPublicKey.contentEquals(newSig))
        assertEquals("UNVERIFIED", current.verificationStatus)
        assertEquals("ONE_WAY", current.keyExchangeStatus)
        assertFalse(current.locallyImported)
        assertFalse(current.verifiedByContact)
        assertFalse(current.remoteIdentityPacketReceived)
        assertEquals(null, dao.findByPeerId("contact"))
        assertFalse(dao.replaceConfirmedIdentity("contact", "invite", 200L))
    }

    @Test fun expiredOrChangedOldIdentityCannotBeReplaced() = runBlocking {
        seed()
        val dao = database.pendingRemoteIdentityChangeDao()
        assertFalse(dao.replaceConfirmedIdentity("contact", "invite", 1001L))
        assertFalse(dao.replaceConfirmedIdentity("contact", "wrong-invite", 200L))
        val current = requireNotNull(database.remoteIdentityDao().findByPeerId("contact"))
        assertTrue(current.signingPublicKey.contentEquals(oldSig))
        assertEquals("VERIFIED", current.verificationStatus)
    }

    private suspend fun seed() {
        database.contactDao().upsertContact(
            ContactEntity(
                id = "contact",
                displayName = "Contact",
                deviceContactId = null,
                deviceContactLinkStatus = "NOT_LINKED",
                preferredPhoneNumberId = null,
                createdAtEpochMilliseconds = 1L,
                updatedAtEpochMilliseconds = 1L
            )
        )
        database.remoteIdentityDao().upsert(
            ContactPublicIdentityEntity(
                contactId = "contact",
                encryptionPublicKey = oldEnc,
                signingPublicKey = oldSig,
                verificationStatus = "VERIFIED",
                verifiedByContact = true,
                keyExchangeStatus = "MUTUAL",
                locallyImported = true,
                remoteIdentityPacketReceived = true,
                updatedAtEpochMilliseconds = 1L
            )
        )
        database.pendingRemoteIdentityChangeDao().upsert(
            PendingRemoteIdentityChangeEntity(
                peerId = "contact",
                sourcePeerId = "bootstrap",
                invitationId = "invite",
                proposedEncryptionPublicKey = newEnc,
                proposedSigningPublicKey = newSig,
                receivedAtEpochMilliseconds = 100L,
                expiresAtEpochMilliseconds = 1000L,
                fingerprintConfirmedAtEpochMilliseconds = 150L,
                confirmedPreviousEncryptionPublicKey = oldEnc,
                confirmedPreviousSigningPublicKey = oldSig
            )
        )
    }
}
