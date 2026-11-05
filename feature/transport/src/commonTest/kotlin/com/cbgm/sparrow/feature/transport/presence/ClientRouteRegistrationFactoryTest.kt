package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.crypto.claim.AliasClaimSealer
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientRouteRegistrationFactoryTest {
    private val sealedClaims = mutableListOf<Pair<String, ByteArray>>()
    private val recordingSealer =
        object : AliasClaimSealer {
            override suspend fun seal(
                claim: ByteArray,
                presencePublicKey: ByteArray
            ): Result<ByteArray> {
                sealedClaims += claim.decodeToString() to presencePublicKey
                return Result.success(byteArrayOf(42))
            }
        }

    @Test
    fun phoneAliasesGoOnlyIntoTheSealedClaim() =
        runTest {
            val signedPayloads = mutableListOf<String>()
            val factory =
                ClientRouteRegistrationFactory(
                    signingKeyPairProvider =
                        object : LocalSigningKeyPairProvider {
                            override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
                                Result.success(LocalSigningKeyPair(publicKey = byteArrayOf(1), privateKey = byteArrayOf(2)))
                        },
                    signatureCrypto =
                        object : DetachedSignatureCrypto {
                            override suspend fun sign(
                                payload: ByteArray,
                                signingPrivateKey: ByteArray
                            ): Result<ByteArray> {
                                signedPayloads += payload.decodeToString()
                                return Result.success(byteArrayOf(7))
                            }

                            override suspend fun verify(
                                payload: ByteArray,
                                signingPublicKey: ByteArray,
                                signature: ByteArray
                            ): Result<Unit> = Result.success(Unit)
                        },
                    aliasClaimSealer = recordingSealer,
                    json = createGatewayJson()
                )

            val registration =
                factory
                    .create(
                        routingId = "scrouting1_test",
                        nodeId = "node-a",
                        connectionId = "connection-a",
                        generation = 123L,
                        expiresAtEpochMilliseconds = 456L,
                        aliasClaim = PendingAliasClaim(listOf("scphone1_own"), "token", byteArrayOf(9))
                    ).getOrThrow()

            assertNull(registration.route.aliases)
            assertContentEquals(byteArrayOf(42), registration.sealedAliasClaim)
            // Same canonical form the presence directory verifies (UnsignedAliasClaim in :server:protocol).
            assertEquals(
                "{\"routingId\":\"scrouting1_test\",\"connectionId\":\"connection-a\",\"generation\":123," +
                    "\"aliases\":[\"scphone1_own\"],\"phoneVerificationToken\":\"token\"}",
                signedPayloads.last()
            )
            val (claim, key) = sealedClaims.single()
            assertTrue(claim.contains("\"signature\""))
            assertContentEquals(byteArrayOf(9), key)
            assertTrue(signedPayloads.none { payload -> payload.contains("scphone1") && payload.contains("nodeId") })
        }

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
                    aliasClaimSealer = recordingSealer,
                    json = createGatewayJson()
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
                    aliasClaimSealer = recordingSealer,
                    json = createGatewayJson()
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
