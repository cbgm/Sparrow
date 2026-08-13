package com.cbgm.securechat.feature.messaging.application.outbox

import com.cbgm.securechat.core.crypto.transport.EncryptedTransportPayload
import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.crypto.transport.TransportMessageCipher
import com.cbgm.securechat.core.crypto.transport.TransportPayloadCodec
import com.cbgm.securechat.core.protocol.codec.PacketCodec
import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.core.protocol.outbox.OutboxStatus
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.securechat.core.protocol.packet.ChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.ContactInvitePacket
import com.cbgm.securechat.core.protocol.packet.ContactReadyPacket
import com.cbgm.securechat.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.securechat.feature.messaging.application.relay.ContactRelayIdResolver
import com.cbgm.securechat.feature.messaging.application.relay.GroupRelayIdResolver
import com.cbgm.securechat.feature.messaging.application.relay.GroupTransportKeyResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultOutboxProcessorTest {
    @Test
    fun mutualIdentityEncryptsAndMarksPacketSent() =
        runTest {
            val outbox = FakeProtocolOutbox(listOf(createItem()))
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val sender = RecordingOutgoingWireSender()
            val listener = RecordingDeliveryStateListener()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.MUTUAL),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    sender = sender,
                    listener = listener
                )

            val result = processor.processPending(limit = 20).getOrThrow()

            assertEquals(1, result.processedCount)
            assertEquals(1, result.sentCount)
            assertEquals(0, result.failedCount)
            assertContentEquals(ENCODED_PACKET, cipher.plaintext)
            assertContentEquals(REMOTE_ENCRYPTION_KEY, cipher.recipientPublicKey)
            assertEquals(TransportEncryptionMode.SEALED_BOX, payloadCodec.payloads.single().mode)
            assertEquals(
                listOf("processing:packet-1", "prepared:packet-1:SEALED_BOX", "sent:packet-1"),
                listener.events
            )
            assertEquals(listOf("outbox-1"), outbox.sentItemIds)
            assertEquals(listOf("recipient-relay-id" to "encoded-transport-payload"), sender.sent)
        }

    @Test
    fun oneWayIdentityUsesPlaintextWithoutCallingCipher() =
        runTest {
            val outbox = FakeProtocolOutbox(listOf(createItem()))
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.ONE_WAY),
                    cipher = cipher,
                    payloadCodec = payloadCodec
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(1, result.sentCount)
            assertEquals(0, cipher.callCount)
            assertEquals(TransportEncryptionMode.PLAINTEXT, payloadCodec.payloads.single().mode)
            assertContentEquals(ENCODED_PACKET, payloadCodec.payloads.single().payload)
        }

    @Test
    fun groupCreationRequiresEncryptedTransport() =
        runTest {
            val outbox =
                FakeProtocolOutbox(
                    listOf(
                        createItem(
                            encodedPacket = GROUP_PACKET_BYTES
                        )
                    )
                )
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.MUTUAL),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    packetCodec = TestPacketCodec()
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(1, result.sentCount)
            assertEquals(1, cipher.callCount)
            assertEquals(TransportEncryptionMode.SEALED_BOX, payloadCodec.payloads.single().mode)
            assertContentEquals(GROUP_PACKET_BYTES, cipher.plaintext)
        }

    @Test
    fun groupCreationFailsInsteadOfFallingBackToPlaintext() =
        runTest {
            val outbox = FakeProtocolOutbox(listOf(createItem(encodedPacket = GROUP_PACKET_BYTES)))
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.ONE_WAY),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    packetCodec = TestPacketCodec()
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(0, result.sentCount)
            assertEquals(1, result.failedCount)
            assertEquals(0, cipher.callCount)
            assertTrue(payloadCodec.payloads.isEmpty())
            assertTrue(
                outbox.failedItems
                    .single()
                    .second
                    .contains("mutual SecureChat key exchange")
            )
        }

    @Test
    fun sharedKeyGroupMessageCanUsePlainOuterTransportWithoutPairwiseIdentity() =
        runTest {
            val outbox =
                FakeProtocolOutbox(
                    listOf(createItem(encodedPacket = GROUP_MESSAGE_PACKET_BYTES))
                )
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val sender = RecordingOutgoingWireSender()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.ONE_WAY),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    packetCodec = TestPacketCodec(),
                    sender = sender
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(1, result.sentCount)
            assertEquals(0, cipher.callCount)
            assertEquals(TransportEncryptionMode.PLAINTEXT, payloadCodec.payloads.single().mode)
            assertContentEquals(GROUP_MESSAGE_PACKET_BYTES, payloadCodec.payloads.single().payload)
            assertEquals(
                listOf("group-recipient-relay-id" to "encoded-transport-payload"),
                sender.sent
            )
        }

    @Test
    fun contactInviteUsesPlainBootstrapTransportEvenWhenContactIsAlreadyMutual() =
        runTest {
            val outbox =
                FakeProtocolOutbox(
                    listOf(createItem(encodedPacket = CONTACT_INVITE_PACKET_BYTES))
                )
            val resolver = RecordingContactRelayIdResolver()
            val sender = RecordingOutgoingWireSender()
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.MUTUAL),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    packetCodec = TestPacketCodec(),
                    sender = sender,
                    contactRelayIdResolver = resolver
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(1, result.sentCount)
            assertEquals(0, cipher.callCount)
            assertEquals(TransportEncryptionMode.PLAINTEXT, payloadCodec.payloads.single().mode)
            assertEquals(0, resolver.canonicalResolveCount)
            assertEquals(1, resolver.bootstrapResolveCount)
            assertEquals(
                listOf("bootstrap-recipient-relay-id" to "encoded-transport-payload"),
                sender.sent
            )
        }

    @Test
    fun groupInviteUsesPlainBootstrapTransportEvenWhenContactIsAlreadyMutual() =
        runTest {
            val outbox =
                FakeProtocolOutbox(
                    listOf(createItem(encodedPacket = GROUP_INVITE_PACKET_BYTES))
                )
            val resolver = RecordingContactRelayIdResolver()
            val sender = RecordingOutgoingWireSender()
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.MUTUAL),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    packetCodec = TestPacketCodec(),
                    sender = sender,
                    contactRelayIdResolver = resolver
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(1, result.sentCount)
            assertEquals(0, cipher.callCount)
            assertEquals(TransportEncryptionMode.PLAINTEXT, payloadCodec.payloads.single().mode)
            assertEquals(0, resolver.canonicalResolveCount)
            assertEquals(1, resolver.bootstrapResolveCount)
            assertEquals(
                listOf("bootstrap-recipient-relay-id" to "encoded-transport-payload"),
                sender.sent
            )
        }

    @Test
    fun contactReadyUsesEncryptedTransportBeforeMutualState() =
        runTest {
            val outbox =
                FakeProtocolOutbox(
                    listOf(createItem(encodedPacket = CONTACT_READY_PACKET_BYTES))
                )
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.ONE_WAY),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    packetCodec = TestPacketCodec()
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(1, result.sentCount)
            assertEquals(1, cipher.callCount)
            assertContentEquals(CONTACT_READY_PACKET_BYTES, cipher.plaintext)
            assertContentEquals(REMOTE_ENCRYPTION_KEY, cipher.recipientPublicKey)
            assertEquals(TransportEncryptionMode.SEALED_BOX, payloadCodec.payloads.single().mode)
        }

    @Test
    fun contactReadyRejectsChangedRecipientIdentity() =
        runTest {
            val outbox =
                FakeProtocolOutbox(
                    listOf(createItem(encodedPacket = CONTACT_READY_PACKET_BYTES))
                )
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact =
                        createContact(
                            keyExchangeStatus = KeyExchangeStatus.ONE_WAY,
                            encryptionPublicKey = byteArrayOf(99),
                            signingPublicKey = byteArrayOf(98)
                        ),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    packetCodec = TestPacketCodec()
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(0, result.sentCount)
            assertEquals(1, result.failedCount)
            assertEquals(0, cipher.callCount)
            assertTrue(payloadCodec.payloads.isEmpty())
            assertTrue(
                outbox.failedItems
                    .single()
                    .second
                    .contains("identity changed")
            )
        }

    @Test
    fun verificationReceiptRequiresEncryptedTransport() =
        runTest {
            val outbox =
                FakeProtocolOutbox(
                    listOf(createItem(encodedPacket = VERIFICATION_RECEIPT_PACKET_BYTES))
                )
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.MUTUAL),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    packetCodec = TestPacketCodec()
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(1, result.sentCount)
            assertEquals(1, cipher.callCount)
            assertContentEquals(VERIFICATION_RECEIPT_PACKET_BYTES, cipher.plaintext)
            assertContentEquals(REMOTE_ENCRYPTION_KEY, cipher.recipientPublicKey)
            assertEquals(TransportEncryptionMode.SEALED_BOX, payloadCodec.payloads.single().mode)
        }

    @Test
    fun verificationReceiptRejectsChangedRecipientIdentity() =
        runTest {
            val outbox =
                FakeProtocolOutbox(
                    listOf(createItem(encodedPacket = VERIFICATION_RECEIPT_PACKET_BYTES))
                )
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact =
                        createContact(
                            keyExchangeStatus = KeyExchangeStatus.MUTUAL,
                            encryptionPublicKey = ByteArray(32) { 99 },
                            signingPublicKey = ByteArray(32) { 98 }
                        ),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    packetCodec = TestPacketCodec()
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(0, result.sentCount)
            assertEquals(1, result.failedCount)
            assertEquals(0, cipher.callCount)
            assertTrue(payloadCodec.payloads.isEmpty())
            assertTrue(
                outbox.failedItems
                    .single()
                    .second
                    .contains("identity changed")
            )
        }

    @Test
    fun failedItemIsMarkedFailedAndDoesNotStopRemainingItems() =
        runTest {
            val first = createItem(id = "outbox-1", packetId = "packet-1")
            val second = createItem(id = "outbox-2", packetId = "packet-2")
            val outbox = FakeProtocolOutbox(listOf(first, second))
            val sender = RecordingOutgoingWireSender(failingCalls = setOf(1))
            val listener = RecordingDeliveryStateListener()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.MUTUAL),
                    sender = sender,
                    listener = listener
                )

            val result = processor.processPending(limit = 20).getOrThrow()

            assertEquals(2, result.processedCount)
            assertEquals(1, result.sentCount)
            assertEquals(1, result.failedCount)
            assertEquals(listOf("outbox-1"), outbox.failedItems.map { it.first })
            assertTrue(
                outbox.failedItems
                    .single()
                    .second
                    .contains("send failed")
            )
            assertEquals(listOf("outbox-2"), outbox.sentItemIds)
            assertEquals(2, sender.sent.size)
            assertTrue(listener.events.contains("failed:packet-1:send failed"))
            assertTrue(listener.events.contains("sent:packet-2"))
        }

    private fun createProcessor(
        outbox: FakeProtocolOutbox,
        contact: Contact,
        cipher: RecordingTransportMessageCipher = RecordingTransportMessageCipher(),
        payloadCodec: RecordingTransportPayloadCodec = RecordingTransportPayloadCodec(),
        packetCodec: PacketCodec = TestPacketCodec(),
        sender: RecordingOutgoingWireSender = RecordingOutgoingWireSender(),
        listener: RecordingDeliveryStateListener = RecordingDeliveryStateListener(),
        contactRelayIdResolver: ContactRelayIdResolver = RecordingContactRelayIdResolver(),
        groupRelayIdResolver: GroupRelayIdResolver = RecordingGroupRelayIdResolver()
    ): DefaultOutboxProcessor =
        DefaultOutboxProcessor(
            protocolOutbox = outbox,
            getContact = GetContactUseCase(FakeContactRepository(contact)),
            transportPayloadFactory =
                DefaultOutgoingTransportPayloadFactory(
                    transportMessageCipher = cipher,
                    packetTransportPolicy = DefaultOutgoingPacketTransportPolicy(),
                    groupTransportKeyResolver = NoGroupTransportKeyResolver
                ),
            transportPayloadCodec = payloadCodec,
            packetCodec = packetCodec,
            contactRelayIdResolver = contactRelayIdResolver,
            groupRelayIdResolver = groupRelayIdResolver,
            outgoingWireSender = sender,
            deliveryStateListener = listener
        )

    private fun createItem(
        id: String = "outbox-1",
        packetId: String = "packet-1",
        encodedPacket: ByteArray = ENCODED_PACKET
    ): ProtocolOutboxItem =
        ProtocolOutboxItem(
            id = id,
            contactId = "contact-1",
            packetId = packetId,
            encodedPacket = encodedPacket,
            status = OutboxStatus.PENDING,
            attemptCount = 0,
            lastError = null,
            createdAtEpochMilliseconds = 1L,
            updatedAtEpochMilliseconds = 1L
        )

    private fun createContact(
        keyExchangeStatus: KeyExchangeStatus,
        encryptionPublicKey: ByteArray = REMOTE_ENCRYPTION_KEY,
        signingPublicKey: ByteArray = REMOTE_SIGNING_KEY
    ): Contact =
        Contact(
            id = "contact-1",
            displayName = "Alice",
            phoneNumbers =
                listOf(
                    ContactPhoneNumber(
                        id = "phone-1",
                        value = "+491701234567",
                        type = ContactPhoneNumberType.MOBILE,
                        label = null
                    )
                ),
            preferredPhoneNumberId = "phone-1",
            deviceContactId = null,
            deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED,
            secureChatIdentity =
                SecureChatIdentity(
                    encryptionPublicKey = encryptionPublicKey,
                    signingPublicKey = signingPublicKey,
                    verificationStatus = ContactVerificationStatus.UNVERIFIED,
                    keyExchangeStatus = keyExchangeStatus,
                    updatedAtEpochMilliseconds = 1L
                ),
            createdAtEpochMilliseconds = 1L,
            updatedAtEpochMilliseconds = 1L
        )

    private class FakeProtocolOutbox(
        private val pendingItems: List<ProtocolOutboxItem>
    ) : ProtocolOutbox {
        val processingItemIds = mutableListOf<String>()
        val sentItemIds = mutableListOf<String>()
        val failedItems = mutableListOf<Pair<String, String>>()

        override suspend fun enqueue(
            contactId: String,
            packet: SecureChatPacket
        ): Result<ProtocolOutboxItem> = Result.failure(UnsupportedOperationException())

        override fun observePending(): Flow<List<ProtocolOutboxItem>> = flowOf(pendingItems)

        override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> = Result.success(pendingItems.take(limit))

        override suspend fun markProcessing(itemId: String): Result<Unit> {
            processingItemIds += itemId
            return Result.success(Unit)
        }

        override suspend fun markSent(itemId: String): Result<Unit> {
            sentItemIds += itemId
            return Result.success(Unit)
        }

        override suspend fun markFailed(
            itemId: String,
            errorMessage: String
        ): Result<Unit> {
            failedItems += itemId to errorMessage
            return Result.success(Unit)
        }

        override suspend fun retry(itemId: String): Result<Unit> = Result.success(Unit)

        override suspend fun resend(packetId: String): Result<Unit> = Result.success(Unit)

        override suspend fun requeueInterrupted(): Result<Unit> = Result.success(Unit)

        override suspend fun retryFailed(): Result<Unit> = Result.success(Unit)

        override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> =
            Result.success(
                pendingItems.firstOrNull { item ->
                    item.packetId == packetId
                }
            )
    }

    private class RecordingTransportMessageCipher : TransportMessageCipher {
        var callCount: Int = 0
        var plaintext: ByteArray? = null
        var recipientPublicKey: ByteArray? = null

        override suspend fun encryptForRecipient(
            plaintext: ByteArray,
            recipientPublicKey: ByteArray
        ): Result<EncryptedTransportPayload> {
            callCount += 1
            this.plaintext = plaintext.copyOf()
            this.recipientPublicKey = recipientPublicKey.copyOf()

            return Result.success(
                EncryptedTransportPayload(
                    version = 1,
                    mode = TransportEncryptionMode.SEALED_BOX,
                    payload = byteArrayOf(9, 9, 9)
                )
            )
        }

        override suspend fun decryptFromSender(
            encryptedPayload: EncryptedTransportPayload,
            localPublicKey: ByteArray,
            localPrivateKey: ByteArray
        ): Result<ByteArray> = Result.failure(UnsupportedOperationException())
    }

    private class RecordingTransportPayloadCodec : TransportPayloadCodec {
        val payloads = mutableListOf<EncryptedTransportPayload>()

        override fun encode(payload: EncryptedTransportPayload): String {
            payloads += payload
            return "encoded-transport-payload"
        }

        override fun decode(encoded: String): Result<EncryptedTransportPayload> = Result.failure(UnsupportedOperationException())
    }

    private class TestPacketCodec : PacketCodec {
        override fun encode(packet: SecureChatPacket): Result<ByteArray> = Result.failure(UnsupportedOperationException())

        override fun decode(encodedPacket: ByteArray): Result<SecureChatPacket> =
            if (encodedPacket.contentEquals(GROUP_PACKET_BYTES)) {
                Result.success(
                    GroupCreatedPacket(
                        packetId = "group-packet",
                        groupId = "group-1",
                        title = "Group",
                        createdAtEpochMilliseconds = 1L,
                        epoch = 1,
                        members =
                            listOf(
                                GroupMemberPayload(
                                    displayName = "Alice",
                                    encryptionPublicKey = byteArrayOf(1),
                                    signingPublicKey = byteArrayOf(2),
                                    role = "OWNER",
                                    phoneNumber = "+491701234567"
                                ),
                                GroupMemberPayload(
                                    displayName = "Bob",
                                    encryptionPublicKey = byteArrayOf(3),
                                    signingPublicKey = byteArrayOf(4),
                                    role = "MEMBER",
                                    phoneNumber = "+491701234568"
                                )
                            ),
                        wrappedGroupKey = byteArrayOf(5),
                        ownerSignature = byteArrayOf(6)
                    )
                )
            } else if (encodedPacket.contentEquals(GROUP_MESSAGE_PACKET_BYTES)) {
                Result.success(
                    GroupChatMessagePacket(
                        packetId = "group-message-packet",
                        groupId = "group-1",
                        epoch = 1,
                        messageId = "group-message-1",
                        sentAtEpochMilliseconds = 1L,
                        nonce = byteArrayOf(1),
                        ciphertext = byteArrayOf(2),
                        senderSignature = byteArrayOf(3)
                    )
                )
            } else if (encodedPacket.contentEquals(GROUP_INVITE_PACKET_BYTES)) {
                Result.success(
                    GroupInvitePacket(
                        packetId = "group-invite-invitation-1",
                        invitationId = "group-invitation-1",
                        groupId = "group-1",
                        title = "Group",
                        createdAtEpochMilliseconds = 1L,
                        expiresAtEpochMilliseconds = 2L,
                        challenge = ByteArray(32) { 1 },
                        ownerEncryptionPublicKey = ByteArray(32) { 2 },
                        ownerSigningPublicKey = ByteArray(32) { 3 },
                        ownerSignature = ByteArray(64) { 4 }
                    )
                )
            } else if (encodedPacket.contentEquals(CONTACT_INVITE_PACKET_BYTES)) {
                Result.success(
                    ContactInvitePacket(
                        packetId = "contact-invite-invitation-1",
                        invitationId = "invitation-1",
                        displayName = "+491701234567",
                        createdAtEpochMilliseconds = 1L,
                        expiresAtEpochMilliseconds = 2L,
                        inviteChallenge = ByteArray(32) { 1 },
                        encryptionPublicKey = ByteArray(32) { 2 },
                        signingPublicKey = ByteArray(32) { 3 },
                        signature = ByteArray(64) { 4 }
                    )
                )
            } else if (encodedPacket.contentEquals(CONTACT_READY_PACKET_BYTES)) {
                Result.success(
                    ContactReadyPacket(
                        packetId = "contact-ready-invitation-1",
                        invitationId = "invitation-1",
                        readyAtEpochMilliseconds = 1L,
                        responseChallenge = ByteArray(32) { 1 },
                        acceptedResponderEncryptionPublicKey = REMOTE_ENCRYPTION_KEY,
                        acceptedResponderSigningPublicKey = REMOTE_SIGNING_KEY,
                        senderEncryptionPublicKey = ByteArray(32) { 20 },
                        senderSigningPublicKey = ByteArray(32) { 23 },
                        signature = ByteArray(64) { 26 }
                    )
                )
            } else if (encodedPacket.contentEquals(VERIFICATION_RECEIPT_PACKET_BYTES)) {
                Result.success(
                    ContactVerificationReceiptPacket(
                        packetId = "contact-verification-receipt-receipt-1",
                        receiptId = "receipt-1",
                        verifiedAtEpochMilliseconds = 1L,
                        senderEncryptionPublicKey = ByteArray(32) { 20 },
                        senderSigningPublicKey = ByteArray(32) { 23 },
                        verifiedEncryptionPublicKey = REMOTE_ENCRYPTION_KEY,
                        verifiedSigningPublicKey = REMOTE_SIGNING_KEY,
                        signature = ByteArray(64) { 26 }
                    )
                )
            } else {
                Result.success(
                    ChatMessagePacket(
                        packetId = "packet",
                        messageId = "message",
                        sentAtEpochMilliseconds = 1L,
                        text = "Hello"
                    )
                )
            }
    }

    private data object NoGroupTransportKeyResolver : GroupTransportKeyResolver {
        override suspend fun resolveEncryptionPublicKey(
            packet: SecureChatPacket,
            contactId: String
        ): Result<ByteArray?> = Result.success(null)
    }

    private class RecordingGroupRelayIdResolver : GroupRelayIdResolver {
        override suspend fun resolve(
            groupId: String,
            contactId: String
        ): Result<String> = Result.success("group-recipient-relay-id")

        override suspend fun resolveMembers(groupId: String): Result<Map<String, String>> =
            Result.success(emptyMap())

        override fun resolveRemovedMember(signingPublicKey: ByteArray): Result<String> =
            Result.success("removed-member-relay-id")

        override suspend fun resolveForMessage(
            messageId: String,
            contactId: String
        ): Result<String?> = Result.success(null)

        override suspend fun resolveContactId(relayId: String): Result<String?> =
            Result.success(null)
    }

    private class RecordingContactRelayIdResolver : ContactRelayIdResolver {
        var canonicalResolveCount: Int = 0
        var bootstrapResolveCount: Int = 0

        override suspend fun resolve(contactId: String): Result<String> {
            canonicalResolveCount += 1
            return Result.success("recipient-relay-id")
        }

        override suspend fun resolveBootstrap(contactId: String): Result<String> {
            bootstrapResolveCount += 1
            return Result.success("bootstrap-recipient-relay-id")
        }
    }

    private class RecordingOutgoingWireSender(
        private val failingCalls: Set<Int> = emptySet()
    ) : OutgoingWireSender {
        val sent = mutableListOf<Pair<String, String>>()

        override suspend fun send(
            recipientAddress: String,
            encodedTransportPayload: String
        ): Result<Unit> {
            sent += recipientAddress to encodedTransportPayload

            return if (sent.size in failingCalls) {
                Result.failure(IllegalStateException("send failed"))
            } else {
                Result.success(Unit)
            }
        }
    }

    private class RecordingDeliveryStateListener : OutboxDeliveryStateListener {
        val events = mutableListOf<String>()

        override suspend fun onProcessing(packetId: String): Result<Unit> {
            events += "processing:$packetId"
            return Result.success(Unit)
        }

        override suspend fun onPrepared(
            packetId: String,
            encodedTransportPayload: String,
            transportMode: String
        ): Result<Unit> {
            events += "prepared:$packetId:$transportMode"
            return Result.success(Unit)
        }

        override suspend fun onSent(packetId: String): Result<Unit> {
            events += "sent:$packetId"
            return Result.success(Unit)
        }

        override suspend fun onFailed(
            packetId: String,
            errorMessage: String
        ): Result<Unit> {
            events += "failed:$packetId:$errorMessage"
            return Result.success(Unit)
        }
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
        val ENCODED_PACKET = byteArrayOf(1, 2, 3)
        val GROUP_PACKET_BYTES = byteArrayOf(4, 5, 6)
        val GROUP_MESSAGE_PACKET_BYTES = byteArrayOf(7, 8, 9)
        val CONTACT_INVITE_PACKET_BYTES = byteArrayOf(10, 11, 12)
        val CONTACT_READY_PACKET_BYTES = byteArrayOf(13, 14, 15)
        val VERIFICATION_RECEIPT_PACKET_BYTES = byteArrayOf(16, 17, 18)
        val GROUP_INVITE_PACKET_BYTES = byteArrayOf(19, 20, 21)
        val REMOTE_ENCRYPTION_KEY = ByteArray(32) { 13 }
        val REMOTE_SIGNING_KEY = ByteArray(32) { 16 }
    }
}
