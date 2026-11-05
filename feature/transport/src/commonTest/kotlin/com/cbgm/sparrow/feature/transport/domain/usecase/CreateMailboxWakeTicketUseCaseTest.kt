package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.transport.mailbox.mailboxWakeTicketPayload
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CreateMailboxWakeTicketUseCaseTest {
    private var signedPayload: ByteArray? = null

    private val useCase =
        CreateMailboxWakeTicketUseCase(
            localRoutingIdProvider =
                object : LocalRoutingIdProvider {
                    override suspend fun getLocalRoutingId(): Result<String> = Result.success(ROUTING_ID)
                },
            signingKeyPairProvider =
                object : LocalSigningKeyPairProvider {
                    override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
                        Result.success(LocalSigningKeyPair(publicKey = PUBLIC_KEY, privateKey = PRIVATE_KEY))
                },
            signatureCrypto =
                object : DetachedSignatureCrypto {
                    override suspend fun sign(
                        payload: ByteArray,
                        signingPrivateKey: ByteArray
                    ): Result<ByteArray> {
                        signedPayload = payload
                        return Result.success(SIGNATURE)
                    }

                    override suspend fun verify(
                        payload: ByteArray,
                        signingPublicKey: ByteArray,
                        signature: ByteArray
                    ): Result<Unit> = Result.success(Unit)
                }
        )

    @Test
    fun payloadMatchesTheServerWireVector() {
        val payload = mailboxWakeTicketPayload("routing", "node", 1_234_567_890_123L)

        // The push server pins the same vector in ProtocolSignaturesTest.
        assertEquals(MAILBOX_WAKE_VECTOR, payload.toHex())
    }

    @Test
    fun signsTheDeviceTheNodeAndTheMailboxExpiry() =
        runTest {
            val ticket = useCase(nodeId = NODE_ID, expiresAtEpochMilliseconds = EXPIRES_AT).getOrThrow()

            assertEquals(ROUTING_ID, ticket.routingId)
            assertEquals(NODE_ID, ticket.nodeId)
            assertEquals(EXPIRES_AT, ticket.expiresAtEpochMilliseconds)
            assertContentEquals(PUBLIC_KEY, ticket.clientSigningPublicKey)
            assertContentEquals(SIGNATURE, ticket.clientSignature)
            assertContentEquals(mailboxWakeTicketPayload(ROUTING_ID, NODE_ID, EXPIRES_AT), signedPayload)
        }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }

    private companion object {
        const val ROUTING_ID = "scrouting1_test"
        const val NODE_ID = "mailbox-node"
        const val EXPIRES_AT = 1_700_000_000_000L
        const val MAILBOX_WAKE_VECTOR =
            "0000001773706172726f772d6d61696c626f782d77616b652d7631" +
                "00000007726f7574696e67000000046e6f6465000000080000011f71fb04cb"
        val PUBLIC_KEY = byteArrayOf(1, 2, 3)
        val PRIVATE_KEY = byteArrayOf(4, 5, 6)
        val SIGNATURE = byteArrayOf(7, 8, 9)
    }
}
