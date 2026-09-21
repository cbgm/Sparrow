package com.cbgm.sparrow.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.PendingRemoteIdentityChangeEntity
import com.cbgm.sparrow.data.database.entity.ProtocolOutboxEntity
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
        seedOutbox("pending", "PENDING")
        seedOutbox("failed", "FAILED")
        seedOutbox("sent", "SENT")
        seedOutbox("expired", "EXPIRED")
        val dao = database.pendingRemoteIdentityChangeDao()
        assertTrue(dao.replaceConfirmedIdentity("contact", "invite", 200L))
        for (packet in listOf("pending", "failed", "sent", "expired")) {
            val row = requireNotNull(database.protocolOutboxDao().findByPacketId(packet))
            assertEquals("QUARANTINED", row.status)
            assertEquals("contact", row.contactId)
        }
        assertEquals(0, database.protocolOutboxDao().getPending(20).size)
        database.protocolOutboxDao().retryFailed(300L)
        database.protocolOutboxDao().requeueInterrupted(300L)
        assertEquals(0, database.protocolOutboxDao().getPending(20).size)
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

    @Test fun cutoverFailsOnlyUnconfirmedMessagesLinkedToQuarantinedPackets() = runBlocking {
        seed()
        database.chatDao().upsertConversation(
            ConversationEntity(
                id = "direct-chat",
                contactId = "contact",
                type = "DIRECT",
                title = null,
                createdAtEpochMilliseconds = 1L,
                updatedAtEpochMilliseconds = 1L
            )
        )
        listOf("QUEUED", "SENDING", "SENT", "FAILED", "DELIVERED", "READ").forEach { state ->
            val packetId = "packet-$state"
            seedOutbox(packetId, if (state == "SENDING") "FAILED" else "SENT")
            seedMessage(packetId, state)
        }
        // Packets associated with an unrelated contact must never be touched.
        seedMessage("other-packet", "QUEUED")
        // Local drafts without an outbox record are not already-encrypted packets.
        seedMessage("waiting", "WAITING_FOR_AUTHORIZATION", null)

        assertTrue(database.pendingRemoteIdentityChangeDao().replaceConfirmedIdentity("contact", "invite", 200L))
        for (state in listOf("QUEUED", "SENDING", "SENT", "FAILED", "DELIVERED", "READ")) {
            val persisted = requireNotNull(database.chatDao().findMessageById("message-packet-$state"))
            assertEquals(
                if (state in listOf("QUEUED", "SENDING", "SENT")) "FAILED" else state,
                persisted.deliveryStatus
            )
            assertEquals("packet-$state", persisted.packetId)
        }
        assertEquals("QUEUED", requireNotNull(database.chatDao().findMessageById("message-other-packet")).deliveryStatus)
        assertEquals("WAITING_FOR_AUTHORIZATION", requireNotNull(database.chatDao().findMessageById("message-waiting")).deliveryStatus)
    }

    private suspend fun seedMessage(id: String, delivery: String, packetId: String? = id) {
        database.chatDao().upsertMessage(
            MessageEntity(
                id = "message-$id",
                conversationId = "direct-chat",
                packetId = packetId,
                text = "Keep my original message",
                transportPayload = null,
                transportMode = "END_TO_END_ENCRYPTED",
                contentStatus = "READABLE",
                deliveryStatus = delivery,
                senderContactId = null,
                isMine = true,
                createdAtEpochMilliseconds = 1L
            )
        )
    }

    @Test fun inFlightPacketPreventsIdentityReplacementAndRollsBack() = runBlocking {
        seed()
        seedOutbox("active", "PROCESSING")
        val dao = database.pendingRemoteIdentityChangeDao()
        assertFalse(dao.replaceConfirmedIdentity("contact", "invite", 200L))
        assertTrue(
            requireNotNull(database.remoteIdentityDao().findByPeerId("contact"))
                .signingPublicKey.contentEquals(oldSig)
        )
        assertEquals("PROCESSING", requireNotNull(database.protocolOutboxDao().findByPacketId("active")).status)
        assertTrue(dao.findByPeerId("contact") != null)
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

    private suspend fun seedOutbox(packetId: String, state: String) {
        database.protocolOutboxDao().upsert(
            ProtocolOutboxEntity(
                id = "outbox-$packetId",
                contactId = "contact",
                packetId = packetId,
                encodedPacket = byteArrayOf(1, 2, 3),
                status = state,
                attemptCount = 1,
                lastError = null,
                expiresAtEpochMilliseconds = null,
                createdAtEpochMilliseconds = 1L,
                updatedAtEpochMilliseconds = 1L
            )
        )
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
