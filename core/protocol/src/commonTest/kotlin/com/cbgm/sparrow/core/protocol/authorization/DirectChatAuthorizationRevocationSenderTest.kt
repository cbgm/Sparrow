package com.cbgm.sparrow.core.protocol.authorization

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DirectChatAuthorizationRevocationSenderTest {
    private val keys = LocalSigningKeyPair(ByteArray(32) { 1 }, ByteArray(64) { 2 })
    private val keyProvider = object : LocalSigningKeyPairProvider {
        override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> = Result.success(keys)
    }
    private val crypto = object : DetachedSignatureCrypto {
        override suspend fun sign(payload: ByteArray, signingPrivateKey: ByteArray): Result<ByteArray> =
            Result.success(ByteArray(64) { 3 })

        override suspend fun verify(payload: ByteArray, signingPublicKey: ByteArray, signature: ByteArray): Result<Unit> =
            Result.success(Unit)
    }

    @Test
    fun enqueuesOnceAndResendsExistingPacket() = runTest {
        val outbox = RecordingOutbox()
        val sender = DirectChatAuthorizationRevocationSender(
            DirectChatAuthorizationRevocationProtocol(crypto),
            keyProvider,
            outbox
        )
        sender.enqueueOrResend("peer", "exchange", ByteArray(32) { 4 }).getOrThrow()
        assertEquals(1, outbox.enqueueCount)
        assertEquals(0, outbox.resendCount)
        assertEquals("peer", outbox.peer)
        assertEquals("direct-chat-authorization-revoked-exchange", outbox.packetId)

        sender.enqueueOrResend("peer", "exchange", ByteArray(32) { 4 }).getOrThrow()
        assertEquals(1, outbox.enqueueCount)
        assertEquals(1, outbox.resendCount)
    }

    @Test
    fun enqueueFailureIsReturnedToCaller() = runTest {
        val outbox = RecordingOutbox(failEnqueue = true)
        val sender = DirectChatAuthorizationRevocationSender(
            DirectChatAuthorizationRevocationProtocol(crypto),
            keyProvider,
            outbox
        )
        val result = sender.enqueueOrResend("peer", "exchange", ByteArray(32) { 4 })
        assertTrue(result.isFailure)
        assertEquals(1, outbox.enqueueCount)
    }

    private class RecordingOutbox(
        private val failEnqueue: Boolean = false
    ) : ProtocolOutbox {
        var enqueueCount = 0
        var resendCount = 0
        var peer: String? = null
        var packetId: String? = null
        private var storedItem: ProtocolOutboxItem? = null

        override suspend fun enqueue(contactId: String, packet: SparrowPacket): Result<ProtocolOutboxItem> {
            enqueueCount++
            peer = contactId
            packetId = packet.packetId
            if (failEnqueue) return Result.failure(IllegalStateException("outbox failed"))
            val item = ProtocolOutboxItem(
                id = "item",
                contactId = contactId,
                packetId = packet.packetId,
                encodedPacket = byteArrayOf(1),
                status = OutboxStatus.PENDING,
                attemptCount = 0,
                lastError = null,
                createdAtEpochMilliseconds = 1L,
                updatedAtEpochMilliseconds = 1L
            )
            storedItem = item
            return Result.success(item)
        }

        override fun observePending(): Flow<List<ProtocolOutboxItem>> = flowOf(emptyList())

        override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> = Result.success(emptyList())

        override suspend fun markProcessing(itemId: String): Result<Unit> = Result.success(Unit)

        override suspend fun markSent(itemId: String): Result<Unit> = Result.success(Unit)

        override suspend fun markFailed(itemId: String, errorMessage: String): Result<Unit> = Result.success(Unit)

        override suspend fun retry(itemId: String): Result<Unit> = Result.success(Unit)

        override suspend fun resend(packetId: String): Result<Unit> {
            resendCount++
            return Result.success(Unit)
        }

        override suspend fun requeueInterrupted(): Result<Unit> = Result.success(Unit)

        override suspend fun retryFailed(): Result<Unit> = Result.success(Unit)

        override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> =
            Result.success(storedItem?.takeIf { it.packetId == packetId })
    }
}
