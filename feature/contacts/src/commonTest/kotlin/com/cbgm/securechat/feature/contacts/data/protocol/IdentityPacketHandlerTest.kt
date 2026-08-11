package com.cbgm.securechat.feature.contacts.data.protocol

import com.cbgm.securechat.core.crypto.identity.IdentityAcknowledgementCrypto
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentity
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.OutboxStatus
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.securechat.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.IdentityPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.contacts.domain.identity.ContactVerificationService
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.RemoteIdentityUpdate
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.repository.RemoteIdentityOrigin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IdentityPacketHandlerTest {
    @Test
    fun identityPacketStoresRemoteKeysAndQueuesSignedAcknowledgement() =
        runTest {
            val keyExchangeStore = RecordingContactKeyExchangeStore()
            val crypto = RecordingIdentityAcknowledgementCrypto()
            val outbox = RecordingProtocolOutbox()
            val handler =
                IdentityPacketHandler(
                    contactRepository = FakeContactRepository(createContact()),
                    contactKeyExchangeStore = keyExchangeStore,
                    localSigningKeyPairProvider =
                        object : LocalSigningKeyPairProvider {
                            override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
                                Result.success(
                                    LocalSigningKeyPair(
                                        publicKey = LOCAL_SIGNING_PUBLIC_KEY,
                                        privateKey = LOCAL_SIGNING_PRIVATE_KEY
                                    )
                                )
                        },
                    identityAcknowledgementCrypto = crypto,
                    protocolOutbox = outbox,
                    contactVerificationService = noOpContactVerificationService
                )
            val packet = createIdentityPacket()

            val result =
                handler.handle(
                    context = createContext(),
                    packet = packet
                )

            assertTrue(result.isSuccess)
            assertEquals("contact-1", keyExchangeStore.contactId)
            assertEquals(RemoteIdentityOrigin.REMOTE_PACKET, keyExchangeStore.origin)
            assertContentEquals(REMOTE_ENCRYPTION_KEY, keyExchangeStore.encryptionPublicKey)
            assertContentEquals(REMOTE_SIGNING_KEY, keyExchangeStore.signingPublicKey)
            assertContentEquals(REMOTE_ENCRYPTION_KEY, crypto.signedEncryptionPublicKey)
            assertContentEquals(REMOTE_SIGNING_KEY, crypto.signedSigningPublicKey)
            assertContentEquals(LOCAL_SIGNING_PUBLIC_KEY, crypto.senderSigningPublicKey)
            assertContentEquals(LOCAL_SIGNING_PRIVATE_KEY, crypto.senderSigningPrivateKey)

            val acknowledgement = assertIs<IdentityAcknowledgementPacket>(outbox.enqueuedPacket)
            assertEquals("contact-1", outbox.enqueuedContactId)
            assertContentEquals(LOCAL_SIGNING_PUBLIC_KEY, acknowledgement.senderSigningPublicKey)
            assertContentEquals(REMOTE_ENCRYPTION_KEY, acknowledgement.acknowledgedEncryptionPublicKey)
            assertContentEquals(REMOTE_SIGNING_KEY, acknowledgement.acknowledgedSigningPublicKey)
            assertContentEquals(SIGNATURE, acknowledgement.signature)
        }

    @Test
    fun identityStoreFailurePreventsSignatureAndAcknowledgement() =
        runTest {
            val keyExchangeStore =
                RecordingContactKeyExchangeStore(
                    storeResult = Result.failure(IllegalStateException("store failed"))
                )
            val crypto = RecordingIdentityAcknowledgementCrypto()
            val outbox = RecordingProtocolOutbox()
            val handler =
                IdentityPacketHandler(
                    contactRepository = FakeContactRepository(createContact()),
                    contactKeyExchangeStore = keyExchangeStore,
                    localSigningKeyPairProvider =
                        object : LocalSigningKeyPairProvider {
                            override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
                                Result.success(
                                    LocalSigningKeyPair(
                                        publicKey = LOCAL_SIGNING_PUBLIC_KEY,
                                        privateKey = LOCAL_SIGNING_PRIVATE_KEY
                                    )
                                )
                        },
                    identityAcknowledgementCrypto = crypto,
                    protocolOutbox = outbox,
                    contactVerificationService = noOpContactVerificationService
                )

            val result =
                handler.handle(
                    context = createContext(),
                    packet = createIdentityPacket()
                )

            assertTrue(result.isFailure)
            assertEquals(0, crypto.signCallCount)
            assertEquals(null, outbox.enqueuedPacket)
        }

    @Test
    fun validIdentityAcknowledgementUsesStoredRemoteKeyForVerification() =
        runTest {
            val crypto = RecordingIdentityAcknowledgementCrypto()
            val handler =
                IdentityAcknowledgementPacketHandler(
                    contactRepository = FakeContactRepository(createContact()),
                    contactKeyExchangeStore = RecordingContactKeyExchangeStore(),
                    localPublicIdentityProvider = createLocalPublicIdentityProvider(),
                    identityAcknowledgementCrypto = crypto,
                    contactVerificationService = noOpContactVerificationService
                )
            val packet = createAcknowledgementPacket()

            val result =
                handler.handle(
                    context = createContext(),
                    packet = packet
                )

            assertTrue(result.isSuccess)
            assertEquals(1, crypto.verifyCallCount)
            assertContentEquals(LOCAL_ENCRYPTION_KEY, crypto.verifiedEncryptionPublicKey)
            assertContentEquals(LOCAL_SIGNING_PUBLIC_KEY, crypto.verifiedSigningPublicKey)
            assertContentEquals(REMOTE_SIGNING_KEY, crypto.verificationSenderSigningPublicKey)
            assertContentEquals(SIGNATURE, crypto.verifiedSignature)
        }

    @Test
    fun acknowledgementWithDifferentSenderKeyIsRejectedBeforeVerification() =
        runTest {
            val crypto = RecordingIdentityAcknowledgementCrypto()
            val handler =
                IdentityAcknowledgementPacketHandler(
                    contactRepository = FakeContactRepository(createContact()),
                    contactKeyExchangeStore = RecordingContactKeyExchangeStore(),
                    localPublicIdentityProvider = createLocalPublicIdentityProvider(),
                    identityAcknowledgementCrypto = crypto,
                    contactVerificationService = noOpContactVerificationService
                )
            val packet =
                createAcknowledgementPacket(
                    senderSigningPublicKey = byteArrayOf(99)
                )

            val result =
                handler.handle(
                    context = createContext(),
                    packet = packet
                )

            assertTrue(result.isFailure)
            assertEquals(0, crypto.verifyCallCount)
        }

    @Test
    fun acknowledgementForOldLocalIdentityIsRejectedBeforeVerification() =
        runTest {
            val crypto = RecordingIdentityAcknowledgementCrypto()
            val handler =
                IdentityAcknowledgementPacketHandler(
                    contactRepository = FakeContactRepository(createContact()),
                    contactKeyExchangeStore = RecordingContactKeyExchangeStore(),
                    localPublicIdentityProvider = createLocalPublicIdentityProvider(),
                    identityAcknowledgementCrypto = crypto,
                    contactVerificationService = noOpContactVerificationService
                )
            val packet =
                createAcknowledgementPacket(
                    acknowledgedEncryptionPublicKey = byteArrayOf(98)
                )

            val result =
                handler.handle(
                    context = createContext(),
                    packet = packet
                )

            assertTrue(result.isFailure)
            assertEquals(0, crypto.verifyCallCount)
        }

    @Test
    fun cryptographicVerificationFailureIsPropagated() =
        runTest {
            val crypto =
                RecordingIdentityAcknowledgementCrypto(
                    verifyResult = Result.failure(IllegalStateException("invalid signature"))
                )
            val handler =
                IdentityAcknowledgementPacketHandler(
                    contactRepository = FakeContactRepository(createContact()),
                    contactKeyExchangeStore = RecordingContactKeyExchangeStore(),
                    localPublicIdentityProvider = createLocalPublicIdentityProvider(),
                    identityAcknowledgementCrypto = crypto,
                    contactVerificationService = noOpContactVerificationService
                )

            val result =
                handler.handle(
                    context = createContext(),
                    packet = createAcknowledgementPacket()
                )

            assertTrue(result.isFailure)
            assertEquals("invalid signature", result.exceptionOrNull()?.message)
            assertEquals(1, crypto.verifyCallCount)
        }

    private val noOpContactVerificationService =
        object : ContactVerificationService {
            override suspend fun verify(contactId: String): Result<Unit> = Result.success(Unit)

            override suspend fun sendReceiptIfLocallyVerified(contactId: String): Result<Unit> = Result.success(Unit)
        }

    private fun createIdentityPacket(): IdentityPacket =
        IdentityPacket(
            packetId = "identity-packet-1",
            displayName = "Alice",
            encryptionPublicKey = REMOTE_ENCRYPTION_KEY,
            signingPublicKey = REMOTE_SIGNING_KEY
        )

    private fun createAcknowledgementPacket(
        senderSigningPublicKey: ByteArray = REMOTE_SIGNING_KEY,
        acknowledgedEncryptionPublicKey: ByteArray = LOCAL_ENCRYPTION_KEY,
        acknowledgedSigningPublicKey: ByteArray = LOCAL_SIGNING_PUBLIC_KEY
    ): IdentityAcknowledgementPacket =
        IdentityAcknowledgementPacket(
            packetId = "acknowledgement-1",
            senderSigningPublicKey = senderSigningPublicKey,
            acknowledgedEncryptionPublicKey = acknowledgedEncryptionPublicKey,
            acknowledgedSigningPublicKey = acknowledgedSigningPublicKey,
            signature = SIGNATURE
        )

    private fun createContext(): IncomingPacketContext =
        IncomingPacketContext(
            contactId = "contact-1",
            conversationId = "control-packet",
            encodedTransportPayload = "payload",
            transportMode = "PLAINTEXT",
            receivedAtEpochMilliseconds = 1L
        )

    private fun createContact(): Contact =
        Contact(
            id = "contact-1",
            displayName = "Alice",
            phoneNumbers = emptyList(),
            preferredPhoneNumberId = null,
            deviceContactId = null,
            deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED,
            secureChatIdentity =
                SecureChatIdentity(
                    encryptionPublicKey = REMOTE_ENCRYPTION_KEY,
                    signingPublicKey = REMOTE_SIGNING_KEY,
                    verificationStatus = ContactVerificationStatus.UNVERIFIED,
                    locallyImported = true,
                    keyExchangeStatus = KeyExchangeStatus.ONE_WAY,
                    updatedAtEpochMilliseconds = 1L
                ),
            createdAtEpochMilliseconds = 1L,
            updatedAtEpochMilliseconds = 1L
        )

    private fun createLocalPublicIdentityProvider(): LocalPublicIdentityProvider =
        object : LocalPublicIdentityProvider {
            override suspend fun getLocalPublicIdentity(): Result<LocalPublicIdentity> =
                Result.success(
                    LocalPublicIdentity(
                        encryptionPublicKey = LOCAL_ENCRYPTION_KEY,
                        signingPublicKey = LOCAL_SIGNING_PUBLIC_KEY
                    )
                )
        }

    private class RecordingContactKeyExchangeStore(
        private val storeResult: Result<RemoteIdentityUpdate> =
            Result.success(
                RemoteIdentityUpdate(
                    contactId = "contact-1",
                    encryptionPublicKey = REMOTE_ENCRYPTION_KEY,
                    signingPublicKey = REMOTE_SIGNING_KEY,
                    keyExchangeStatus = KeyExchangeStatus.ONE_WAY,
                    verificationStatus = ContactVerificationStatus.UNVERIFIED,
                    identityChanged = false
                )
            )
    ) : ContactKeyExchangeStore {
        var contactId: String? = null
        var encryptionPublicKey: ByteArray? = null
        var signingPublicKey: ByteArray? = null
        var origin: RemoteIdentityOrigin? = null

        override suspend fun storeRemoteIdentity(
            contactId: String,
            encryptionPublicKey: ByteArray,
            signingPublicKey: ByteArray,
            origin: RemoteIdentityOrigin
        ): Result<RemoteIdentityUpdate> {
            this.contactId = contactId
            this.encryptionPublicKey = encryptionPublicKey.copyOf()
            this.signingPublicKey = signingPublicKey.copyOf()
            this.origin = origin

            return storeResult
        }

        override suspend fun acceptRemoteIdentity(
            contactId: String,
            expectedRemoteEncryptionPublicKey: ByteArray,
            expectedRemoteSigningPublicKey: ByteArray
        ): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun acceptRemoteIdentityForHandshake(
            contactId: String,
            expectedRemoteEncryptionPublicKey: ByteArray,
            expectedRemoteSigningPublicKey: ByteArray
        ): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun markMutual(
            contactId: String,
            expectedRemoteEncryptionPublicKey: ByteArray,
            expectedRemoteSigningPublicKey: ByteArray
        ): Result<Unit> = Result.success(Unit)

        override suspend fun resetAllAfterLocalIdentityChange(): Result<Unit> = Result.failure(UnsupportedOperationException())
    }

    private class RecordingIdentityAcknowledgementCrypto(
        private val verifyResult: Result<Unit> = Result.success(Unit)
    ) : IdentityAcknowledgementCrypto {
        var signCallCount: Int = 0
        var verifyCallCount: Int = 0

        var signedEncryptionPublicKey: ByteArray? = null
        var signedSigningPublicKey: ByteArray? = null
        var senderSigningPublicKey: ByteArray? = null
        var senderSigningPrivateKey: ByteArray? = null

        var verifiedEncryptionPublicKey: ByteArray? = null
        var verifiedSigningPublicKey: ByteArray? = null
        var verificationSenderSigningPublicKey: ByteArray? = null
        var verifiedSignature: ByteArray? = null

        override suspend fun sign(
            acknowledgedEncryptionPublicKey: ByteArray,
            acknowledgedSigningPublicKey: ByteArray,
            senderSigningPublicKey: ByteArray,
            senderSigningPrivateKey: ByteArray
        ): Result<ByteArray> {
            signCallCount += 1
            signedEncryptionPublicKey = acknowledgedEncryptionPublicKey.copyOf()
            signedSigningPublicKey = acknowledgedSigningPublicKey.copyOf()
            this.senderSigningPublicKey = senderSigningPublicKey.copyOf()
            this.senderSigningPrivateKey = senderSigningPrivateKey.copyOf()

            return Result.success(SIGNATURE)
        }

        override suspend fun verify(
            acknowledgedEncryptionPublicKey: ByteArray,
            acknowledgedSigningPublicKey: ByteArray,
            senderSigningPublicKey: ByteArray,
            signature: ByteArray
        ): Result<Unit> {
            verifyCallCount += 1
            verifiedEncryptionPublicKey = acknowledgedEncryptionPublicKey.copyOf()
            verifiedSigningPublicKey = acknowledgedSigningPublicKey.copyOf()
            verificationSenderSigningPublicKey = senderSigningPublicKey.copyOf()
            verifiedSignature = signature.copyOf()

            return verifyResult
        }
    }

    private class RecordingProtocolOutbox : ProtocolOutbox {
        var enqueuedContactId: String? = null
        var enqueuedPacket: SecureChatPacket? = null

        override suspend fun enqueue(
            contactId: String,
            packet: SecureChatPacket
        ): Result<ProtocolOutboxItem> {
            enqueuedContactId = contactId
            enqueuedPacket = packet

            return Result.success(
                ProtocolOutboxItem(
                    id = "outbox-1",
                    contactId = contactId,
                    packetId = packet.packetId,
                    encodedPacket = byteArrayOf(1),
                    status = OutboxStatus.PENDING,
                    attemptCount = 0,
                    lastError = null,
                    createdAtEpochMilliseconds = 1L,
                    updatedAtEpochMilliseconds = 1L
                )
            )
        }

        override fun observePending(): Flow<List<ProtocolOutboxItem>> = flowOf(emptyList())

        override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> = Result.success(emptyList())

        override suspend fun markProcessing(itemId: String): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun markSent(itemId: String): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun markFailed(
            itemId: String,
            errorMessage: String
        ): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun retry(itemId: String): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun resend(packetId: String): Result<Unit> = Result.success(Unit)

        override suspend fun requeueInterrupted(): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun retryFailed(): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> = Result.success(null)
    }

    private class FakeContactRepository(
        private val contact: Contact
    ) : ContactRepository {
        override suspend fun importDeviceContact(request: ImportDeviceContactRequest): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun importContact(request: ImportContactRequest): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun getContact(contactId: String): Result<Contact?> = Result.success(contact.takeIf { it.id == contactId })

        override suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): Result<Contact?> = Result.success(null)

        override suspend fun findOrCreateByPhoneNumber(phoneNumber: String): Result<Contact> = Result.failure(UnsupportedOperationException())

        override fun observeContacts(): Flow<List<Contact>> = flowOf(listOf(contact))

        override suspend fun updateContactDetails(
            contactId: String,
            displayName: String?,
            phoneNumber: String?
        ): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun markVerified(contactId: String): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun markKeyExchangeMutual(contactId: String): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun resetKeyExchange(contactId: String): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun updateDeviceContactLinkStatus(
            deviceContactId: String,
            status: DeviceContactLinkStatus
        ): Result<Contact?> = Result.failure(UnsupportedOperationException())
    }

    private companion object {
        val REMOTE_ENCRYPTION_KEY = byteArrayOf(1, 2, 3)
        val REMOTE_SIGNING_KEY = byteArrayOf(4, 5, 6)
        val LOCAL_ENCRYPTION_KEY = byteArrayOf(7, 8, 9)
        val LOCAL_SIGNING_PUBLIC_KEY = byteArrayOf(10, 11, 12)
        val LOCAL_SIGNING_PRIVATE_KEY = byteArrayOf(13, 14, 15)
        val SIGNATURE = byteArrayOf(16, 17, 18)
    }
}
