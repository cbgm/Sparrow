package com.cbgm.securechat.feature.transport.relay.presence

import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.feature.transport.relay.codec.createRelayJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientRouteRegistrationFactoryTest {
    @Test
    fun signsTheCanonicalUnsignedRouteWithTheLocalIdentityKey() =
        runTest {
            var signedPayload = byteArrayOf()
            var capturedPrivateKey = byteArrayOf()
            val factory =
                ClientRouteRegistrationFactory(
                    signingKeyPairProvider =
                        object : LocalSigningKeyPairProvider {
                            override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
                                Result.success(
                                    LocalSigningKeyPair(
                                        publicKey = byteArrayOf(1, 2, 3),
                                        privateKey = byteArrayOf(4, 5, 6)
                                    )
                                )
                        },
                    signatureCrypto =
                        object : DetachedSignatureCrypto {
                            override suspend fun sign(
                                payload: ByteArray,
                                signingPrivateKey: ByteArray
                            ): Result<ByteArray> {
                                signedPayload = payload
                                capturedPrivateKey = signingPrivateKey
                                return Result.success(byteArrayOf(7, 8, 9))
                            }

                            override suspend fun verify(
                                payload: ByteArray,
                                signingPublicKey: ByteArray,
                                signature: ByteArray
                            ): Result<Unit> = Result.success(Unit)
                        },
                    json = createRelayJson()
                )

            val registration =
                factory
                    .create(
                        routingId = "scrouting1_test",
                        nodeId = "node-a",
                        connectionId = "connection-a",
                        generation = 123L,
                        expiresAtEpochMilliseconds = 456L
                    ).getOrThrow()
            val expectedPayload =
                "{\"routingId\":\"scrouting1_test\",\"nodeId\":\"node-a\"," +
                    "\"connectionId\":\"connection-a\",\"generation\":123," +
                    "\"expiresAtEpochMilliseconds\":456}"

            assertEquals(
                expectedPayload,
                signedPayload.decodeToString()
            )
            assertContentEquals(byteArrayOf(4, 5, 6), capturedPrivateKey)
            assertContentEquals(byteArrayOf(1, 2, 3), registration.clientSigningPublicKey)
            assertContentEquals(byteArrayOf(7, 8, 9), registration.route.clientSignature)
        }

    @Test
    fun rejectsLocallyInconsistentSigningKeyPair() =
        runTest {
            val factory =
                ClientRouteRegistrationFactory(
                    signingKeyPairProvider =
                        object : LocalSigningKeyPairProvider {
                            override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
                                Result.success(
                                    LocalSigningKeyPair(
                                        publicKey = byteArrayOf(1, 2, 3),
                                        privateKey = byteArrayOf(4, 5, 6)
                                    )
                                )
                        },
                    signatureCrypto =
                        object : DetachedSignatureCrypto {
                            override suspend fun sign(
                                payload: ByteArray,
                                signingPrivateKey: ByteArray
                            ): Result<ByteArray> = Result.success(byteArrayOf(7, 8, 9))

                            override suspend fun verify(
                                payload: ByteArray,
                                signingPublicKey: ByteArray,
                                signature: ByteArray
                            ): Result<Unit> =
                                Result.failure(IllegalArgumentException("Mismatched signing key pair"))
                        },
                    json = createRelayJson()
                )

            val result =
                factory.create(
                    routingId = "scrouting1_test",
                    nodeId = "node-a",
                    connectionId = "connection-a",
                    generation = 123L,
                    expiresAtEpochMilliseconds = 456L
                )

            assertTrue(result.isFailure)
        }
}
