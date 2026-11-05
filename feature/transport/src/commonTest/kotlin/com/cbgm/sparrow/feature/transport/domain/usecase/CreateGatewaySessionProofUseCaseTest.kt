package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayServerMessage
import com.cbgm.sparrow.feature.transport.gateway.model.gatewaySessionPayload
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.InternalSerializationApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalSerializationApi::class)
class CreateGatewaySessionProofUseCaseTest {
    private var signedPayload: ByteArray? = null
    private var signingPrivateKey: ByteArray? = null

    private val useCase =
        CreateGatewaySessionProofUseCase(
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
                        this@CreateGatewaySessionProofUseCaseTest.signingPrivateKey = signingPrivateKey
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
    fun payloadMatchesTheGatewayWireVector() {
        val payload =
            gatewaySessionPayload(
                routingId = "r",
                connectionId = "c",
                nodeId = "n",
                nonce = byteArrayOf(1, 2)
            )

        // The gateway pins the same vector in ProtocolSignaturesTest.
        assertEquals(GATEWAY_SESSION_VECTOR, payload.toHex())
    }

    @Test
    fun signsTheChallengeWithTheIdentityKey() =
        runTest {
            val challenge = GatewayServerMessage.Challenge(nonce = NONCE, nodeId = NODE_ID)

            val proof =
                useCase(
                    challenge = challenge,
                    expectedNodeId = NODE_ID,
                    routingId = ROUTING_ID,
                    connectionId = CONNECTION_ID
                ).getOrThrow()

            assertContentEquals(PUBLIC_KEY, proof.clientSigningPublicKey)
            assertContentEquals(SIGNATURE, proof.clientSignature)
            assertContentEquals(PRIVATE_KEY, signingPrivateKey)
            assertContentEquals(
                gatewaySessionPayload(
                    routingId = ROUTING_ID,
                    connectionId = CONNECTION_ID,
                    nodeId = NODE_ID,
                    nonce = NONCE
                ),
                signedPayload
            )
        }

    @Test
    fun challengeFromAnotherNodeIsRefusedWithoutSigning() =
        runTest {
            val result =
                useCase(
                    challenge = GatewayServerMessage.Challenge(nonce = NONCE, nodeId = "node-b"),
                    expectedNodeId = NODE_ID,
                    routingId = ROUTING_ID,
                    connectionId = CONNECTION_ID
                )

            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("node-b"))
            assertNull(signedPayload)
        }

    @Test
    fun shortNonceIsRefused() =
        runTest {
            val result =
                useCase(
                    challenge = GatewayServerMessage.Challenge(nonce = ByteArray(8), nodeId = NODE_ID),
                    expectedNodeId = NODE_ID,
                    routingId = ROUTING_ID,
                    connectionId = CONNECTION_ID
                )

            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertNull(signedPayload)
        }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }

    private companion object {
        const val NODE_ID = "node-a"
        const val ROUTING_ID = "scrouting1_test"
        const val CONNECTION_ID = "connection-a"
        const val GATEWAY_SESSION_VECTOR =
            "0000001a73706172726f772d676174657761792d73657373696f6e2d7631" +
                "00000001720000000163000000016e000000020102"
        val NONCE = ByteArray(32) { index -> index.toByte() }
        val PUBLIC_KEY = byteArrayOf(1, 2, 3)
        val PRIVATE_KEY = byteArrayOf(4, 5, 6)
        val SIGNATURE = byteArrayOf(7, 8, 9)
    }
}
