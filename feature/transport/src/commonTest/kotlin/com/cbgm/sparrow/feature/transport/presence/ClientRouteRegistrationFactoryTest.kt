package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.phone.PhoneVerificationProofProvider
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
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
                    json = createGatewayJson(),
                    phoneNumberProvider = object : LocalPhoneNumberProvider {
                        override suspend fun getLocalPhoneNumber() = Result.success("+491701234567")
                    },
                    phoneVerificationProof = object : PhoneVerificationProofProvider {
                        override suspend fun tokenFor(phoneNumber: String): String? = null
                    }
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
    fun carriesPhoneProofWithoutChangingSignedRoutePayload() = runTest {
        var signedPayload = byteArrayOf()
        val factory = ClientRouteRegistrationFactory(
            signingKeyPairProvider = object : LocalSigningKeyPairProvider {
                override suspend fun getSigningKeyPair() = Result.success(
                    LocalSigningKeyPair(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))
                )
            },
            signatureCrypto = object : DetachedSignatureCrypto {
                override suspend fun sign(payload: ByteArray, signingPrivateKey: ByteArray): Result<ByteArray> {
                    signedPayload = payload
                    return Result.success(byteArrayOf(7, 8, 9))
                }

                override suspend fun verify(payload: ByteArray, signingPublicKey: ByteArray, signature: ByteArray) =
                    Result.success(Unit)
            },
            json = createGatewayJson(),
            phoneNumberProvider = object : LocalPhoneNumberProvider {
                override suspend fun getLocalPhoneNumber() = Result.success("+491701234567")
            },
            phoneVerificationProof = object : PhoneVerificationProofProvider {
                override suspend fun tokenFor(phoneNumber: String) =
                    if (phoneNumber == "+491701234567") "firebase-id-token" else null
            }
        )
        val registration = factory.create(
            routingId = "scrouting1_test",
            nodeId = "node-a",
            connectionId = "connection-a",
            generation = 123L,
            expiresAtEpochMilliseconds = 456L,
            aliases = listOf("phone-hash")
        ).getOrThrow()
        assertEquals("firebase-id-token", registration.phoneVerificationToken)
        assertTrue(!signedPayload.decodeToString().contains("firebase-id-token"))
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
                    json = createGatewayJson(),
                    phoneNumberProvider = object : LocalPhoneNumberProvider {
                        override suspend fun getLocalPhoneNumber() = Result.success("+491701234567")
                    },
                    phoneVerificationProof = object : PhoneVerificationProofProvider {
                        override suspend fun tokenFor(phoneNumber: String): String? = null
                    }
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
